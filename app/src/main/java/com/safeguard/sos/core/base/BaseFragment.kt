package com.safeguard.sos.core.base

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.fragment.findNavController
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
 * Base Fragment class that provides common functionality
 * for all fragments in the application.
 */
abstract class BaseFragment<VB : ViewBinding> : Fragment() {

    private var _binding: VB? = null
    protected val binding: VB
        get() = _binding ?: throw IllegalStateException(
            "Binding is only valid between onCreateView and onDestroyView"
        )

    private var loadingDialog: LoadingDialog? = null

    // Navigation controller
    protected val navController: NavController
        get() = findNavController()

    // Abstract function to get ViewBinding
    abstract fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): VB

    // Optional: Override to set up views after binding is created
    open fun setupViews() {}

    // Optional: Override to observe data from ViewModel
    open fun observeData() {}

    // Optional: Override to setup click listeners
    protected open fun setupClickListeners() {}

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = getViewBinding(inflater, container)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        setupClickListeners()
        observeData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        dismissLoadingDialog()
        _binding = null
    }

    /**
     * Collect flow safely with lifecycle awareness
     */
    protected fun <T> collectFlow(flow: Flow<T>, action: suspend (T) -> Unit) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                flow.collect { action(it) }
            }
        }
    }

    /**
     * Collect flow when fragment is at least started
     */
    protected fun <T> collectWhenStarted(flow: Flow<T>, action: suspend (T) -> Unit) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
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
            is UiEvent.NavigateBack -> navigateBack()
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
     * Navigate to a destination
     */
    protected fun navigateTo(directions: NavDirections) {
        try {
            navController.navigate(directions)
        } catch (e: Exception) {
            Timber.e(e, "Navigation failed")
        }
    }

    /**
     * Navigate to route by ID
     */
    protected fun navigateTo(destinationId: Int, args: Bundle? = null) {
        try {
            navController.navigate(destinationId, args)
        } catch (e: Exception) {
            Timber.e(e, "Navigation failed")
        }
    }

    /**
     * Navigate to route by string
     */
    protected fun navigateTo(route: String) {
        Timber.d("Navigate to: $route")
        // Implement based on your navigation setup
    }

    /**
     * Navigate back
     */
    protected fun navigateBack() {
        try {
            navController.navigateUp()
        } catch (e: Exception) {
            Timber.e(e, "Navigate back failed")
            activity?.onBackPressedDispatcher?.onBackPressed()
        }
    }

    /**
     * Pop back stack to destination
     */
    protected fun popBackTo(destinationId: Int, inclusive: Boolean = false) {
        navController.popBackStack(destinationId, inclusive)
    }

    /**
     * Show loading dialog
     */
    protected fun showLoadingDialog(message: String? = null) {
        if (loadingDialog == null) {
            loadingDialog = LoadingDialog(requireContext())
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
        view?.let { view ->
            val snackbarDuration = when (duration) {
                UiEvent.SnackbarDuration.Short -> Snackbar.LENGTH_SHORT
                UiEvent.SnackbarDuration.Long -> Snackbar.LENGTH_LONG
                UiEvent.SnackbarDuration.Indefinite -> Snackbar.LENGTH_INDEFINITE
            }

            val snackbar = Snackbar.make(view, message, snackbarDuration)

            if (actionLabel != null && action != null) {
                snackbar.setAction(actionLabel) { action() }
            }

            snackbar.show()
        }
    }

    /**
     * Show toast
     */
    protected fun showToast(
        message: String,
        duration: UiEvent.ToastDuration = UiEvent.ToastDuration.Short
    ) {
        val toastDuration = when (duration) {
            UiEvent.ToastDuration.Short -> Toast.LENGTH_SHORT
            UiEvent.ToastDuration.Long -> Toast.LENGTH_LONG
        }
        Toast.makeText(requireContext(), message, toastDuration).show()
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
        context?.let { ctx ->
            MaterialAlertDialogBuilder(ctx, R.style.SafeGuard_Dialog)
                .setTitle(title)
                .setMessage(message)
                .setCancelable(cancelable)
                .setPositiveButton(positiveButton) { dialog, _ ->
                    onPositiveClick?.invoke()
                    dialog.dismiss()
                }
                .apply {
                    negativeButton?.let {
                        setNegativeButton(it) { dialog, _ ->
                            onNegativeClick?.invoke()
                            dialog.dismiss()
                        }
                    }
                }
                .show()
        }
    }

    /**
     * Show error dialog with optional retry
     */
    protected fun showErrorDialog(
        message: String,
        title: String? = null,
        onRetry: (() -> Unit)? = null
    ) {
        context?.let { ctx ->
            val builder = MaterialAlertDialogBuilder(ctx, R.style.SafeGuard_Dialog)
                .setTitle(title ?: getString(R.string.error_generic))
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
    }

    /**
     * Hide keyboard
     */
    protected fun hideKeyboard() {
        activity?.let { activity ->
            val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            activity.currentFocus?.let { view ->
                imm.hideSoftInputFromWindow(view.windowToken, 0)
            }
        }
    }

    /**
     * Show keyboard
     */
    protected fun showKeyboard(view: View) {
        view.requestFocus()
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

    /**
     * Check if fragment is visible and active
     */
    protected fun isFragmentActive(): Boolean {
        return isAdded && !isDetached && view != null
    }
}
