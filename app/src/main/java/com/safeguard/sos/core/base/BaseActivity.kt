package com.safeguard.sos.core.base

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewbinding.ViewBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.safeguard.sos.R
import com.safeguard.sos.core.common.UiEvent
import com.safeguard.sos.presentation.components.dialogs.LoadingDialog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Base Activity class that provides common functionality
 * for all activities in the application.
 */
abstract class BaseActivity<VB : ViewBinding> : AppCompatActivity() {

    private var _binding: VB? = null
    protected val binding: VB
        get() = _binding ?: throw IllegalStateException(
            "Binding is only valid between onCreate and onDestroy"
        )

    private var loadingDialog: LoadingDialog? = null

    // Abstract function to get ViewBinding
    abstract fun getViewBinding(): VB

    // Optional: Override to set up views
    open fun setupViews() {}

    // Optional: Override to observe data
    open fun observeData() {}

    // Optional: Override to setup click listeners
    protected open fun setupClickListeners() {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = getViewBinding()
        setContentView(binding.root)

        setupViews()
        setupClickListeners()
        observeData()
    }

    override fun onDestroy() {
        super.onDestroy()
        dismissLoadingDialog()
        _binding = null
    }

    /**
     * Collect flow safely with lifecycle awareness
     */
    protected fun <T> collectFlow(flow: Flow<T>, action: suspend (T) -> Unit) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                flow.collect { action(it) }
            }
        }
    }

    /**
     * Collect flow with latest value
     */
    protected fun <T> collectLatestFlow(flow: Flow<T>, action: suspend (T) -> Unit) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                flow.collect { action(it) }
            }
        }
    }

    /**
     * Handle common UI events
     */
    protected fun handleUiEvent(event: UiEvent) {
        when (event) {
            is UiEvent.ShowSnackbar -> showSnackbar(
                message = event.message,
                actionLabel = event.actionLabel,
                duration = event.duration
            )
            is UiEvent.ShowToast -> showToast(event.message, event.duration)
            is UiEvent.Navigate -> navigateTo(event.route)
            is UiEvent.NavigateBack -> onBackPressedDispatcher.onBackPressed()
            is UiEvent.ShowDialog -> showAlertDialog(
                title = event.title,
                message = event.message,
                positiveButton = event.positiveButton,
                negativeButton = event.negativeButton,
                onPositiveClick = event.onPositiveClick,
                onNegativeClick = event.onNegativeClick,
                cancelable = event.cancelable
            )
            is UiEvent.HideKeyboard -> hideKeyboard()
            else -> Timber.d("Unhandled UI event: $event")
        }
    }

    /**
     * Show loading dialog
     */
    protected fun showLoadingDialog(message: String? = null) {
        if (loadingDialog == null) {
            loadingDialog = LoadingDialog(this)
        }
        loadingDialog?.show(message ?: getString(R.string.loading))
    }

    /**
     * Dismiss loading dialog
     */
    protected fun dismissLoadingDialog() {
        loadingDialog?.dismiss()
        loadingDialog = null
    }

    /**
     * Update loading state
     */
    protected fun updateLoadingState(isLoading: Boolean, message: String? = null) {
        if (isLoading) {
            showLoadingDialog(message)
        } else {
            dismissLoadingDialog()
        }
    }

    /**
     * Show snackbar
     */
    protected fun showSnackbar(
        message: String,
        actionLabel: String? = null,
        duration: UiEvent.SnackbarDuration = UiEvent.SnackbarDuration.Short,
        action: (() -> Unit)? = null
    ) {
        val snackbarDuration = when (duration) {
            UiEvent.SnackbarDuration.Short -> Snackbar.LENGTH_SHORT
            UiEvent.SnackbarDuration.Long -> Snackbar.LENGTH_LONG
            UiEvent.SnackbarDuration.Indefinite -> Snackbar.LENGTH_INDEFINITE
        }

        val snackbar = Snackbar.make(binding.root, message, snackbarDuration)

        if (actionLabel != null && action != null) {
            snackbar.setAction(actionLabel) { action() }
        }

        snackbar.show()
    }

    /**
     * Show toast
     */
    protected fun showToast(message: String, duration: UiEvent.ToastDuration = UiEvent.ToastDuration.Short) {
        val toastDuration = when (duration) {
            UiEvent.ToastDuration.Short -> Toast.LENGTH_SHORT
            UiEvent.ToastDuration.Long -> Toast.LENGTH_LONG
        }
        Toast.makeText(this, message, toastDuration).show()
    }

    /**
     * Show alert dialog
     */
    protected fun showAlertDialog(
        title: String,
        message: String,
        positiveButton: String = getString(R.string.ok),
        negativeButton: String? = null,
        onPositiveClick: (() -> Unit)? = null,
        onNegativeClick: (() -> Unit)? = null,
        cancelable: Boolean = true
    ) {
        val builder = MaterialAlertDialogBuilder(this, R.style.SafeGuard_Dialog)
            .setTitle(title)
            .setMessage(message)
            .setCancelable(cancelable)
            .setPositiveButton(positiveButton) { dialog, _ ->
                onPositiveClick?.invoke()
                dialog.dismiss()
            }

        negativeButton?.let {
            builder.setNegativeButton(it) { dialog, _ ->
                onNegativeClick?.invoke()
                dialog.dismiss()
            }
        }

        builder.show()
    }

    /**
     * Show error dialog
     */
    protected fun showErrorDialog(
        message: String,
        title: String = getString(R.string.error_generic),
        onRetry: (() -> Unit)? = null
    ) {
        val builder = MaterialAlertDialogBuilder(this, R.style.SafeGuard_Dialog)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(getString(R.string.ok)) { dialog, _ -> dialog.dismiss() }

        onRetry?.let {
            builder.setNegativeButton(getString(R.string.retry)) { dialog, _ ->
                dialog.dismiss()
                it.invoke()
            }
        }

        builder.show()
    }

    /**
     * Hide keyboard
     */
    protected fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        currentFocus?.let {
            imm.hideSoftInputFromWindow(it.windowToken, 0)
        }
    }

    /**
     * Show keyboard
     */
    protected fun showKeyboard(view: View) {
        view.requestFocus()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

    /**
     * Navigate to route - override in subclass for actual navigation
     */
    protected open fun navigateTo(route: String) {
        Timber.d("Navigate to: $route")
    }
}
