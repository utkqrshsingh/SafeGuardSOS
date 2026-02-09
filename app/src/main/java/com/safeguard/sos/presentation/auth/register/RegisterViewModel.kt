// app/src/main/java/com/safeguard/sos/presentation/auth/register/RegisterViewModel.kt

package com.safeguard.sos.presentation.auth.register

import androidx.lifecycle.viewModelScope
import com.safeguard.sos.core.base.BaseViewModel
import com.safeguard.sos.core.common.Resource
import com.safeguard.sos.domain.model.User
import com.safeguard.sos.domain.model.UserType
import com.safeguard.sos.domain.repository.AuthRepository
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
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    private val _registerState = MutableStateFlow<Resource<User>>(Resource.Empty)
    val registerState: StateFlow<Resource<User>> = _registerState.asStateFlow()

    private val _navigateToUserType = MutableSharedFlow<Boolean>()
    val navigateToUserType: SharedFlow<Boolean> = _navigateToUserType.asSharedFlow()

    private val _selectedUserType = MutableStateFlow<UserType?>(null)
    val selectedUserType: StateFlow<UserType?> = _selectedUserType.asStateFlow()

    // Temporarily store registration data
    var registrationData: RegistrationData? = null
        private set

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun toggleConfirmPasswordVisibility() {
        _uiState.update { it.copy(isConfirmPasswordVisible = !it.isConfirmPasswordVisible) }
    }

    fun setUserType(userType: UserType) {
        _selectedUserType.value = userType
    }

    fun saveRegistrationData(
        fullName: String,
        phoneNumber: String,
        email: String?,
        password: String
    ) {
        registrationData = RegistrationData(
            fullName = fullName,
            phoneNumber = phoneNumber,
            email = email,
            password = password
        )

        viewModelScope.launch {
            _navigateToUserType.emit(true)
        }
    }

    fun completeRegistration(
        fullName: String?,
        email: String?,
        phone: String?,
        password: String?,
        userType: String?,
        aadhaarNumber: String?
    ) {
        if (fullName.isNullOrBlank() || phone.isNullOrBlank() || password.isNullOrBlank()) {
            _registerState.value = Resource.Error("Required fields are missing")
            return
        }

        viewModelScope.launch {
            _registerState.value = Resource.Loading
            val result = authRepository.register(
                fullName = fullName,
                phoneNumber = phone,
                email = email,
                password = password,
                userType = userType ?: "USER"
            )
            _registerState.value = result
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

data class RegisterUiState(
    val isLoading: Boolean = false,
    val isPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false,
    val error: String? = null
)

data class RegistrationData(
    val fullName: String,
    val phoneNumber: String,
    val email: String?,
    val password: String,
    var userType: String = ""
)
