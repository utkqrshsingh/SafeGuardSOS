package com.safeguard.sos.data.remote.interceptor

import com.safeguard.sos.data.local.datastore.UserPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val userPreferences: UserPreferences
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Skip auth header for login/register endpoints
        val path = originalRequest.url.encodedPath
        if (path.contains("login") || path.contains("register") || path.contains("send-otp")) {
            return chain.proceed(originalRequest)
        }

        // Get token synchronously
        val token = runBlocking {
            userPreferences.authTokenFlow.first()
        }

        // If no token, proceed without auth header
        if (token.isNullOrEmpty()) {
            return chain.proceed(originalRequest)
        }

        // Add authorization header
        val authenticatedRequest = originalRequest.newBuilder()
            .header("Authorization", "Bearer $token")
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .build()

        val response = chain.proceed(authenticatedRequest)

        // Handle 401 Unauthorized - token expired
        if (response.code == 401) {
            response.close()

            // Try to refresh token
            // In a real implementation, you would refresh the token here
            // For now, we'll just return the 401 response

            // TODO: Implement token refresh logic
            // val newToken = refreshToken()
            // if (newToken != null) {
            //     val newRequest = originalRequest.newBuilder()
            //         .header("Authorization", "Bearer $newToken")
            //         .build()
            //     return chain.proceed(newRequest)
            // }
        }

        return response
    }
}