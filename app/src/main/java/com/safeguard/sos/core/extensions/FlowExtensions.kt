// app/src/main/java/com/safeguard/sos/core/extensions/FlowExtensions.kt

package com.safeguard.sos.core.extensions

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.safeguard.sos.core.common.Resource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.IOException
import kotlin.math.pow

/**
 * Collect flow with lifecycle awareness
 */
fun <T> Flow<T>.collectWithLifecycle(
    lifecycleOwner: LifecycleOwner,
    state: Lifecycle.State = Lifecycle.State.STARTED,
    collector: suspend (T) -> Unit
) {
    lifecycleOwner.lifecycleScope.launch {
        lifecycleOwner.repeatOnLifecycle(state) {
            collect { collector(it) }
        }
    }
}

/**
 * Collect flow in coroutine scope
 */
fun <T> Flow<T>.collectIn(
    scope: CoroutineScope,
    collector: suspend (T) -> Unit
) {
    scope.launch {
        collect { collector(it) }
    }
}

/**
 * Map flow to Resource
 */
fun <T> Flow<T>.asResource(): Flow<Resource<T>> = this
    .map<T, Resource<T>> { Resource.Success(it) }
    .onStart { emit(Resource.Loading) }
    .catch { emit(Resource.Error(it.message ?: "Unknown error", exception = it)) }

/**
 * Retry with exponential backoff
 */
fun <T> Flow<T>.retryWithExponentialBackoff(
    maxRetries: Int = 3,
    initialDelay: Long = 1000,
    maxDelay: Long = 10000,
    factor: Double = 2.0,
    retryOn: (Throwable) -> Boolean = { it is IOException }
): Flow<T> = this.retryWhen { cause, attempt ->
    if (attempt < maxRetries && retryOn(cause)) {
        val delayTime = (initialDelay * factor.pow(attempt.toDouble())).toLong().coerceAtMost(maxDelay)
        Timber.d("Retrying in ${delayTime}ms (attempt ${attempt + 1}/$maxRetries)")
        delay(delayTime)
        true
    } else {
        false
    }
}

/**
 * Handle errors with logging
 */
fun <T> Flow<T>.handleErrors(
    onError: (Throwable) -> Unit = { Timber.e(it) }
): Flow<T> = this.catch { e ->
    onError(e)
}

/**
 * Emit loading state
 */
fun <T> resourceFlow(block: suspend () -> T): Flow<Resource<T>> = flow {
    emit(Resource.Loading)
    try {
        emit(Resource.Success(block()))
    } catch (e: Exception) {
        Timber.e(e, "Resource flow error")
        emit(Resource.Error(e.message ?: "Unknown error", exception = e))
    }
}

/**
 * Update MutableStateFlow value
 */
inline fun <T> MutableStateFlow<T>.update(function: (T) -> T) {
    while (true) {
        val prevValue = value
        val nextValue = function(prevValue)
        if (compareAndSet(prevValue, nextValue)) {
            return
        }
    }
}

/**
 * Throttle first emission
 */
fun <T> Flow<T>.throttleFirst(windowDuration: Long): Flow<T> = flow {
    var lastEmissionTime = 0L
    collect { upstream ->
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastEmissionTime >= windowDuration) {
            lastEmissionTime = currentTime
            emit(upstream)
        }
    }
}

/**
 * Debounce with immediate first emission
 */
fun <T> Flow<T>.debounceImmediate(timeoutMillis: Long): Flow<T> = flow {
    var lastEmissionTime = 0L
    var lastValue: T? = null
    var hasEmitted = false

    collect { value ->
        val currentTime = System.currentTimeMillis()
        lastValue = value

        if (!hasEmitted) {
            emit(value)
            hasEmitted = true
            lastEmissionTime = currentTime
        } else {
            delay(timeoutMillis)
            if (lastValue == value && currentTime - lastEmissionTime >= timeoutMillis) {
                emit(value)
                lastEmissionTime = System.currentTimeMillis()
            }
        }
    }
}