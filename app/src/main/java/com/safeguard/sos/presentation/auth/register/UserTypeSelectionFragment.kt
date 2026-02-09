// presentation/auth/register/UserTypeSelectionFragment.kt
package com.safeguard.sos.presentation.auth.register

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.safeguard.sos.R
import com.safeguard.sos.core.base.BaseFragment
import com.safeguard.sos.databinding.FragmentUserTypeSelectionBinding
import com.safeguard.sos.domain.model.UserType
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class UserTypeSelectionFragment : BaseFragment<FragmentUserTypeSelectionBinding>() {

    private val registerViewModel: RegisterViewModel by activityViewModels()
    private var selectedUserType: UserType? = null

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentUserTypeSelectionBinding {
        return FragmentUserTypeSelectionBinding.inflate(inflater, container, false)
    }

    override fun setupViews() {
        // Set initial state - nothing selected
        updateSelectionUI(null)
    }

    override fun setupClickListeners() {
        binding.cardUserType.setOnClickListener {
            selectedUserType = UserType.REGULAR
            updateSelectionUI(UserType.REGULAR)
        }

        binding.cardHelperType.setOnClickListener {
            selectedUserType = UserType.HELPER
            updateSelectionUI(UserType.HELPER)
        }

        binding.cardBothType.setOnClickListener {
            selectedUserType = UserType.BOTH
            updateSelectionUI(UserType.BOTH)
        }

        binding.btnContinue.setOnClickListener {
            selectedUserType?.let { type ->
                registerViewModel.setUserType(type)
                findNavController().navigate(
                    R.id.action_userTypeSelection_to_aadhaarVerification
                )
            } ?: run {
                showToast(getString(R.string.please_select_user_type))
            }
        }

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun updateSelectionUI(type: UserType?) {
        // Reset all cards to unselected state
        binding.cardUserType.apply {
            strokeWidth = resources.getDimensionPixelSize(R.dimen.card_stroke_unselected)
            strokeColor = getColorCompat(com.google.android.material.R.color.material_dynamic_neutral90)
            setCardBackgroundColor(getColorCompat(android.R.color.transparent))
        }
        binding.cardHelperType.apply {
            strokeWidth = resources.getDimensionPixelSize(R.dimen.card_stroke_unselected)
            strokeColor = getColorCompat(com.google.android.material.R.color.material_dynamic_neutral90)
            setCardBackgroundColor(getColorCompat(android.R.color.transparent))
        }
        binding.cardBothType.apply {
            strokeWidth = resources.getDimensionPixelSize(R.dimen.card_stroke_unselected)
            strokeColor = getColorCompat(com.google.android.material.R.color.material_dynamic_neutral90)
            setCardBackgroundColor(getColorCompat(android.R.color.transparent))
        }

        binding.ivCheckUser.visibility = View.GONE
        binding.ivCheckHelper.visibility = View.GONE
        binding.ivCheckBoth.visibility = View.GONE

        // Highlight selected card
        when (type) {
            UserType.REGULAR -> {
                binding.cardUserType.apply {
                    strokeWidth = resources.getDimensionPixelSize(R.dimen.card_stroke_selected)
                    strokeColor = getColorCompat(R.color.accent)
                    // setCardBackgroundColor(getColorCompat(R.color.surface_selected))
                }
                binding.ivCheckUser.visibility = View.VISIBLE
            }
            UserType.HELPER -> {
                binding.cardHelperType.apply {
                    strokeWidth = resources.getDimensionPixelSize(R.dimen.card_stroke_selected)
                    strokeColor = getColorCompat(R.color.accent)
                    // setCardBackgroundColor(getColorCompat(R.color.surface_selected))
                }
                binding.ivCheckHelper.visibility = View.VISIBLE
            }
            UserType.BOTH -> {
                binding.cardBothType.apply {
                    strokeWidth = resources.getDimensionPixelSize(R.dimen.card_stroke_selected)
                    strokeColor = getColorCompat(R.color.accent)
                    // setCardBackgroundColor(getColorCompat(R.color.surface_selected))
                }
                binding.ivCheckBoth.visibility = View.VISIBLE
            }
            null -> { /* Nothing selected */ }
        }

        // Enable/disable continue button
        binding.btnContinue.isEnabled = type != null
        binding.btnContinue.alpha = if (type != null) 1.0f else 0.5f
    }

    private fun getColorCompat(colorRes: Int): Int {
        return androidx.core.content.ContextCompat.getColor(requireContext(), colorRes)
    }

    override fun observeData() {
        // Restore previously selected type if navigating back
        registerViewModel.selectedUserType.value?.let { type ->
            selectedUserType = type
            updateSelectionUI(type)
        }
    }
}
