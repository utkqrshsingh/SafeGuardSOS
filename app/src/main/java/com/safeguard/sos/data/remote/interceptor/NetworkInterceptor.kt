package com.safeguard.sos.data.remote.interceptor

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.safeguard.sos.core.common.Constants
import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

class NetworkInterceptor(
    private val context: Context
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()

        if (!isNetworkAvailable()) {
            // If no network, try to get from cache
            val cacheControl = CacheControl.Builder()
                .maxStale(Constants.CACHE_MAX_STALE, TimeUnit.SECONDS)
                .build()

            request = request.newBuilder()
                .cacheControl(cacheControl)
                .build()
        } else {
            // If network available, get fresh data (with some cache tolerance)
            val cacheControl = CacheControl.Builder()
                .maxAge(Constants.CACHE_MAX_AGE, TimeUnit.SECONDS)
                .build()

            request = request.newBuilder()
                .cacheControl(cacheControl)
                .build()
        }

        val response = chain.proceed(request)

        // Add caching headers to response
        return if (isNetworkAvailable()) {
            response.newBuilder()
                .removeHeader("Pragma")
                .removeHeader("Cache-Control")
                .header("Cache-Control", "public, max-age=${Constants.CACHE_MAX_AGE}")
                .build()
        } else {
            response.newBuilder()
                .removeHeader("Pragma")
                .removeHeader("Cache-Control")
                .header("Cache-Control", "public, only-if-cached, max-stale=${Constants.CACHE_MAX_STALE}")
                .build()
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}

class NoNetworkException : IOException("No internet connection available")