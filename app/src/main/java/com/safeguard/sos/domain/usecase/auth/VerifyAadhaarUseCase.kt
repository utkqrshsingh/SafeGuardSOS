package com.safeguard.sos.domain.usecase.auth

import com.safeguard.sos.core.common.Resource
import com.safeguard.sos.core.utils.AadhaarValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class VerifyAadhaarUseCase @Inject constructor() {
    operator fun invoke(aadhaarNumber: String): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading)

        val isValid = AadhaarValidator.isValid(aadhaarNumber)

        if (isValid) {
            emit(Resource.Success(true))
        } else {
            emit(Resource.Error("Invalid Aadhaar number"))
        }
    }
}
