package com.safeguard.sos.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.safeguard.sos.core.common.Resource
import com.safeguard.sos.data.local.dao.UserDao
import com.safeguard.sos.data.local.datastore.UserPreferences
import com.safeguard.sos.data.mapper.UserMapper
import com.safeguard.sos.data.remote.api.UserApi
import com.safeguard.sos.domain.model.Address
import com.safeguard.sos.domain.model.MedicalInfo
import com.safeguard.sos.domain.model.User
import com.safeguard.sos.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val userApi: UserApi,
    private val userDao: UserDao,
    private val firestore: FirebaseFirestore,
    private val userPreferences: UserPreferences,
    private val userMapper: UserMapper
) : UserRepository {

    override fun getCurrentUser(): Flow<User?> {
        return userDao.getCurrentUserFlow().map { entity ->
            entity?.let { userMapper.mapEntityToDomain(it) }
        }
    }

    override suspend fun getCurrentUserSync(): User? {
        return userDao.getCurrentUser()?.let { userMapper.mapEntityToDomain(it) }
    }

    override suspend fun getUserById(userId: String): Resource<User> {
        return try {
            val response = userApi.getUserById(userId)
            if (response.isSuccessful && response.body() != null) {
                val user = userMapper.mapResponseToDomain(response.body()!!)
                userDao.insertUser(userMapper.mapDomainToEntity(user))
                Resource.Success(user)
            } else {
                val localUser = userDao.getUserById(userId)
                if (localUser != null) {
                    Resource.Success(userMapper.mapEntityToDomain(localUser))
                } else {
                    Resource.Error(response.message().ifEmpty { "Failed to fetch user" })
                }
            }
        } catch (e: Exception) {
            val localUser = userDao.getUserById(userId)
            if (localUser != null) {
                Resource.Success(userMapper.mapEntityToDomain(localUser))
            } else {
                Resource.Error(e.message ?: "An error occurred")
            }
        }
    }

    override suspend fun updateUser(user: User): Resource<User> {
        return try {
            val request = mapOf(
                "name" to user.fullName,
                "email" to user.email,
                "city" to user.city,
                "state" to user.state,
                "pincode" to user.pincode,
                "blood_group" to user.bloodGroup,
                "date_of_birth" to user.dateOfBirth,
                "gender" to user.gender?.value,
                "profile_image_url" to user.profileImageUrl
            )
            val response = userApi.updateUser(request)
            if (response.isSuccessful && response.body() != null) {
                val updatedUser = userMapper.mapResponseToDomain(response.body()!!)
                userDao.insertUser(userMapper.mapDomainToEntity(updatedUser))
                
                userPreferences.setUserData(
                    userId = updatedUser.id,
                    name = updatedUser.fullName,
                    phone = updatedUser.phoneNumber,
                    email = updatedUser.email
                )
                
                Resource.Success(updatedUser)
            } else {
                Resource.Error("Failed to update user")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to update user")
        }
    }

    override suspend fun updateProfile(
        fullName: String?,
        email: String?,
        dateOfBirth: String?,
        gender: String?,
        bloodGroup: String?
    ): Resource<User> {
        return try {
            val request = mutableMapOf<String, Any?>()
            fullName?.let { request["name"] = it }
            email?.let { request["email"] = it }
            dateOfBirth?.let { request["date_of_birth"] = it }
            gender?.let { request["gender"] = it }
            bloodGroup?.let { request["blood_group"] = it }

            val response = userApi.updateProfile(request)
            if (response.isSuccessful && response.body() != null) {
                val updatedUser = userMapper.mapResponseToDomain(response.body()!!)
                userDao.insertUser(userMapper.mapDomainToEntity(updatedUser))
                Resource.Success(updatedUser)
            } else {
                Resource.Error("Failed to update profile")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to update profile")
        }
    }

    override suspend fun updateProfileImage(imageUrl: String): Resource<User> {
        return try {
            val request = mapOf("profile_image_url" to imageUrl)
            val response = userApi.updateProfile(request)
            if (response.isSuccessful && response.body() != null) {
                val updatedUser = userMapper.mapResponseToDomain(response.body()!!)
                userDao.insertUser(userMapper.mapDomainToEntity(updatedUser))
                Resource.Success(updatedUser)
            } else {
                Resource.Error("Failed to update profile image")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to update profile image")
        }
    }

    override suspend fun updateAddress(address: Address): Resource<User> {
        return try {
            val request = mapOf(
                "street" to address.street,
                "city" to address.city,
                "state" to address.state,
                "pincode" to address.pincode,
                "country" to address.country
            )
            val response = userApi.updateAddress(request)
            if (response.isSuccessful && response.body() != null) {
                val updatedUser = userMapper.mapResponseToDomain(response.body()!!)
                userDao.insertUser(userMapper.mapDomainToEntity(updatedUser))
                Resource.Success(updatedUser)
            } else {
                Resource.Error("Failed to update address")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to update address")
        }
    }

    override suspend fun updateMedicalInfo(medicalInfo: MedicalInfo): Resource<User> {
        return try {
            val request = mapOf(
                "conditions" to medicalInfo.conditions,
                "allergies" to medicalInfo.allergies,
                "medications" to medicalInfo.medications,
                "emergency_notes" to medicalInfo.emergencyNotes,
                "organ_donor" to medicalInfo.organDonor
            )
            val response = userApi.updateMedicalInfo(request)
            if (response.isSuccessful && response.body() != null) {
                val updatedUser = userMapper.mapResponseToDomain(response.body()!!)
                userDao.insertUser(userMapper.mapDomainToEntity(updatedUser))
                Resource.Success(updatedUser)
            } else {
                Resource.Error("Failed to update medical info")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to update medical info")
        }
    }

    override suspend fun updateFcmToken(token: String): Resource<Boolean> {
        return try {
            val userId = userPreferences.userIdFlow.first() ?: return Resource.Error("User not logged in")
            val response = userApi.updateFcmToken(mapOf("fcm_token" to token))
            if (response.isSuccessful) {
                userDao.updateFcmToken(userId, token)
                firestore.collection("users").document(userId)
                    .update("fcmToken", token)
                    .await()
                Resource.Success(true)
            } else {
                Resource.Error("Failed to update FCM token")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to update FCM token")
        }
    }

    override suspend fun deleteAccount(): Resource<Boolean> {
        return try {
            val response = userApi.deleteAccount()
            if (response.isSuccessful) {
                val userId = userPreferences.userIdFlow.first()
                if (userId != null) {
                    userDao.deleteUserById(userId)
                }
                userPreferences.clearUserData()
                Resource.Success(true)
            } else {
                Resource.Error("Failed to delete account")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to delete account")
        }
    }

    override suspend fun syncUserData(): Resource<User> {
        return try {
            val response = userApi.getCurrentUser()
            if (response.isSuccessful && response.body() != null) {
                val user = userMapper.mapResponseToDomain(response.body()!!)
                userDao.insertUser(userMapper.mapDomainToEntity(user))
                
                userPreferences.setUserData(
                    userId = user.id,
                    name = user.fullName,
                    phone = user.phoneNumber,
                    email = user.email
                )
                userPreferences.setIsHelper(user.isHelper)
                
                Resource.Success(user)
            } else {
                Resource.Error("Failed to sync user data")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to sync user data")
        }
    }
}
