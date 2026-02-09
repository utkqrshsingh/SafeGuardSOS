// app/src/main/java/com/safeguard/sos/domain/repository/UserRepository.kt

package com.safeguard.sos.domain.repository

import com.safeguard.sos.core.common.Resource
import com.safeguard.sos.domain.model.Address
import com.safeguard.sos.domain.model.MedicalInfo
import com.safeguard.sos.domain.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {

    fun getCurrentUser(): Flow<User?>

    suspend fun getCurrentUserSync(): User?

    suspend fun getUserById(userId: String): Resource<User>

    suspend fun updateUser(user: User): Resource<User>

    suspend fun updateProfile(
        fullName: String?,
        email: String?,
        dateOfBirth: String?,
        gender: String?,
        bloodGroup: String?
    ): Resource<User>

    suspend fun updateProfileImage(imageUrl: String): Resource<User>

    suspend fun updateAddress(address: Address): Resource<User>

    suspend fun updateMedicalInfo(medicalInfo: MedicalInfo): Resource<User>

    suspend fun updateFcmToken(token: String): Resource<Boolean>

    suspend fun deleteAccount(): Resource<Boolean>

    suspend fun syncUserData(): Resource<User>
}