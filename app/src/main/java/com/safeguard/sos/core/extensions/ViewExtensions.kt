// app/src/main/java/com/safeguard/sos/core/extensions/ViewExtensions.kt

package com.safeguard.sos.core.extensions

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Context
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.annotation.AnimRes
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isGone
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.safeguard.sos.core.common.Constants

/**
 * Show view with animation
 */
fun View.show() {
    if (!isVisible) {
        visibility = View.VISIBLE
    }
}

/**
 * Hide view (GONE)
 */
fun View.hide() {
    if (!isGone) {
        visibility = View.GONE
    }
}

/**
 * Make view invisible
 */
fun View.invisible() {
    if (!isInvisible) {
        visibility = View.INVISIBLE
    }
}

/**
 * Toggle visibility
 */
fun View.toggleVisibility() {
    visibility = if (isVisible) View.GONE else View.VISIBLE
}

/**
 * Set visibility based on condition
 */
fun View.visibleIf(condition: Boolean) {
    visibility = if (condition) View.VISIBLE else View.GONE
}

/**
 * Set invisible based on condition
 */
fun View.invisibleIf(condition: Boolean) {
    visibility = if (condition) View.INVISIBLE else View.VISIBLE
}

/**
 * Set gone based on condition
 */
fun View.goneIf(condition: Boolean) {
    visibility = if (condition) View.GONE else View.VISIBLE
}

/**
 * Fade in animation
 */
fun View.fadeIn(duration: Long = Constants.AnimationDurations.MEDIUM) {
    if (visibility != View.VISIBLE) {
        alpha = 0f
        visibility = View.VISIBLE
    }
    animate()
        .alpha(1f)
        .setDuration(duration)
        .setListener(null)
        .start()
}

/**
 * Fade out animation
 */
fun View.fadeOut(duration: Long = Constants.AnimationDurations.MEDIUM, hideAfter: Boolean = true) {
    animate()
        .alpha(0f)
        .setDuration(duration)
        .setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                if (hideAfter) {
                    visibility = View.GONE
                }
            }
        })
        .start()
}

/**
 * Scale in animation
 */
fun View.scaleIn(duration: Long = Constants.AnimationDurations.MEDIUM) {
    scaleX = 0f
    scaleY = 0f
    visibility = View.VISIBLE

    animate()
        .scaleX(1f)
        .scaleY(1f)
        .setDuration(duration)
        .setInterpolator(AccelerateDecelerateInterpolator())
        .setListener(null)
        .start()
}

/**
 * Scale out animation
 */
fun View.scaleOut(duration: Long = Constants.AnimationDurations.MEDIUM, hideAfter: Boolean = true) {
    animate()
        .scaleX(0f)
        .scaleY(0f)
        .setDuration(duration)
        .setInterpolator(AccelerateDecelerateInterpolator())
        .setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                if (hideAfter) {
                    visibility = View.GONE
                }
            }
        })
        .start()
}

/**
 * Slide in from bottom
 */
fun View.slideInFromBottom(duration: Long = Constants.AnimationDurations.MEDIUM) {
    translationY = height.toFloat()
    visibility = View.VISIBLE

    animate()
        .translationY(0f)
        .setDuration(duration)
        .setInterpolator(AccelerateDecelerateInterpolator())
        .setListener(null)
        .start()
}

/**
 * Slide out to bottom
 */
fun View.slideOutToBottom(duration: Long = Constants.AnimationDurations.MEDIUM, hideAfter: Boolean = true) {
    animate()
        .translationY(height.toFloat())
        .setDuration(duration)
        .setInterpolator(AccelerateDecelerateInterpolator())
        .setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                if (hideAfter) {
                    visibility = View.GONE
                }
                translationY = 0f
            }
        })
        .start()
}

/**
 * Shake animation for error indication
 */
fun View.shake() {
    ObjectAnimator.ofFloat(this, "translationX", 0f, 25f, -25f, 25f, -25f, 15f, -15f, 6f, -6f, 0f)
        .apply {
            duration = 500
            start()
        }
}

/**
 * Pulse animation
 */
fun View.pulse(scale: Float = 1.1f, duration: Long = Constants.AnimationDurations.PULSE) {
    val scaleXAnimator = ObjectAnimator.ofFloat(this, "scaleX", 1f, scale, 1f)
    val scaleYAnimator = ObjectAnimator.ofFloat(this, "scaleY", 1f, scale, 1f)

    scaleXAnimator.duration = duration
    scaleYAnimator.duration = duration
    scaleXAnimator.repeatCount = ObjectAnimator.INFINITE
    scaleYAnimator.repeatCount = ObjectAnimator.INFINITE

    scaleXAnimator.start()
    scaleYAnimator.start()
}

/**
 * Stop pulse animation
 */
fun View.stopPulse() {
    animate()
        .scaleX(1f)
        .scaleY(1f)
        .setDuration(100)
        .start()
}

