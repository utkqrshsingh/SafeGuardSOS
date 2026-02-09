package com.safeguard.sos.presentation.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.safeguard.sos.R
import com.safeguard.sos.core.base.BaseFragment
import com.safeguard.sos.databinding.FragmentMedicalInfoBinding
import com.safeguard.sos.presentation.components.dialogs.ConfirmationDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MedicalInfoFragment : BaseFragment<FragmentMedicalInfoBinding>() {

    private val viewModel: ProfileViewModel by viewModels({ requireParentFragment().requireParentFragment() })

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentMedicalInfoBinding {
        return FragmentMedicalInfoBinding.inflate(inflater, container, false)
    }

    override fun setupViews() {
        setupToolbar()
        setupDropdowns()
        setupInputFields()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            handleBackPress()
        }
    }

    private fun handleBackPress() {
        val state = viewModel.medicalInfoState.value
        if (state.hasChanges) {
            showDiscardChangesDialog()
        } else {
            findNavController().navigateUp()
        }
    }

    private fun showDiscardChangesDialog() {
        ConfirmationDialog.show(
            requireContext(),
            ConfirmationDialog.Config(
                title = "Discard Changes?",
                message = "You have unsaved changes. Are you sure you want to discard them?",
                positiveButtonText = "Discard",
                negativeButtonText = "Keep Editing",
                onPositiveClick = {
                    findNavController().navigateUp()
                }
            )
        )
    }

    private fun setupDropdowns() {
        val bloodGroups = listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, bloodGroups)
        binding.autoCompleteBloodGroup.setAdapter(adapter)
        binding.autoCompleteBloodGroup.setOnItemClickListener { _, _, position, _ ->
            viewModel.onBloodGroupChanged(bloodGroups[position])
        }
    }

    private fun setupInputFields() {
        binding.apply {
            editTextEmergencyNotes.doAfterTextChanged { text ->
                viewModel.onEmergencyNotesChanged(text?.toString() ?: "")
            }

            editTextInsuranceProvider.doAfterTextChanged { text ->
                viewModel.onInsuranceProviderChanged(text?.toString() ?: "")
            }

            editTextPolicyNumber.doAfterTextChanged { text ->
                viewModel.onInsurancePolicyNumberChanged(text?.toString() ?: "")
            }

            editTextPhysicianName.doAfterTextChanged { text ->
                viewModel.onPrimaryPhysicianChanged(text?.toString() ?: "")
            }

            editTextPhysicianPhone.doAfterTextChanged { text ->
                viewModel.onPhysicianPhoneChanged(text?.toString() ?: "")
            }

            switchOrganDonor.setOnCheckedChangeListener { _, isChecked ->
                viewModel.onOrganDonorChanged(isChecked)
            }
        }
    }

    override fun setupClickListeners() {
        binding.apply {
            // Add Allergy
            buttonAddAllergy.setOnClickListener {
                showAddItemDialog("Add Allergy", "Enter allergy") { allergy ->
                    viewModel.addAllergy(allergy)
                }
            }

            // Add Medication
            buttonAddMedication.setOnClickListener {
                showAddItemDialog("Add Medication", "Enter medication name") { medication ->
                    viewModel.addMedication(medication)
                }
            }

            // Add Condition
            buttonAddCondition.setOnClickListener {
                showAddItemDialog("Add Medical Condition", "Enter condition") { condition ->
                    viewModel.addCondition(condition)
                }
            }

            // Save Button
            buttonSave.setOnClickListener {
                viewModel.saveMedicalInfo()
            }

            // Cancel Button
            buttonCancel.setOnClickListener {
                handleBackPress()
            }
        }
    }

    private fun showAddItemDialog(title: String, hint: String, onAdd: (String) -> Unit) {
        val editText = TextInputEditText(requireContext()).apply {
            this.hint = hint
            setPadding(48, 32, 48, 32)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setView(editText)
            .setPositiveButton("Add") { _, _ ->
                val text = editText.text?.toString()?.trim()
                if (!text.isNullOrEmpty()) {
                    onAdd(text)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.medicalInfoState.collectLatest { state ->
                    updateUI(state)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collectLatest { event ->
                    when (event) {
                        is ProfileEvent.ShowError -> {
                            showSnackbar(event.message)
                        }
                        is ProfileEvent.ShowSuccess -> {
                            showSnackbar(event.message)
                            if (event.message.contains("saved", ignoreCase = true)) {
                                findNavController().navigateUp()
                            }
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    private fun updateUI(state: MedicalInfoUiState) {
        binding.apply {
            progressBar.isVisible = state.isLoading || state.isSaving
            contentLayout.alpha = if (state.isSaving) 0.5f else 1f

            // Blood Group
            if (autoCompleteBloodGroup.text.toString() != state.bloodGroup) {
                autoCompleteBloodGroup.setText(state.bloodGroup, false)
            }

            // Allergies Chips
            chipGroupAllergies.removeAllViews()
            state.allergies.forEach { allergy ->
                val chip = createRemovableChip(allergy) {
                    viewModel.removeAllergy(allergy)
                }
                chipGroupAllergies.addView(chip)
            }
            textNoAllergies.isVisible = state.allergies.isEmpty()

            // Medications Chips
            chipGroupMedications.removeAllViews()
            state.medications.forEach { medication ->
                val chip = createRemovableChip(medication) {
                    viewModel.removeMedication(medication)
                }
                chipGroupMedications.addView(chip)
            }
            textNoMedications.isVisible = state.medications.isEmpty()

            // Conditions Chips
            chipGroupConditions.removeAllViews()
            state.medicalConditions.forEach { condition ->
                val chip = createRemovableChip(condition) {
                    viewModel.removeCondition(condition)
                }
                chipGroupConditions.addView(chip)
            }
            textNoConditions.isVisible = state.medicalConditions.isEmpty()

            // Other fields
            if (!editTextEmergencyNotes.isFocused && editTextEmergencyNotes.text.toString() != state.emergencyNotes) {
                editTextEmergencyNotes.setText(state.emergencyNotes)
            }
            if (switchOrganDonor.isChecked != state.organDonor) {
                switchOrganDonor.isChecked = state.organDonor
            }
            if (!editTextInsuranceProvider.isFocused && editTextInsuranceProvider.text.toString() != state.insuranceProvider) {
                editTextInsuranceProvider.setText(state.insuranceProvider)
            }
            if (!editTextPolicyNumber.isFocused && editTextPolicyNumber.text.toString() != state.insurancePolicyNumber) {
                editTextPolicyNumber.setText(state.insurancePolicyNumber)
            }
            if (!editTextPhysicianName.isFocused && editTextPhysicianName.text.toString() != state.primaryPhysician) {
                editTextPhysicianName.setText(state.primaryPhysician)
            }
            if (!editTextPhysicianPhone.isFocused && editTextPhysicianPhone.text.toString() != state.physicianPhone) {
                editTextPhysicianPhone.setText(state.physicianPhone)
            }

            // Button state
            buttonSave.isEnabled = state.hasChanges && !state.isSaving
            buttonSave.alpha = if (state.hasChanges && !state.isSaving) 1f else 0.5f
            buttonSave.text = when {
                state.isSaving -> "Saving..."
                state.hasChanges -> "Save Changes"
                else -> "No Changes"
            }
        }
    }

    private fun createRemovableChip(text: String, onRemove: () -> Unit): Chip {
        return Chip(requireContext()).apply {
            this.text = text
            isCloseIconVisible = true
            // setChipBackgroundColorResource(R.color.chip_background)
            // setTextColor(requireContext().getColor(R.color.text_primary))
            // setCloseIconTintResource(R.color.text_secondary)
            setOnCloseIconClickListener {
                onRemove()
            }
        }
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }
}
