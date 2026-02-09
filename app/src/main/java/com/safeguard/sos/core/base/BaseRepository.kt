// app/src/main/java/com/safeguard/sos/core/base/BaseRepository.kt

package com.safeguard.sos.core.base

import com.safeguard.sos.core.common.Constants
import com.safeguard.sos.core.common.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import retrofit2.HttpException
import timber.log.Timber
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

abstract class BaseRepository {

    /**
     * Execute API call with proper error handling
     */
    protected suspend fun <T> safeApiCall(
        apiCall: suspend () -> T
    ): Resource<T> {
        return try {
            Resource.Success(apiCall())
        } catch (e: Exception) {
            Timber.e(e, "API call failed")
            handleException(e)
        }
    }

    /**
     * Execute API call and emit as Flow
     */
    protected fun <T> safeApiFlow(
        apiCall: suspend () -> T
    ): Flow<Resource<T>> = flow {
        emit(Resource.Loading)
        emit(safeApiCall(apiCall))
    }.flowOn(Dispatchers.IO)

    /**
     * Execute API call with caching strategy
     * First emit cached data, then fetch from network
     */
    protected fun <T> networkBoundResource(
        fetchFromLocal: suspend () -> T?,
        fetchFromRemote: suspend () -> T,
        saveToLocal: suspend (T) -> Unit,
        shouldFetch: (T?) -> Boolean = { true }
    ): Flow<Resource<T>> = flow {
        emit(Resource.Loading)

        val localData = fetchFromLocal()

        if (localData != null) {
            emit(Resource.Success(localData))
        }

        if (shouldFetch(localData)) {
            when (val remoteResult = safeApiCall { fetchFromRemote() }) {
                is Resource.Success -> {
                    saveToLocal(remoteResult.data)
                    emit(Resource.Success(remoteResult.data))
                }
                is Resource.Error -> {
                    if (localData == null) {
                        emit(remoteResult)
                    }
                    // If we have local data, we already emitted it
                }
                else -> { /* Loading state already emitted */ }
            }
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Handle different types of exceptions
     */
    private fun <T> handleException(exception: Exception): Resource<T> {
        return when (exception) {
            is SocketTimeoutException -> {
                Resource.Error(
                    message = "Connection timed out. Please try again.",
                    errorCode = Constants.ErrorCodes.TIMEOUT_ERROR,
                    exception = exception
                )
            }
            is UnknownHostException -> {
                Resource.Error(
                    message = "No internet connection. Please check your network.",
                    errorCode = Constants.ErrorCodes.NETWORK_ERROR,
                    exception = exception
                )
            }
            is IOException -> {
                Resource.Error(
                    message = "Network error occurred. Please try again.",
                    errorCode = Constants.ErrorCodes.NETWORK_ERROR,
                    exception = exception
                )
            }
            is HttpException -> {
                val errorMessage = when (exception.code()) {
                    400 -> "Bad request"
                    401 -> "Unauthorized. Please login again."
                    403 -> "Access forbidden"
                    404 -> "Resource not found"
                    500, 502, 503 -> "Server error. Please try again later."
                    else -> "An error occurred"
                }
                Resource.Error(
                    message = errorMessage,
                    errorCode = exception.code(),
                    exception = exception
                )
            }
            else -> {
                Resource.Error(
                    message = exception.message ?: "An unexpected error occurred",
                    errorCode = Constants.ErrorCodes.UNKNOWN_ERROR,
                    exception = exception
                )
            }
        }
    }
}