/**
 * Load animation from resource
 */
fun View.loadAnimation(@AnimRes animRes: Int): Animation {
    return AnimationUtils.loadAnimation(context, animRes)
}

/**
 * Start animation from resource
 */
fun View.startAnimation(@AnimRes animRes: Int) {
    startAnimation(loadAnimation(animRes))
}

/**
 * Enable view
 */
fun View.enable() {
    isEnabled = true
    alpha = 1f
}

/**
 * Disable view
 */
fun View.disable() {
    isEnabled = false
    alpha = 0.5f
}

/**
 * Set enabled with alpha
 */
fun View.setEnabledWithAlpha(enabled: Boolean) {
    isEnabled = enabled
    alpha = if (enabled) 1f else 0.5f
}

/**
 * Set click listener with debounce
 */
fun View.setOnSafeClickListener(
    debounceTime: Long = 500L,
    action: (View) -> Unit
) {
    setOnClickListener(object : View.OnClickListener {
        private var lastClickTime: Long = 0

        override fun onClick(v: View) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastClickTime >= debounceTime) {
                lastClickTime = currentTime
                action(v)
            }
        }
    })
}

/**
 * Remove click listener
 */
fun View.removeClickListener() {
    setOnClickListener(null)
    isClickable = false
}

/**
 * Set padding for all sides
 */
fun View.setPaddingAll(padding: Int) {
    setPadding(padding, padding, padding, padding)
}

/**
 * Set padding with dp
 */
fun View.setPaddingDp(
    left: Int = 0,
    top: Int = 0,
    right: Int = 0,
    bottom: Int = 0
) {
    val l = context.dpToPx(left)
    val t = context.dpToPx(top)
    val r = context.dpToPx(right)
    val b = context.dpToPx(bottom)
    setPadding(l, t, r, b)
}

/**
 * Set margins
 */
fun View.setMargins(left: Int, top: Int, right: Int, bottom: Int) {
    if (layoutParams is ViewGroup.MarginLayoutParams) {
        val params = layoutParams as ViewGroup.MarginLayoutParams
        params.setMargins(left, top, right, bottom)
        layoutParams = params
    }
}

/**
 * Set margins with dp
 */
fun View.setMarginsDp(left: Int = 0, top: Int = 0, right: Int = 0, bottom: Int = 0) {
    setMargins(
        context.dpToPx(left),
        context.dpToPx(top),
        context.dpToPx(right),
        context.dpToPx(bottom)
    )
}

/**
 * Get view location on screen
 */
fun View.getLocationOnScreen(): IntArray {
    val location = IntArray(2)
    getLocationOnScreen(location)
    return location
}

/**
 * Check if view is visible on screen
 */
fun View.isVisibleOnScreen(): Boolean {
    if (!isShown) return false
    val actualPosition = Rect()
    val isGlobalVisible = getGlobalVisibleRect(actualPosition)
    val screenWidth = context.getScreenWidth()
    val screenHeight = context.getScreenHeight()
    val screen = Rect(0, 0, screenWidth, screenHeight)
    return isGlobalVisible && Rect.intersects(actualPosition, screen)
}

/**
 * Request focus and show keyboard
 */
fun EditText.focusAndShowKeyboard() {
    requestFocus()
    post {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
    }
}

/**
 * Clear focus and hide keyboard
 */
fun EditText.clearFocusAndHideKeyboard() {
    clearFocus()
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(windowToken, 0)
}

/**
 * Hide keyboard for Activity
 */
fun Activity.hideKeyboard() {
    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    currentFocus?.let {
        imm.hideSoftInputFromWindow(it.windowToken, 0)
    }
}

/**
 * Hide keyboard for Fragment/View
 */
fun View.hideKeyboard() {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(windowToken, 0)
}

/**
 * Get trimmed text from EditText
 */
fun EditText.getTrimmedText(): String {
    return text?.toString()?.trim() ?: ""
}

/**
 * Check if EditText is empty
 */
fun EditText.isEmpty(): Boolean {
    return getTrimmedText().isEmpty()
}

/**
 * Set text without triggering text watcher
 */
fun EditText.setTextSilently(text: String) {
    val currentText = this.text?.toString() ?: ""
    if (currentText != text) {
        this.setText(text)
        this.setSelection(text.length)
    }
}

/**
 * Apply window insets to view
 */
fun View.applySystemWindowInsets(
    applyTop: Boolean = false,
    applyBottom: Boolean = false,
    applyLeft: Boolean = false,
    applyRight: Boolean = false
) {
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
        val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

        view.setPadding(
            if (applyLeft) insets.left else view.paddingLeft,
            if (applyTop) insets.top else view.paddingTop,
            if (applyRight) insets.right else view.paddingRight,
            if (applyBottom) insets.bottom else view.paddingBottom
        )

        windowInsets
    }
}