// app/src/main/java/com/safeguard/sos/core/common/UiEvent.kt

package com.safeguard.sos.core.common

/**
 * Sealed class representing one-time UI events
 */
sealed class UiEvent {

    data object Success : UiEvent()

    data class Error(val message: String) : UiEvent()

    data class ShowSnackbar(
        val message: String,
        val actionLabel: String? = null,
        val duration: SnackbarDuration = SnackbarDuration.Short
    ) : UiEvent()

    data class ShowToast(
        val message: String,
        val duration: ToastDuration = ToastDuration.Short
    ) : UiEvent()

    data class Navigate(
        val route: String,
        val popUpTo: String? = null,
        val inclusive: Boolean = false,
        val singleTop: Boolean = true
    ) : UiEvent()

    data object NavigateBack : UiEvent()

    data class NavigateWithArgs(
        val route: String,
        val args: Map<String, Any> = emptyMap()
    ) : UiEvent()

    data class ShowDialog(
        val title: String,
        val message: String,
        val positiveButton: String = "OK",
        val negativeButton: String? = null,
        val onPositiveClick: (() -> Unit)? = null,
        val onNegativeClick: (() -> Unit)? = null,
        val cancelable: Boolean = true
    ) : UiEvent()

    data object DismissDialog : UiEvent()

    data class ShowBottomSheet(val tag: String) : UiEvent()

    data object DismissBottomSheet : UiEvent()

    data object HideKeyboard : UiEvent()

    data class LaunchIntent(val intentAction: String, val data: Map<String, String> = emptyMap()) : UiEvent()

    data class RequestPermission(val permission: String) : UiEvent()

    data class RequestPermissions(val permissions: List<String>) : UiEvent()

    enum class SnackbarDuration {
        Short, Long, Indefinite
    }

    enum class ToastDuration {
        Short, Long
    }
}

/**
 * Wrapper class for one-time events that shouldn't be consumed multiple times
 */
class Event<out T>(private val content: T) {

    private var hasBeenHandled = false

    /**
     * Returns the content and prevents its use again.
     */
    fun getContentIfNotHandled(): T? {
        return if (hasBeenHandled) {
            null
        } else {
            hasBeenHandled = true
            content
        }
    }

    /**
     * Returns the content, even if it's already been handled.
     */
    fun peekContent(): T = content
}
