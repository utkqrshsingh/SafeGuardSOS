// app/src/main/java/com/safeguard/sos/data/remote/firebase/FirebaseMessagingServiceImpl.kt

package com.safeguard.sos.data.remote.firebase

import com.google.firebase.messaging.FirebaseMessaging
import com.safeguard.sos.core.common.Resource
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseMessagingServiceImpl @Inject constructor(
    private val firebaseMessaging: FirebaseMessaging
) {

    suspend fun getToken(): Resource<String> {
        return try {
            val token = firebaseMessaging.token.await()
            Resource.Success(token)
        } catch (e: Exception) {
            Timber.e(e, "Get FCM token failed")
            Resource.Error(e.message ?: "Failed to get FCM token", exception = e)
        }
    }

    suspend fun subscribeToTopic(topic: String): Resource<Boolean> {
        return try {
            firebaseMessaging.subscribeToTopic(topic).await()
            Timber.d("Subscribed to topic: $topic")
            Resource.Success(true)
        } catch (e: Exception) {
            Timber.e(e, "Subscribe to topic failed")
            Resource.Error(e.message ?: "Failed to subscribe to topic", exception = e)
        }
    }

    suspend fun unsubscribeFromTopic(topic: String): Resource<Boolean> {
        return try {
            firebaseMessaging.unsubscribeFromTopic(topic).await()
            Timber.d("Unsubscribed from topic: $topic")
            Resource.Success(true)
        } catch (e: Exception) {
            Timber.e(e, "Unsubscribe from topic failed")
            Resource.Error(e.message ?: "Failed to unsubscribe from topic", exception = e)
        }
    }

    fun deleteToken() {
        firebaseMessaging.deleteToken()
    }
}