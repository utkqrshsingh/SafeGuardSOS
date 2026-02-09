// app/src/main/java/com/safeguard/sos/presentation/components/dialogs/LoadingDialog.kt

package com.safeguard.sos.presentation.components.dialogs

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Window
import android.view.WindowManager
import com.safeguard.sos.R
import com.safeguard.sos.databinding.DialogLoadingBinding

/**
 * Custom loading dialog with animated indicator
 */
class LoadingDialog(context: Context) : Dialog(context, R.style.Theme_SafeGuardSOS_Dialog) {

    private lateinit var binding: DialogLoadingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        binding = DialogLoadingBinding.inflate(LayoutInflater.from(context))
        setContentView(binding.root)

        // Configure window
        window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
            setDimAmount(0.6f)
        }

        setCancelable(false)
        setCanceledOnTouchOutside(false)
    }

    /**
     * Show dialog with custom message
     */
    fun show(message: String) {
        if (!isShowing) {
            show()
        }
        binding.tvLoadingMessage.text = message
    }

    /**
     * Update loading message
     */
    fun updateMessage(message: String) {
        if (isShowing) {
            binding.tvLoadingMessage.text = message
        }
    }
}