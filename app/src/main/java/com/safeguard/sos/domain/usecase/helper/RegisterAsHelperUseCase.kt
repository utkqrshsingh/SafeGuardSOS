// app/src/main/java/com/safeguard/sos/domain/usecase/helper/RegisterAsHelperUseCase.kt

package com.safeguard.sos.domain.usecase.helper

import com.safeguard.sos.core.common.Resource
import com.safeguard.sos.domain.model.Helper
import com.safeguard.sos.domain.repository.HelperRepository
import com.safeguard.sos.domain.repository.UserRepository
import javax.inject.Inject

class RegisterAsHelperUseCase @Inject constructor(
    private val helperRepository: HelperRepository,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(
        radiusKm: Int = 10,
        skills: List<String>? = null
    ): Resource<Helper> {
        // Validate radius
        if (radiusKm < 1 || radiusKm > 50) {
            return Resource.Error("Please select a valid radius between 1 and 50 km")
        }

        // Check if user is verified
        val user = userRepository.getCurrentUserSync()
        if (user == null) {
            return Resource.Error("Please login to register as a helper")
        }

        if (!user.isVerified) {
            return Resource.Error("Please verify your Aadhaar to register as a helper")
        }

        return helperRepository.registerAsHelper(radiusKm, skills)
    }
}