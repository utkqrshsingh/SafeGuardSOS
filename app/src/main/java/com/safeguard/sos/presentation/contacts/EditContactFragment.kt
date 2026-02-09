package com.safeguard.sos.presentation.contacts

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
import androidx.navigation.fragment.navArgs
import com.safeguard.sos.R
import com.safeguard.sos.core.base.BaseFragment
import com.safeguard.sos.databinding.FragmentEditContactBinding
import com.safeguard.sos.domain.model.Relationship
import com.safeguard.sos.presentation.components.dialogs.ConfirmationDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class EditContactFragment : BaseFragment<FragmentEditContactBinding>() {

    private val viewModel: ContactsViewModel by viewModels()
    private val args: EditContactFragmentArgs by navArgs()

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentEditContactBinding {
        return FragmentEditContactBinding.inflate(inflater, container, false)
    }

    override fun setupViews() {
        viewModel.loadContactForEdit(args.contactId)
        setupInputFields()
        setupRelationshipDropdown()
        setupSwitches()
    }

    override fun setupClickListeners() {
        binding.apply {
            toolbar.setNavigationOnClickListener {
                handleBackPress()
            }

            toolbar.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_delete -> {
                        showDeleteConfirmation()
                        true
                    }
                    else -> false
                }
            }

            buttonSave.setOnClickListener {
                viewModel.saveEditedContact()
            }

            buttonCancel.setOnClickListener {
                handleBackPress()
            }
        }
    }

    private fun handleBackPress() {
        val state = viewModel.editContactState.value
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

    private fun showDeleteConfirmation() {
        val contact = viewModel.editContactState.value.originalContact
        ConfirmationDialog.show(
            requireContext(),
            ConfirmationDialog.Config(
                title = "Delete Contact",
                message = "Are you sure you want to delete ${contact?.name ?: "this contact"}?",
                positiveButtonText = "Delete",
                negativeButtonText = "Cancel",
                isDanger = true,
                onPositiveClick = {
                    viewModel.deleteContact(args.contactId)
                }
            )
        )
    }

    private fun setupInputFields() {
        binding.apply {
            editTextName.doAfterTextChanged { text ->
                viewModel.onEditNameChanged(text?.toString() ?: "")
            }

            editTextPhone.doAfterTextChanged { text ->
                viewModel.onEditPhoneChanged(text?.toString() ?: "")
            }

            editTextEmail.doAfterTextChanged { text ->
                viewModel.onEditEmailChanged(text?.toString() ?: "")
            }
        }
    }

    private fun setupRelationshipDropdown() {
        val relationships = Relationship.getAll().map { it.displayName }

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            relationships
        )

        binding.autoCompleteRelationship.setAdapter(adapter)
        binding.autoCompleteRelationship.setOnItemClickListener { _, _, position, _ ->
            viewModel.onEditRelationshipChanged(relationships[position])
        }
    }

    private fun setupSwitches() {
        binding.apply {
            switchPrimary.setOnCheckedChangeListener { _, isChecked ->
                viewModel.onEditPrimaryChanged(isChecked)
            }

            switchNotifyOnSOS.setOnCheckedChangeListener { _, isChecked ->
                viewModel.onEditNotifyOnSOSChanged(isChecked)
            }

            switchShareLocation.setOnCheckedChangeListener { _, isChecked ->
                viewModel.onEditShareLocationChanged(isChecked)
            }
        }
    }

    override fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.editContactState.collectLatest { state ->
                    updateUI(state)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collectLatest { event ->
                    when (event) {
                        is ContactsEvent.ContactUpdated -> {
                            navController.navigateUp()
                        }
                        is ContactsEvent.ContactDeleted -> {
                            navController.navigateUp()
                        }
                        is ContactsEvent.ShowError -> {
                            showToast(event.message)
                        }
                        is ContactsEvent.ShowSuccess -> {
                            showToast(event.message)
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    private fun updateUI(state: EditContactUiState) {
        binding.apply {
            progressBar.isVisible = state.isLoading

            // Only update fields if not currently focused (to avoid cursor issues)
            if (!editTextName.isFocused && editTextName.text.toString() != state.name) {
                editTextName.setText(state.name)
            }
            if (!editTextPhone.isFocused && editTextPhone.text.toString() != state.phone) {
                editTextPhone.setText(state.phone)
            }
            if (!editTextEmail.isFocused && editTextEmail.text.toString() != state.email) {
                editTextEmail.setText(state.email)
            }
            if (autoCompleteRelationship.text.toString() != state.relationship) {
                autoCompleteRelationship.setText(state.relationship, false)
            }

            // Update button state
            buttonSave.isEnabled = state.isValid && state.hasChanges && !state.isLoading
            buttonSave.alpha = if (state.isValid && state.hasChanges && !state.isLoading) 1f else 0.5f
            buttonSave.text = if (state.hasChanges) "Save Changes" else "No Changes"

            // Show errors
            textInputLayoutName.error = state.nameError
            textInputLayoutPhone.error = state.phoneError
            textInputLayoutRelationship.error = state.relationshipError
            textInputLayoutEmail.error = state.emailError

            // Update switches without triggering listeners
            if (switchPrimary.isChecked != state.isPrimary) {
                switchPrimary.isChecked = state.isPrimary
            }
            if (switchNotifyOnSOS.isChecked != state.notifyOnSOS) {
                switchNotifyOnSOS.isChecked = state.notifyOnSOS
            }
            if (switchShareLocation.isChecked != state.shareLocation) {
                switchShareLocation.isChecked = state.shareLocation
            }

            // Show last updated time
            state.originalContact?.updatedAt?.let { timestamp ->
                textLastUpdated.isVisible = true
                textLastUpdated.text = getString(
                    R.string.last_updated_format,
                    formatTimestamp(timestamp)
                )
            } ?: run {
                textLastUpdated.isVisible = false
            }
        }
    }

    private fun formatTimestamp(timestamp: Long): String {
        val sdf = java.text.SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestamp))
    }
}
