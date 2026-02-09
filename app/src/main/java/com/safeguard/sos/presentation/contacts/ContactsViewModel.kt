package com.safeguard.sos.presentation.contacts

import androidx.lifecycle.viewModelScope
import com.safeguard.sos.core.base.BaseViewModel
import com.safeguard.sos.core.common.Resource
import com.safeguard.sos.domain.model.EmergencyContact
import com.safeguard.sos.domain.model.Relationship
import com.safeguard.sos.domain.usecase.contact.AddEmergencyContactUseCase
import com.safeguard.sos.domain.usecase.contact.GetEmergencyContactsUseCase
import com.safeguard.sos.domain.usecase.contact.DeleteEmergencyContactUseCase
import com.safeguard.sos.domain.usecase.contact.UpdateEmergencyContactUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ContactsViewModel @Inject constructor(
    private val getEmergencyContactsUseCase: GetEmergencyContactsUseCase,
    private val addEmergencyContactUseCase: AddEmergencyContactUseCase,
    private val updateEmergencyContactUseCase: UpdateEmergencyContactUseCase,
    private val deleteEmergencyContactUseCase: DeleteEmergencyContactUseCase
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(ContactsUiState())
    val uiState: StateFlow<ContactsUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ContactsEvent>()
    val events: SharedFlow<ContactsEvent> = _events.asSharedFlow()

    private val _addContactState = MutableStateFlow(AddContactUiState())
    val addContactState: StateFlow<AddContactUiState> = _addContactState.asStateFlow()

    private val _editContactState = MutableStateFlow(EditContactUiState())
    val editContactState: StateFlow<EditContactUiState> = _editContactState.asStateFlow()

    init {
        loadContacts()
    }

    fun loadContacts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            getEmergencyContactsUseCase().collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                    is Resource.Success -> {
                        val contacts = result.data
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                contacts = contacts,
                                filteredContacts = filterContacts(contacts, it.searchQuery),
                                canAddMore = contacts.size < it.maxContactsLimit,
                                error = null
                            )
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = result.message
                            )
                        }
                        _events.emit(ContactsEvent.ShowError(result.message))
                    }
                    is Resource.Empty -> {
                        _uiState.update { it.copy(isLoading = false) }
                    }
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { state ->
            state.copy(
                searchQuery = query,
                filteredContacts = filterContacts(state.contacts, query)
            )
        }
    }

    private fun filterContacts(contacts: List<EmergencyContact>, query: String): List<EmergencyContact> {
        if (query.isBlank()) return contacts
        return contacts.filter { contact ->
            contact.name.contains(query, ignoreCase = true) ||
                    contact.phoneNumber.contains(query, ignoreCase = true) ||
                    contact.relationship.displayName.contains(query, ignoreCase = true)
        }
    }

    fun onContactClick(contact: EmergencyContact) {
        _uiState.update { it.copy(selectedContact = contact) }
        viewModelScope.launch {
            _events.emit(ContactsEvent.NavigateToEdit(contact.id))
        }
    }

    fun onAddContactClick() {
        if (_uiState.value.canAddMore) {
            resetAddContactState()
            viewModelScope.launch {
                _events.emit(ContactsEvent.NavigateToAdd)
            }
        } else {
            viewModelScope.launch {
                _events.emit(ContactsEvent.ShowError("Maximum ${_uiState.value.maxContactsLimit} contacts allowed"))
            }
        }
    }

    fun deleteContact(contactId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val result = deleteEmergencyContactUseCase(contactId)
            when (result) {
                is Resource.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _events.emit(ContactsEvent.ContactDeleted)
                    _events.emit(ContactsEvent.ShowSuccess("Contact deleted successfully"))
                    loadContacts()
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _events.emit(ContactsEvent.ShowError(result.message))
                }
                else -> {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    fun togglePrimary(contactId: String) {
        val contact = _uiState.value.contacts.find { it.id == contactId } ?: return
        val updatedContact = contact.copy(isPrimary = !contact.isPrimary)

        viewModelScope.launch {
            val result = updateEmergencyContactUseCase(updatedContact)
            when (result) {
                is Resource.Success -> {
                    loadContacts()
                }
                is Resource.Error -> {
                    _events.emit(ContactsEvent.ShowError(result.message))
                }
                else -> {}
            }
        }
    }

    // Add Contact Functions
    fun resetAddContactState() {
        _addContactState.value = AddContactUiState()
    }

    fun onAddNameChanged(name: String) {
        _addContactState.update {
            it.copy(
                name = name,
                nameError = validateName(name),
                isValid = validateAddContactForm(name, it.phone, Relationship.fromValue(it.relationship.lowercase()))
            )
        }
    }

    fun onAddPhoneChanged(phone: String) {
        _addContactState.update {
            it.copy(
                phone = phone,
                phoneError = validatePhone(phone),
                isValid = validateAddContactForm(it.name, phone, Relationship.fromValue(it.relationship.lowercase()))
            )
        }
    }

    fun onAddRelationshipChanged(relationship: String) {
        val rel = Relationship.fromValue(relationship.lowercase())
        _addContactState.update {
            it.copy(
                relationship = rel.displayName,
                relationshipError = validateRelationship(rel),
                isValid = validateAddContactForm(it.name, it.phone, rel)
            )
        }
    }

    fun onAddEmailChanged(email: String) {
        _addContactState.update {
            it.copy(
                email = email,
                emailError = if (email.isNotEmpty()) validateEmail(email) else null
            )
        }
    }

    fun onAddPrimaryChanged(isPrimary: Boolean) {
        _addContactState.update { it.copy(isPrimary = isPrimary) }
    }

    fun onAddNotifyOnSOSChanged(notify: Boolean) {
        _addContactState.update { it.copy(notifyOnSOS = notify) }
    }

    fun onAddShareLocationChanged(share: Boolean) {
        _addContactState.update { it.copy(shareLocation = share) }
    }

    fun saveNewContact() {
        val state = _addContactState.value

        if (!state.isValid) {
            viewModelScope.launch {
                _events.emit(ContactsEvent.ShowError("Please fill all required fields correctly"))
            }
            return
        }

        viewModelScope.launch {
            _addContactState.update { it.copy(isLoading = true) }

            val rel = Relationship.fromValue(state.relationship.lowercase())
            val result = addEmergencyContactUseCase(
                name = state.name.trim(),
                phoneNumber = state.phone.trim(),
                relationship = rel,
                isPrimary = state.isPrimary,
                notifyViaSms = state.notifyOnSOS,
                notifyViaCall = false // Default
            )
            when (result) {
                is Resource.Success -> {
                    _addContactState.update { it.copy(isLoading = false) }
                    _events.emit(ContactsEvent.ContactAdded)
                    _events.emit(ContactsEvent.ShowSuccess("Contact added successfully"))
                    loadContacts()
                }
                is Resource.Error -> {
                    _addContactState.update { it.copy(isLoading = false) }
                    _events.emit(ContactsEvent.ShowError(result.message))
                }
                else -> {
                    _addContactState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    // Edit Contact Functions
    fun loadContactForEdit(contactId: String) {
        val contact = _uiState.value.contacts.find { it.id == contactId }
        if (contact != null) {
            _editContactState.update {
                EditContactUiState(
                    contactId = contact.id,
                    name = contact.name,
                    phone = contact.phoneNumber,
                    relationship = contact.relationship.displayName,
                    email = "", // Email is not in model anymore or handled differently
                    isPrimary = contact.isPrimary,
                    notifyOnSOS = contact.notifyViaSms,
                    shareLocation = false, // Not in model
                    originalContact = contact,
                    isValid = true
                )
            }
        }
    }

    fun onEditNameChanged(name: String) {
        _editContactState.update {
            it.copy(
                name = name,
                nameError = validateName(name),
                isValid = validateEditContactForm(name, it.phone, Relationship.fromValue(it.relationship.lowercase())),
                hasChanges = checkForChanges(it.copy(name = name))
            )
        }
    }

    fun onEditPhoneChanged(phone: String) {
        _editContactState.update {
            it.copy(
                phone = phone,
                phoneError = validatePhone(phone),
                isValid = validateEditContactForm(it.name, phone, Relationship.fromValue(it.relationship.lowercase())),
                hasChanges = checkForChanges(it.copy(phone = phone))
            )
        }
    }

    fun onEditRelationshipChanged(relationship: String) {
        val rel = Relationship.fromValue(relationship.lowercase())
        _editContactState.update {
            val newState = it.copy(
                relationship = rel.displayName,
                relationshipError = validateRelationship(rel),
                isValid = validateEditContactForm(it.name, it.phone, rel)
            )
            newState.copy(hasChanges = checkForChanges(newState))
        }
    }

    fun onEditEmailChanged(email: String) {
        _editContactState.update {
            it.copy(
                email = email,
                emailError = if (email.isNotEmpty()) validateEmail(email) else null,
                hasChanges = checkForChanges(it.copy(email = email))
            )
        }
    }

    fun onEditPrimaryChanged(isPrimary: Boolean) {
        _editContactState.update {
            it.copy(
                isPrimary = isPrimary,
                hasChanges = checkForChanges(it.copy(isPrimary = isPrimary))
            )
        }
    }

    fun onEditNotifyOnSOSChanged(notify: Boolean) {
        _editContactState.update {
            it.copy(
                notifyOnSOS = notify,
                hasChanges = checkForChanges(it.copy(notifyOnSOS = notify))
            )
        }
    }

    fun onEditShareLocationChanged(share: Boolean) {
        _editContactState.update {
            it.copy(
                shareLocation = share,
                hasChanges = checkForChanges(it.copy(shareLocation = share))
            )
        }
    }

    private fun checkForChanges(state: EditContactUiState): Boolean {
        val original = state.originalContact ?: return false
        return state.name != original.name ||
                state.phone != original.phoneNumber ||
                state.relationship != original.relationship.displayName ||
                state.isPrimary != original.isPrimary ||
                state.notifyOnSOS != original.notifyViaSms
    }

    fun saveEditedContact() {
        val state = _editContactState.value

        if (!state.isValid) {
            viewModelScope.launch {
                _events.emit(ContactsEvent.ShowError("Please fill all required fields correctly"))
            }
            return
        }

        if (!state.hasChanges) {
            viewModelScope.launch {
                _events.emit(ContactsEvent.ShowError("No changes to save"))
            }
            return
        }

        viewModelScope.launch {
            _editContactState.update { it.copy(isLoading = true) }

            val rel = Relationship.fromValue(state.relationship.lowercase())
            val contact = state.originalContact?.copy(
                name = state.name.trim(),
                phoneNumber = state.phone.trim(),
                relationship = rel,
                isPrimary = state.isPrimary,
                notifyViaSms = state.notifyOnSOS,
                updatedAt = System.currentTimeMillis()
            ) ?: return@launch

            val result = updateEmergencyContactUseCase(contact)
            when (result) {
                is Resource.Success -> {
                    _editContactState.update { it.copy(isLoading = false) }
                    _events.emit(ContactsEvent.ContactUpdated)
                    _events.emit(ContactsEvent.ShowSuccess("Contact updated successfully"))
                    loadContacts()
                }
                is Resource.Error -> {
                    _editContactState.update { it.copy(isLoading = false) }
                    _events.emit(ContactsEvent.ShowError(result.message))
                }
                else -> {
                    _editContactState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    // Validation Functions
    private fun validateName(name: String): String? {
        return when {
            name.isBlank() -> "Name is required"
            name.length < 2 -> "Name must be at least 2 characters"
            name.length > 50 -> "Name must be less than 50 characters"
            !name.matches(Regex("^[a-zA-Z\\s]+$")) -> "Name can only contain letters"
            else -> null
        }
    }

    private fun validatePhone(phone: String): String? {
        return when {
            phone.isBlank() -> "Phone number is required"
            phone.length < 10 -> "Phone number must be at least 10 digits"
            phone.length > 15 -> "Phone number is too long"
            !phone.matches(Regex("^[+]?[0-9]{10,15}$")) -> "Invalid phone number format"
            else -> null
        }
    }

    private fun validateRelationship(relationship: Relationship): String? {
        return null // Enum is always valid if selected
    }

    private fun validateEmail(email: String): String? {
        val emailPattern = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        return when {
            !email.matches(emailPattern) -> "Invalid email format"
            else -> null
        }
    }

    private fun validateAddContactForm(name: String, phone: String, relationship: Relationship): Boolean {
        return validateName(name) == null &&
                validatePhone(phone) == null
    }

    private fun validateEditContactForm(name: String, phone: String, relationship: Relationship): Boolean {
        return validateName(name) == null &&
                validatePhone(phone) == null
    }
}
