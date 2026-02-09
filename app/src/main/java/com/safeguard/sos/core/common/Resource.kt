// app/src/main/java/com/safeguard/sos/core/common/Resource.kt

package com.safeguard.sos.core.common

/**
 * A generic class that holds a value or an error status.
 * Used for wrapping API responses and database operations.
 */
sealed class Resource<out T> {

    data class Success<out T>(val data: T) : Resource<T>()

    data class Error(
        val message: String,
        val errorCode: Int? = null,
        val exception: Throwable? = null
    ) : Resource<Nothing>()

    data object Loading : Resource<Nothing>()

    data object Empty : Resource<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error
    val isLoading: Boolean get() = this is Loading
    val isEmpty: Boolean get() = this is Empty

    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }

    fun getOrDefault(default: @UnsafeVariance T): T = when (this) {
        is Success -> data
        else -> default
    }

    fun <R> map(transform: (T) -> R): Resource<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> Error(message, errorCode, exception)
        is Loading -> Loading
        is Empty -> Empty
    }

    inline fun onSuccess(action: (T) -> Unit): Resource<T> {
        if (this is Success) action(data)
        return this
    }

    inline fun onError(action: (String, Throwable?) -> Unit): Resource<T> {
        if (this is Error) action(message, exception)
        return this
    }

    inline fun onLoading(action: () -> Unit): Resource<T> {
        if (this is Loading) action()
        return this
    }

    companion object {
        fun <T> success(data: T): Resource<T> = Success(data)
        fun error(message: String, code: Int? = null, exception: Throwable? = null): Resource<Nothing> =
            Error(message, code, exception)
        fun loading(): Resource<Nothing> = Loading
        fun empty(): Resource<Nothing> = Empty
    }
}

/**
 * Extension function to handle Resource in a clean way
 */
inline fun <T, R> Resource<T>.fold(
    onSuccess: (T) -> R,
    onError: (String, Int?, Throwable?) -> R,
    onLoading: () -> R,
    onEmpty: () -> R
): R = when (this) {
    is Resource.Success -> onSuccess(data)
    is Resource.Error -> onError(message, errorCode, exception)
    is Resource.Loading -> onLoading()
    is Resource.Empty -> onEmpty()
}