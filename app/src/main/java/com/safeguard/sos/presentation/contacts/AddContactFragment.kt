package com.safeguard.sos.presentation.contacts

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.provider.ContactsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.safeguard.sos.R
import com.safeguard.sos.core.base.BaseFragment
import com.safeguard.sos.databinding.FragmentAddContactBinding
import com.safeguard.sos.domain.model.Relationship
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AddContactFragment : BaseFragment<FragmentAddContactBinding>() {

    private val viewModel: ContactsViewModel by viewModels()

    private val contactPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == AppCompatActivity.RESULT_OK) {
            result.data?.data?.let { uri ->
                loadContactFromUri(uri)
            }
        }
    }

    private val contactPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openContactPicker()
        } else {
            showToast("Contact permission is required to import contacts")
        }
    }

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentAddContactBinding {
        return FragmentAddContactBinding.inflate(inflater, container, false)
    }

    override fun setupViews() {
        viewModel.resetAddContactState()
        setupInputFields()
        setupRelationshipDropdown()
        setupSwitches()
    }

    private fun setupInputFields() {
        binding.apply {
            editTextName.doAfterTextChanged { text ->
                viewModel.onAddNameChanged(text?.toString() ?: "")
            }

            editTextPhone.doAfterTextChanged { text ->
                viewModel.onAddPhoneChanged(text?.toString() ?: "")
            }

            editTextEmail.doAfterTextChanged { text ->
                viewModel.onAddEmailChanged(text?.toString() ?: "")
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
            viewModel.onAddRelationshipChanged(relationships[position])
        }
    }

    private fun setupSwitches() {
        binding.apply {
            switchPrimary.setOnCheckedChangeListener { _, isChecked ->
                viewModel.onAddPrimaryChanged(isChecked)
            }

            switchNotifyOnSOS.setOnCheckedChangeListener { _, isChecked ->
                viewModel.onAddNotifyOnSOSChanged(isChecked)
            }

            switchShareLocation.setOnCheckedChangeListener { _, isChecked ->
                viewModel.onAddShareLocationChanged(isChecked)
            }
        }
    }

    override fun setupClickListeners() {
        binding.apply {
            buttonImportContact.setOnClickListener {
                requestContactPermission()
            }

            buttonSave.setOnClickListener {
                viewModel.saveNewContact()
            }

            buttonCancel.setOnClickListener {
                navController.navigateUp()
            }
            
            toolbar.setNavigationOnClickListener {
                navController.navigateUp()
            }
        }
    }

    private fun requestContactPermission() {
        contactPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
    }

    private fun openContactPicker() {
        val intent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
        contactPickerLauncher.launch(intent)
    }

    private fun loadContactFromUri(uri: android.net.Uri) {
        try {
            requireContext().contentResolver.query(
                uri,
                arrayOf(
                    ContactsContract.Contacts._ID,
                    ContactsContract.Contacts.DISPLAY_NAME,
                    ContactsContract.Contacts.HAS_PHONE_NUMBER
                ),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val id = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID))
                    val name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME))
                    val hasPhone = cursor.getInt(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.HAS_PHONE_NUMBER))

                    binding.editTextName.setText(name)
                    viewModel.onAddNameChanged(name)

                    if (hasPhone > 0) {
                        loadPhoneNumber(id)
                    }
                }
            }
        } catch (e: Exception) {
            showToast("Failed to load contact details")
        }
    }

    private fun loadPhoneNumber(contactId: String) {
        requireContext().contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
            arrayOf(contactId),
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val phone = cursor.getString(
                    cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
                )
                binding.editTextPhone.setText(phone.replace(Regex("[^+0-9]"), ""))
                viewModel.onAddPhoneChanged(phone.replace(Regex("[^+0-9]"), ""))
            }
        }
    }

    override fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.addContactState.collectLatest { state ->
                    updateUI(state)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collectLatest { event ->
                    when (event) {
                        is ContactsEvent.ContactAdded -> {
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

    private fun updateUI(state: AddContactUiState) {
        binding.apply {
            progressBar.isVisible = state.isLoading
            buttonSave.isEnabled = state.isValid && !state.isLoading
            buttonSave.alpha = if (state.isValid && !state.isLoading) 1f else 0.5f

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
        }
    }
}
