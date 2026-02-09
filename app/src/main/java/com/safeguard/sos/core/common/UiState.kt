// app/src/main/java/com/safeguard/sos/core/common/UiState.kt

package com.safeguard.sos.core.common

/**
 * Base interface for UI states
 */
interface UiState

/**
 * Common loading state for screens
 */
data class LoadingState(
    val isLoading: Boolean = false,
    val loadingMessage: String? = null
)

/**
 * Common error state for screens
 */
data class ErrorState(
    val hasError: Boolean = false,
    val errorMessage: String? = null,
    val errorCode: Int? = null,
    val retryAction: (() -> Unit)? = null
)

/**
 * Base UI state that can be extended
 */
abstract class BaseUiState(
    open val isLoading: Boolean = false,
    open val error: String? = null
) : UiState {
    val hasError: Boolean get() = error != null
}

/**
 * Result state for operations
 */
sealed class ResultState<out T> {
    data object Idle : ResultState<Nothing>()
    data object Loading : ResultState<Nothing>()
    data class Success<T>(val data: T) : ResultState<T>()
    data class Error(val message: String) : ResultState<Nothing>()
}