// app/src/main/java/com/safeguard/sos/presentation/components/dialogs/ConfirmationDialog.kt

package com.safeguard.sos.presentation.components.dialogs

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Window
import android.view.WindowManager
import androidx.annotation.DrawableRes
import androidx.core.view.isVisible
import com.safeguard.sos.R
import com.safeguard.sos.databinding.DialogConfirmationBinding

/**
 * Custom confirmation dialog
 */
class ConfirmationDialog(
    context: Context,
    private val config: Config
) : Dialog(context, R.style.Theme_SafeGuardSOS_Dialog) {

    private lateinit var binding: DialogConfirmationBinding

    data class Config(
        val title: String,
        val message: String,
        val positiveButtonText: String = "Confirm",
        val negativeButtonText: String? = "Cancel",
        @DrawableRes val iconRes: Int? = null,
        val iconTint: Int? = null,
        val isDanger: Boolean = false,
        val onPositiveClick: () -> Unit = {},
        val onNegativeClick: () -> Unit = {},
        val cancelable: Boolean = true
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        binding = DialogConfirmationBinding.inflate(LayoutInflater.from(context))
        setContentView(binding.root)

        // Configure window
        window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
            setDimAmount(0.6f)
        }

        setCancelable(config.cancelable)
        setCanceledOnTouchOutside(config.cancelable)

        setupViews()
        setupClickListeners()
    }

    private fun setupViews() {
        with(binding) {
            tvTitle.text = config.title
            tvMessage.text = config.message
            btnPositive.text = config.positiveButtonText

            // Icon
            if (config.iconRes != null) {
                ivIcon.isVisible = true
                ivIcon.setImageResource(config.iconRes)
                config.iconTint?.let { ivIcon.setColorFilter(it) }
            } else {
                ivIcon.isVisible = false
            }

            // Negative button
            if (config.negativeButtonText != null) {
                btnNegative.isVisible = true
                btnNegative.text = config.negativeButtonText
            } else {
                btnNegative.isVisible = false
            }

            // Danger styling
            if (config.isDanger) {
                btnPositive.setBackgroundColor(context.getColor(R.color.status_error))
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnPositive.setOnClickListener {
            config.onPositiveClick()
            dismiss()
        }

        binding.btnNegative.setOnClickListener {
            config.onNegativeClick()
            dismiss()
        }
    }

    companion object {
        fun show(context: Context, config: Config): ConfirmationDialog {
            return ConfirmationDialog(context, config).also { it.show() }
        }
    }
}