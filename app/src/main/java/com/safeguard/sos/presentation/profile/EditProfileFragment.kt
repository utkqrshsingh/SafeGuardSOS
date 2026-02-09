package com.safeguard.sos.presentation.profile

import android.app.DatePickerDialog
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.safeguard.sos.R
import com.safeguard.sos.core.base.BaseFragment
import com.safeguard.sos.databinding.FragmentEditProfileBinding
import com.safeguard.sos.presentation.components.dialogs.ConfirmationDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Calendar

@AndroidEntryPoint
class EditProfileFragment : BaseFragment<FragmentEditProfileBinding>() {

    private val viewModel: ProfileViewModel by viewModels()

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            binding.imageProfile.setImageURI(it)
            viewModel.onProfileImageSelected(it.toString())
        }
    }

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentEditProfileBinding {
        return FragmentEditProfileBinding.inflate(inflater, container, false)
    }

    override fun setupViews() {
        setupInputFields()
        setupDropdowns()
    }

    override fun setupClickListeners() {
        binding.apply {
            toolbar.setNavigationOnClickListener {
                handleBackPress()
            }

            // Profile Image
            cardProfileImage.setOnClickListener {
                showImagePickerOptions()
            }

            buttonChangePhoto.setOnClickListener {
                showImagePickerOptions()
            }

            // Date of Birth
            textInputLayoutDateOfBirth.setEndIconOnClickListener {
                showDatePicker()
            }
            editTextDateOfBirth.setOnClickListener {
                showDatePicker()
            }

            // Save Button
            buttonSave.setOnClickListener {
                viewModel.saveProfile()
            }

            // Cancel Button
            buttonCancel.setOnClickListener {
                handleBackPress()
            }
        }
    }

    private fun handleBackPress() {
        val state = viewModel.editProfileState.value
        if (state.hasChanges) {
            showDiscardChangesDialog()
        } else {
            navController.navigateUp()
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
                    navController.navigateUp()
                }
            )
        )
    }

    private fun setupInputFields() {
        binding.apply {
            editTextName.doAfterTextChanged { text ->
                viewModel.onEditNameChanged(text?.toString() ?: "")
            }

            editTextEmail.doAfterTextChanged { text ->
                viewModel.onEditEmailChanged(text?.toString() ?: "")
            }

            editTextPhone.doAfterTextChanged { text ->
                viewModel.onEditPhoneChanged(text?.toString() ?: "")
            }

            editTextAddress.doAfterTextChanged { text ->
                viewModel.onEditAddressChanged(text?.toString() ?: "")
            }

            editTextCity.doAfterTextChanged { text ->
                viewModel.onEditCityChanged(text?.toString() ?: "")
            }

            editTextState.doAfterTextChanged { text ->
                viewModel.onEditStateChanged(text?.toString() ?: "")
            }

            editTextPincode.doAfterTextChanged { text ->
                viewModel.onEditPincodeChanged(text?.toString() ?: "")
            }
        }
    }

    private fun setupDropdowns() {
        // Blood Group Dropdown
        val bloodGroups = listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")
        val bloodGroupAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            bloodGroups
        )
        binding.autoCompleteBloodGroup.setAdapter(bloodGroupAdapter)
        binding.autoCompleteBloodGroup.setOnItemClickListener { _, _, position, _ ->
            viewModel.onEditBloodGroupChanged(bloodGroups[position])
        }

        // Gender Dropdown
        val genders = listOf("Male", "Female", "Other", "Prefer not to say")
        val genderAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            genders
        )
        binding.autoCompleteGender.setAdapter(genderAdapter)
        binding.autoCompleteGender.setOnItemClickListener { _, _, position, _ ->
            viewModel.onEditGenderChanged(genders[position])
        }
    }

    private fun showImagePickerOptions() {
        val options = arrayOf("Choose from Gallery", "Remove Photo")

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Profile Photo")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openImagePicker()
                    1 -> {
                        binding.imageProfile.setImageResource(android.R.drawable.ic_menu_report_image)
                        viewModel.onProfileImageSelected("")
                    }
                }
            }
            .show()
    }

    private fun openImagePicker() {
        imagePickerLauncher.launch("image/*")
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                val date = String.format("%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear)
                binding.editTextDateOfBirth.setText(date)
                viewModel.onEditDateOfBirthChanged(date)
            },
            year - 18, // Default to 18 years ago
            month,
            day
        ).apply {
            datePicker.maxDate = System.currentTimeMillis()
            datePicker.minDate = calendar.apply { add(Calendar.YEAR, -100) }.timeInMillis
        }.show()
    }

    override fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.editProfileState.collectLatest { state ->
                    updateUI(state)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collectLatest { event ->
                    when (event) {
                        is ProfileEvent.ProfileUpdated -> {
                            navController.navigateUp()
                        }
                        is ProfileEvent.ShowError -> {
                            showToast(event.message)
                        }
                        is ProfileEvent.ShowSuccess -> {
                            showToast(event.message)
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    private fun updateUI(state: EditProfileUiState) {
        binding.apply {
            progressBar.isVisible = state.isLoading || state.isSaving
            // contentLayout.alpha = if (state.isSaving) 0.5f else 1f

            // Update fields only if not focused
            if (!editTextName.isFocused && editTextName.text.toString() != state.name) {
                editTextName.setText(state.name)
            }
            if (!editTextEmail.isFocused && editTextEmail.text.toString() != state.email) {
                editTextEmail.setText(state.email)
            }
            if (!editTextPhone.isFocused && editTextPhone.text.toString() != state.phone) {
                editTextPhone.setText(state.phone)
            }
            if (!editTextAddress.isFocused && editTextAddress.text.toString() != state.address) {
                editTextAddress.setText(state.address)
            }
            if (!editTextCity.isFocused && editTextCity.text.toString() != state.city) {
                editTextCity.setText(state.city)
            }
            if (!editTextState.isFocused && editTextState.text.toString() != state.state) {
                editTextState.setText(state.state)
            }
            if (!editTextPincode.isFocused && editTextPincode.text.toString() != state.pincode) {
                editTextPincode.setText(state.pincode)
            }
            if (autoCompleteBloodGroup.text.toString() != state.bloodGroup) {
                autoCompleteBloodGroup.setText(state.bloodGroup, false)
            }
            if (autoCompleteGender.text.toString() != state.gender) {
                autoCompleteGender.setText(state.gender, false)
            }
            if (editTextDateOfBirth.text.toString() != state.dateOfBirth) {
                editTextDateOfBirth.setText(state.dateOfBirth)
            }

            // Errors
            textInputLayoutName.error = state.nameError
            textInputLayoutEmail.error = state.emailError
            textInputLayoutPhone.error = state.phoneError

            // Button state
            buttonSave.isEnabled = state.isValid && state.hasChanges && !state.isSaving
            buttonSave.alpha = if (state.isValid && state.hasChanges && !state.isSaving) 1f else 0.5f
            buttonSave.text = when {
                state.isSaving -> "Saving..."
                state.hasChanges -> "Save Changes"
                else -> "No Changes"
            }
        }
    }
}
