// presentation/auth/verification/VerificationViewModel.kt
package com.safeguard.sos.presentation.auth.verification

import androidx.lifecycle.viewModelScope
import com.safeguard.sos.core.base.BaseViewModel
import com.safeguard.sos.core.common.Resource
import com.safeguard.sos.core.common.UiEvent
import com.safeguard.sos.core.utils.AadhaarValidator
import com.safeguard.sos.domain.usecase.auth.SendOtpUseCase
import com.safeguard.sos.domain.usecase.auth.VerifyAadhaarUseCase
import com.safeguard.sos.domain.usecase.auth.VerifyOtpUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VerificationViewModel @Inject constructor(
    private val verifyAadhaarUseCase: VerifyAadhaarUseCase,
    private val sendOtpUseCase: SendOtpUseCase,
    private val verifyOtpUseCase: VerifyOtpUseCase
) : BaseViewModel() {

    private val _verificationState = MutableStateFlow(VerificationUiState())
    val verificationState: StateFlow<VerificationUiState> = _verificationState.asStateFlow()

    private val _otpTimerSeconds = MutableStateFlow(0)
    val otpTimerSeconds: StateFlow<Int> = _otpTimerSeconds.asStateFlow()

    private var timerJob: Job? = null
    private var storedAadhaarNumber: String = ""

    fun initiateAadhaarVerification(aadhaarNumber: String) {
        // Client-side validation first
        if (!AadhaarValidator.isValidFormat(aadhaarNumber)) {
            _verificationState.update {
                it.copy(error = "Invalid Aadhaar number format")
            }
            return
        }

        storedAadhaarNumber = aadhaarNumber

        viewModelScope.launch {
            _verificationState.update { it.copy(isLoading = true, error = null) }

            verifyAadhaarUseCase(aadhaarNumber).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        // Aadhaar exists, now send OTP
                        sendOtp(aadhaarNumber)
                    }
                    is Resource.Error -> {
                        _verificationState.update {
                            it.copy(
                                isLoading = false,
                                error = result.message
                            )
                        }
                    }
                    is Resource.Loading -> {
                        _verificationState.update { it.copy(isLoading = true) }
                    }
                    is Resource.Empty -> {
                        _verificationState.update { it.copy(isLoading = false) }
                    }
                }
            }
        }
    }

    private fun sendOtp(aadhaarNumber: String) {
        viewModelScope.launch {
            sendOtpUseCase(aadhaarNumber).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        _verificationState.update {
                            it.copy(
                                isLoading = false,
                                isOtpSent = true,
                                aadhaarNumber = aadhaarNumber,
                                error = null
                            )
                        }
                        startOtpTimer()
                    }
                    is Resource.Error -> {
                        _verificationState.update {
                            it.copy(
                                isLoading = false,
                                error = result.message
                            )
                        }
                    }
                    is Resource.Loading -> {
                        _verificationState.update { it.copy(isLoading = true) }
                    }
                    is Resource.Empty -> {
                        _verificationState.update { it.copy(isLoading = false) }
                    }
                }
            }
        }
    }

    fun verifyOtp(otp: String) {
        if (otp.length != 6) {
            _verificationState.update {
                it.copy(error = "Please enter a valid 6-digit OTP")
            }
            return
        }

        viewModelScope.launch {
            _verificationState.update {
                it.copy(isLoading = true, error = null, isOtpVerifying = true)
            }

            // verifyOtpUseCase returns a Resource<Boolean>, not a Flow.
            val result = verifyOtpUseCase(storedAadhaarNumber, otp)
            when (result) {
                is Resource.Success -> {
                    _verificationState.update {
                        it.copy(
                            isLoading = false,
                            isOtpVerifying = false,
                            isVerified = true,
                            error = null
                        )
                    }
                }
                is Resource.Error -> {
                    _verificationState.update {
                        it.copy(
                            isLoading = false,
                            isOtpVerifying = false,
                            error = result.message,
                            otpAttempts = it.otpAttempts + 1
                        )
                    }

                    // Lock after 5 failed attempts
                    if (_verificationState.value.otpAttempts >= 5) {
                        _verificationState.update {
                            it.copy(
                                error = "Too many failed attempts. Please try again later.",
                                isLocked = true
                            )
                        }
                    }
                }
                is Resource.Loading -> {
                    _verificationState.update { it.copy(isLoading = true) }
                }
                is Resource.Empty -> {
                    _verificationState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    fun resendOtp() {
        if (_otpTimerSeconds.value > 0) return
        if (_verificationState.value.isLocked) return

        _verificationState.update { it.copy(isLoading = true, error = null) }
        sendOtp(storedAadhaarNumber)
    }

    private fun startOtpTimer() {
        timerJob?.cancel()
        _otpTimerSeconds.value = OTP_TIMER_DURATION

        timerJob = viewModelScope.launch {
            while (_otpTimerSeconds.value > 0) {
                delay(1000)
                _otpTimerSeconds.value -= 1
            }
        }
    }

    fun clearError() {
        _verificationState.update { it.copy(error = null) }
    }

    fun resetOtpState() {
        _verificationState.update {
            it.copy(
                isOtpSent = false,
                isOtpVerifying = false,
                otpAttempts = 0
            )
        }
    }

    fun getStoredAadhaarNumber(): String = storedAadhaarNumber

    fun getMaskedAadhaar(): String {
        return if (storedAadhaarNumber.length == 12) {
            "XXXX XXXX ${storedAadhaarNumber.takeLast(4)}"
        } else {
            ""
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }

    companion object {
        const val OTP_TIMER_DURATION = 60 // seconds
    }
}
