// app/src/main/java/com/safeguard/sos/core/base/BaseViewModel.kt

package com.safeguard.sos.core.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safeguard.sos.core.common.Resource
import com.safeguard.sos.core.common.UiEvent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Base ViewModel class that provides common functionality
 * for all ViewModels in the application.
 */
abstract class BaseViewModel : ViewModel() {

    // Channel for one-time UI events
    private val _uiEvent = Channel<UiEvent>(Channel.BUFFERED)
    val uiEvent: Flow<UiEvent> = _uiEvent.receiveAsFlow()

    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Error state
    private val _error = MutableSharedFlow<String>()
    val error: Flow<String> = _error.asSharedFlow()

    // Exception handler for coroutines
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Timber.e(throwable, "Coroutine exception")
        handleException(throwable)
    }

    /**
     * Launch a coroutine with exception handling
     */
    protected fun launchSafe(
        dispatcher: CoroutineDispatcher = Dispatchers.Main,
        block: suspend () -> Unit
    ): Job {
        return viewModelScope.launch(dispatcher + exceptionHandler) {
            block()
        }
    }

    /**
     * Launch a coroutine with loading state management
     */
    protected fun launchWithLoading(
        dispatcher: CoroutineDispatcher = Dispatchers.Main,
        block: suspend () -> Unit
    ): Job {
        return viewModelScope.launch(dispatcher + exceptionHandler) {
            try {
                setLoading(true)
                block()
            } finally {
                setLoading(false)
            }
        }
    }

    /**
     * Execute a suspending function and wrap the result in Resource
     */
    protected suspend fun <T> executeWithResource(
        block: suspend () -> T
    ): Resource<T> {
        return try {
            Resource.Success(block())
        } catch (e: Exception) {
            Timber.e(e, "Execute with resource failed")
            Resource.Error(e.message ?: "Unknown error", exception = e)
        }
    }

    /**
     * Collect a flow and handle loading/error states
     */
    protected fun <T> collectWithState(
        flow: Flow<Resource<T>>,
        onSuccess: (T) -> Unit,
        onError: ((String) -> Unit)? = null,
        onLoading: (() -> Unit)? = null
    ): Job {
        return viewModelScope.launch {
            flow.collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        setLoading(true)
                        onLoading?.invoke()
                    }
                    is Resource.Success -> {
                        setLoading(false)
                        onSuccess(resource.data)
                    }
                    is Resource.Error -> {
                        setLoading(false)
                        onError?.invoke(resource.message) ?: handleError(resource.message)
                    }
                    is Resource.Empty -> {
                        setLoading(false)
                    }
                }
            }
        }
    }

    /**
     * Send UI event
     */
    protected fun sendUiEvent(event: UiEvent) {
        viewModelScope.launch {
            _uiEvent.send(event)
        }
    }

    /**
     * Show snackbar
     */
    protected fun showSnackbar(
        message: String,
        actionLabel: String? = null,
        duration: UiEvent.SnackbarDuration = UiEvent.SnackbarDuration.Short
    ) {
        sendUiEvent(UiEvent.ShowSnackbar(message, actionLabel, duration))
    }

    /**
     * Show toast
     */
    protected fun showToast(
        message: String,
        duration: UiEvent.ToastDuration = UiEvent.ToastDuration.Short
    ) {
        sendUiEvent(UiEvent.ShowToast(message, duration))
    }

    /**
     * Navigate to route
     */
    protected fun navigateTo(route: String) {
        sendUiEvent(UiEvent.Navigate(route))
    }

    /**
     * Navigate back
     */
    protected fun navigateBack() {
        sendUiEvent(UiEvent.NavigateBack)
    }

    /**
     * Show dialog
     */
    protected fun showDialog(
        title: String,
        message: String,
        positiveButton: String = "OK",
        negativeButton: String? = null,
        onPositiveClick: (() -> Unit)? = null,
        onNegativeClick: (() -> Unit)? = null
    ) {
        sendUiEvent(
            UiEvent.ShowDialog(
                title = title,
                message = message,
                positiveButton = positiveButton,
                negativeButton = negativeButton,
                onPositiveClick = onPositiveClick,
                onNegativeClick = onNegativeClick
            )
        )
    }

    /**
     * Hide keyboard
     */
    protected fun hideKeyboard() {
        sendUiEvent(UiEvent.HideKeyboard)
    }

    /**
     * Set loading state
     */
    protected fun setLoading(isLoading: Boolean) {
        _isLoading.value = isLoading
    }

    /**
     * Handle error
     */
    protected fun handleError(message: String) {
        viewModelScope.launch {
            _error.emit(message)
        }
        showSnackbar(message, duration = UiEvent.SnackbarDuration.Long)
    }

    /**
     * Handle exception
     */
    protected open fun handleException(throwable: Throwable) {
        val message = throwable.message ?: "An unexpected error occurred"
        handleError(message)
    }
}