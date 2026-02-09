package com.safeguard.sos.presentation.profile

import androidx.lifecycle.viewModelScope
import com.safeguard.sos.core.base.BaseViewModel
import com.safeguard.sos.core.common.Resource
import com.safeguard.sos.data.local.datastore.UserPreferences
import com.safeguard.sos.domain.model.MedicalInfo
import com.safeguard.sos.domain.model.User
import com.safeguard.sos.domain.repository.AuthRepository
import com.safeguard.sos.domain.repository.SOSRepository
import com.safeguard.sos.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val sosRepository: SOSRepository,
    private val userPreferences: UserPreferences
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _editProfileState = MutableStateFlow(EditProfileUiState())
    val editProfileState: StateFlow<EditProfileUiState> = _editProfileState.asStateFlow()

    private val _medicalInfoState = MutableStateFlow(MedicalInfoUiState())
    val medicalInfoState: StateFlow<MedicalInfoUiState> = _medicalInfoState.asStateFlow()

    private val _events = MutableSharedFlow<ProfileEvent>()
    val events: SharedFlow<ProfileEvent> = _events.asSharedFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            userRepository.getCurrentUser().collect { user ->
                if (user != null) {
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            user = user,
                            verificationStatus = user.verificationStatus,
                            memberSince = formatDate(user.createdAt)
                        )
                    }
                    loadStats()
                } else {
                    _uiState.update {
                        it.copy(isLoading = false, error = "Failed to load profile")
                    }
                }
            }
        }
    }

    private fun loadStats() {
        // Stats methods are placeholders as they might not exist in repository yet
        viewModelScope.launch {
            try {
                val contactCount = userPreferences.emergencyContactCountFlow.first()
                _uiState.update { it.copy(emergencyContactCount = contactCount) }
            } catch (e: Exception) {
            }
        }
    }

    private fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun onEditProfileClick() {
        loadUserForEdit()
        viewModelScope.launch {
            _events.emit(ProfileEvent.NavigateToEditProfile)
        }
    }

    fun onMedicalInfoClick() {
        loadMedicalInfo()
        viewModelScope.launch {
            _events.emit(ProfileEvent.NavigateToMedicalInfo)
        }
    }

    fun onVerificationClick() {
        viewModelScope.launch {
            _events.emit(ProfileEvent.NavigateToVerification)
        }
    }

    // Edit Profile Functions
    private fun loadUserForEdit() {
        val user = _uiState.value.user ?: return
        _editProfileState.update {
            EditProfileUiState(
                name = user.fullName,
                email = user.email ?: "",
                phone = user.phoneNumber,
                address = user.address?.toString() ?: "", // address is model
                city = user.city ?: "",
                state = user.state ?: "",
                pincode = user.pincode ?: "",
                bloodGroup = user.bloodGroup ?: "",
                dateOfBirth = user.dateOfBirth ?: "",
                gender = user.gender?.name ?: "",
                profileImageUri = user.profileImageUrl,
                originalUser = user,
                isValid = true
            )
        }
    }

    fun onEditNameChanged(name: String) {
        _editProfileState.update { state ->
            state.copy(
                name = name,
                nameError = validateName(name),
                hasChanges = checkEditProfileChanges(state.copy(name = name)),
                isValid = validateEditProfileForm(name, state.phone, state.email)
            )
        }
    }

    fun onEditEmailChanged(email: String) {
        _editProfileState.update { state ->
            state.copy(
                email = email,
                emailError = if (email.isNotEmpty()) validateEmail(email) else null,
                hasChanges = checkEditProfileChanges(state.copy(email = email)),
                isValid = validateEditProfileForm(state.name, state.phone, email)
            )
        }
    }

    fun onEditPhoneChanged(phone: String) {
        _editProfileState.update { state ->
            state.copy(
                phone = phone,
                phoneError = validatePhone(phone),
                hasChanges = checkEditProfileChanges(state.copy(phone = phone)),
                isValid = validateEditProfileForm(state.name, phone, state.email)
            )
        }
    }

    fun onEditAddressChanged(address: String) {
        _editProfileState.update { state ->
            state.copy(
                address = address,
                hasChanges = checkEditProfileChanges(state.copy(address = address))
            )
        }
    }

    fun onEditCityChanged(city: String) {
        _editProfileState.update { state ->
            state.copy(
                city = city,
                hasChanges = checkEditProfileChanges(state.copy(city = city))
            )
        }
    }

    fun onEditStateChanged(stateValue: String) {
        _editProfileState.update { state ->
            state.copy(
                state = stateValue,
                hasChanges = checkEditProfileChanges(state.copy(state = stateValue))
            )
        }
    }

    fun onEditPincodeChanged(pincode: String) {
        _editProfileState.update { state ->
            state.copy(
                pincode = pincode,
                hasChanges = checkEditProfileChanges(state.copy(pincode = pincode))
            )
        }
    }

    fun onEditBloodGroupChanged(bloodGroup: String) {
        _editProfileState.update { state ->
            state.copy(
                bloodGroup = bloodGroup,
                hasChanges = checkEditProfileChanges(state.copy(bloodGroup = bloodGroup))
            )
        }
    }

    fun onEditDateOfBirthChanged(dateOfBirth: String) {
        _editProfileState.update { state ->
            state.copy(
                dateOfBirth = dateOfBirth,
                hasChanges = checkEditProfileChanges(state.copy(dateOfBirth = dateOfBirth))
            )
        }
    }

    fun onEditGenderChanged(gender: String) {
        _editProfileState.update { state ->
            state.copy(
                gender = gender,
                hasChanges = checkEditProfileChanges(state.copy(gender = gender))
            )
        }
    }

    fun onProfileImageSelected(uri: String) {
        _editProfileState.update { state ->
            state.copy(
                profileImageUri = uri,
                hasChanges = true
            )
        }
    }

    private fun checkEditProfileChanges(state: EditProfileUiState): Boolean {
        val original = state.originalUser ?: return false
        return state.name != original.fullName ||
                state.email != (original.email ?: "") ||
                state.phone != original.phoneNumber ||
                state.address != (original.address?.toString() ?: "") ||
                state.city != (original.city ?: "") ||
                state.state != (original.state ?: "") ||
                state.pincode != (original.pincode ?: "") ||
                state.bloodGroup != (original.bloodGroup ?: "") ||
                state.dateOfBirth != (original.dateOfBirth ?: "") ||
                state.gender != (original.gender?.name ?: "") ||
                state.profileImageUri != original.profileImageUrl
    }

    fun saveProfile() {
        val state = _editProfileState.value

        if (!state.isValid) {
            viewModelScope.launch {
                _events.emit(ProfileEvent.ShowError("Please fill all required fields correctly"))
            }
            return
        }

        if (!state.hasChanges) {
            viewModelScope.launch {
                _events.emit(ProfileEvent.ShowError("No changes to save"))
            }
            return
        }

        viewModelScope.launch {
            _editProfileState.update { it.copy(isSaving = true) }

            val updatedUser = state.originalUser?.copy(
                fullName = state.name.trim(),
                email = state.email.trim().takeIf { it.isNotEmpty() },
                phoneNumber = state.phone.trim(),
                // address handled separately in repo usually or as model here
                city = state.city.trim().takeIf { it.isNotEmpty() },
                state = state.state.trim().takeIf { it.isNotEmpty() },
                pincode = state.pincode.trim().takeIf { it.isNotEmpty() },
                bloodGroup = state.bloodGroup.takeIf { it.isNotEmpty() },
                dateOfBirth = state.dateOfBirth.takeIf { it.isNotEmpty() },
                // gender = state.gender.takeIf { it.isNotEmpty() }, // Adjust based on Gender enum
                profileImageUrl = state.profileImageUri,
                updatedAt = System.currentTimeMillis()
            )

            if (updatedUser == null) {
                _editProfileState.update { it.copy(isSaving = false) }
                _events.emit(ProfileEvent.ShowError("Failed to update profile"))
                return@launch
            }

            val result = userRepository.updateUser(updatedUser)
            when (result) {
                is Resource.Success -> {
                    _editProfileState.update { it.copy(isSaving = false) }
                    _events.emit(ProfileEvent.ProfileUpdated)
                    _events.emit(ProfileEvent.ShowSuccess("Profile updated successfully"))
                    loadProfile()
                }
                is Resource.Error -> {
                    _editProfileState.update { it.copy(isSaving = false) }
                    _events.emit(ProfileEvent.ShowError(result.message ?: "Failed to update profile"))
                }
                else -> {
                    _editProfileState.update { it.copy(isSaving = false) }
                }
            }
        }
    }

    // Medical Info Functions
    private fun loadMedicalInfo() {
        viewModelScope.launch {
            _medicalInfoState.update { it.copy(isLoading = true) }

            val user = _uiState.value.user
            val medicalInfo = user?.medicalInfo

            _medicalInfoState.update {
                MedicalInfoUiState(
                    isLoading = false,
                    bloodGroup = user?.bloodGroup ?: "",
                    allergies = medicalInfo?.allergies ?: emptyList(),
                    medications = medicalInfo?.medications ?: emptyList(),
                    medicalConditions = medicalInfo?.conditions ?: emptyList(),
                    emergencyNotes = medicalInfo?.emergencyNotes ?: "",
                    organDonor = medicalInfo?.organDonor ?: false,
                    insuranceProvider = medicalInfo?.insuranceProvider ?: "",
                    insurancePolicyNumber = medicalInfo?.insurancePolicyNumber ?: "",
                    primaryPhysician = medicalInfo?.primaryPhysician ?: "",
                    physicianPhone = medicalInfo?.physicianPhone ?: ""
                )
            }
        }
    }

    fun onBloodGroupChanged(bloodGroup: String) {
        _medicalInfoState.update { it.copy(bloodGroup = bloodGroup, hasChanges = true) }
    }

    fun addAllergy(allergy: String) {
        if (allergy.isNotBlank()) {
            _medicalInfoState.update { state ->
                state.copy(
                    allergies = state.allergies + allergy.trim(),
                    hasChanges = true
                )
            }
        }
    }

    fun removeAllergy(allergy: String) {
        _medicalInfoState.update { state ->
            state.copy(
                allergies = state.allergies - allergy,
                hasChanges = true
            )
        }
    }

    fun addMedication(medication: String) {
        if (medication.isNotBlank()) {
            _medicalInfoState.update { state ->
                state.copy(
                    medications = state.medications + medication.trim(),
                    hasChanges = true
                )
            }
        }
    }

    fun removeMedication(medication: String) {
        _medicalInfoState.update { state ->
            state.copy(
                medications = state.medications - medication,
                hasChanges = true
            )
        }
    }

    fun addCondition(condition: String) {
        if (condition.isNotBlank()) {
            _medicalInfoState.update { state ->
                state.copy(
                    medicalConditions = state.medicalConditions + condition.trim(),
                    hasChanges = true
                )
            }
        }
    }

    fun removeCondition(condition: String) {
        _medicalInfoState.update { state ->
            state.copy(
                medicalConditions = state.medicalConditions - condition,
                hasChanges = true
            )
        }
    }

    fun onEmergencyNotesChanged(notes: String) {
        _medicalInfoState.update { it.copy(emergencyNotes = notes, hasChanges = true) }
    }

    fun onOrganDonorChanged(isDonor: Boolean) {
        _medicalInfoState.update { it.copy(organDonor = isDonor, hasChanges = true) }
    }

    fun onInsuranceProviderChanged(provider: String) {
        _medicalInfoState.update { it.copy(insuranceProvider = provider, hasChanges = true) }
    }

    fun onInsurancePolicyNumberChanged(policyNumber: String) {
        _medicalInfoState.update { it.copy(insurancePolicyNumber = policyNumber, hasChanges = true) }
    }

    fun onPrimaryPhysicianChanged(physician: String) {
        _medicalInfoState.update { it.copy(primaryPhysician = physician, hasChanges = true) }
    }

    fun onPhysicianPhoneChanged(phone: String) {
        _medicalInfoState.update { it.copy(physicianPhone = phone, hasChanges = true) }
    }

    fun saveMedicalInfo() {
        val state = _medicalInfoState.value

        if (!state.hasChanges) {
            viewModelScope.launch {
                _events.emit(ProfileEvent.ShowError("No changes to save"))
            }
            return
        }

        viewModelScope.launch {
            _medicalInfoState.update { it.copy(isSaving = true) }

            val user = _uiState.value.user
            if (user == null) {
                _medicalInfoState.update { it.copy(isSaving = false) }
                _events.emit(ProfileEvent.ShowError("User not found"))
                return@launch
            }

            val medicalInfo = MedicalInfo(
                allergies = state.allergies,
                medications = state.medications,
                conditions = state.medicalConditions,
                emergencyNotes = state.emergencyNotes,
                organDonor = state.organDonor,
                insuranceProvider = state.insuranceProvider,
                insurancePolicyNumber = state.insurancePolicyNumber,
                primaryPhysician = state.primaryPhysician,
                physicianPhone = state.physicianPhone
            )

            val updatedUser = user.copy(
                bloodGroup = state.bloodGroup.takeIf { it.isNotEmpty() },
                medicalInfo = medicalInfo,
                updatedAt = System.currentTimeMillis()
            )

            val result = userRepository.updateUser(updatedUser)
            when (result) {
                is Resource.Success -> {
                    _medicalInfoState.update { it.copy(isSaving = false, hasChanges = false) }
                    _events.emit(ProfileEvent.ShowSuccess("Medical information saved"))
                    loadProfile()
                }
                is Resource.Error -> {
                    _medicalInfoState.update { it.copy(isSaving = false) }
                    _events.emit(ProfileEvent.ShowError(result.message ?: "Failed to save medical info"))
                }
                else -> {
                    _medicalInfoState.update { it.copy(isSaving = false) }
                }
            }
        }
    }

    // Logout Function
    fun logout() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val result = authRepository.logout()
            when (result) {
                is Resource.Success -> {
                    userPreferences.clearUserData()
                    _events.emit(ProfileEvent.LoggedOut)
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _events.emit(ProfileEvent.ShowError(result.message ?: "Failed to logout"))
                }
                else -> {
                    _uiState.update { it.copy(isLoading = false) }
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
            else -> null
        }
    }

    private fun validatePhone(phone: String): String? {
        return when {
            phone.isBlank() -> "Phone number is required"
            phone.length < 10 -> "Phone number must be at least 10 digits"
            !phone.matches(Regex("^[+]?[0-9]{10,15}$")) -> "Invalid phone number format"
            else -> null
        }
    }

    private fun validateEmail(email: String): String? {
        val emailPattern = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        return when {
            !email.matches(emailPattern) -> "Invalid email format"
            else -> null
        }
    }

    private fun validateEditProfileForm(name: String, phone: String, email: String): Boolean {
        return validateName(name) == null &&
                validatePhone(phone) == null &&
                (email.isEmpty() || validateEmail(email) == null)
    }
}
