package com.safeguard.sos.presentation.contacts

import com.safeguard.sos.domain.model.EmergencyContact

data class ContactsUiState(
    val isLoading: Boolean = false,
    val contacts: List<EmergencyContact> = emptyList(),
    val selectedContact: EmergencyContact? = null,
    val searchQuery: String = "",
    val filteredContacts: List<EmergencyContact> = emptyList(),
    val maxContactsLimit: Int = 5,
    val canAddMore: Boolean = true,
    val error: String? = null,
    val successMessage: String? = null
) {
    val contactCount: Int get() = contacts.size
    val remainingSlots: Int get() = maxContactsLimit - contactCount
}

sealed class ContactsEvent {
    data class ShowError(val message: String) : ContactsEvent()
    data class ShowSuccess(val message: String) : ContactsEvent()
    data class NavigateToEdit(val contactId: String) : ContactsEvent()
    object NavigateToAdd : ContactsEvent()
    object ContactDeleted : ContactsEvent()
    object ContactAdded : ContactsEvent()
    object ContactUpdated : ContactsEvent()
}

data class AddContactUiState(
    val isLoading: Boolean = false,
    val name: String = "",
    val phone: String = "",
    val relationship: String = "",
    val email: String = "",
    val isPrimary: Boolean = false,
    val notifyOnSOS: Boolean = true,
    val shareLocation: Boolean = true,
    val nameError: String? = null,
    val phoneError: String? = null,
    val relationshipError: String? = null,
    val emailError: String? = null,
    val isValid: Boolean = false,
    val availableRelationships: List<String> = listOf(
        "Parent", "Spouse", "Sibling", "Child", "Friend",
        "Colleague", "Neighbor", "Doctor", "Other"
    )
)

data class EditContactUiState(
    val isLoading: Boolean = false,
    val contactId: String = "",
    val name: String = "",
    val phone: String = "",
    val relationship: String = "",
    val email: String = "",
    val isPrimary: Boolean = false,
    val notifyOnSOS: Boolean = true,
    val shareLocation: Boolean = true,
    val nameError: String? = null,
    val phoneError: String? = null,
    val relationshipError: String? = null,
    val emailError: String? = null,
    val isValid: Boolean = false,
    val hasChanges: Boolean = false,
    val originalContact: EmergencyContact? = null,
    val availableRelationships: List<String> = listOf(
        "Parent", "Spouse", "Sibling", "Child", "Friend",
        "Colleague", "Neighbor", "Doctor", "Other"
    )
)