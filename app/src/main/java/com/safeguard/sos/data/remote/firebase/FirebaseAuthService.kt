// app/src/main/java/com/safeguard/sos/data/remote/firebase/FirebaseAuthService.kt

package com.safeguard.sos.data.remote.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.safeguard.sos.core.common.Resource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAuthService @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) {

    val currentUser: FirebaseUser?
        get() = firebaseAuth.currentUser

    val isLoggedIn: Boolean
        get() = currentUser != null

    val currentUserId: String?
        get() = currentUser?.uid

    fun authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser)
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    suspend fun signInWithEmailAndPassword(
        email: String,
        password: String
    ): Resource<FirebaseUser> {
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            result.user?.let {
                Resource.Success(it)
            } ?: Resource.Error("Sign in failed")
        } catch (e: Exception) {
            Timber.e(e, "Sign in with email failed")
            Resource.Error(e.message ?: "Sign in failed", exception = e)
        }
    }

    suspend fun createUserWithEmailAndPassword(
        email: String,
        password: String
    ): Resource<FirebaseUser> {
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            result.user?.let {
                Resource.Success(it)
            } ?: Resource.Error("Registration failed")
        } catch (e: Exception) {
            Timber.e(e, "Create user with email failed")
            Resource.Error(e.message ?: "Registration failed", exception = e)
        }
    }

    suspend fun signInWithGoogle(idToken: String): Resource<FirebaseUser> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = firebaseAuth.signInWithCredential(credential).await()
            result.user?.let {
                Resource.Success(it)
            } ?: Resource.Error("Google sign in failed")
        } catch (e: Exception) {
            Timber.e(e, "Sign in with Google failed")
            Resource.Error(e.message ?: "Google sign in failed", exception = e)
        }
    }

    suspend fun signInWithPhoneCredential(credential: PhoneAuthCredential): Resource<FirebaseUser> {
        return try {
            val result = firebaseAuth.signInWithCredential(credential).await()
            result.user?.let {
                Resource.Success(it)
            } ?: Resource.Error("Phone sign in failed")
        } catch (e: Exception) {
            Timber.e(e, "Sign in with phone failed")
            Resource.Error(e.message ?: "Phone sign in failed", exception = e)
        }
    }

    fun sendPhoneVerificationCode(
        phoneNumber: String,
        activity: android.app.Activity,
        callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    ) {
        val options = PhoneAuthOptions.newBuilder(firebaseAuth)
            .setPhoneNumber("+91$phoneNumber")
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun verifyPhoneWithCode(
        verificationId: String,
        code: String
    ): PhoneAuthCredential {
        return PhoneAuthProvider.getCredential(verificationId, code)
    }

    suspend fun sendPasswordResetEmail(email: String): Resource<Boolean> {
        return try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            Resource.Success(true)
        } catch (e: Exception) {
            Timber.e(e, "Send password reset email failed")
            Resource.Error(e.message ?: "Failed to send reset email", exception = e)
        }
    }

    suspend fun updatePassword(newPassword: String): Resource<Boolean> {
        return try {
            currentUser?.updatePassword(newPassword)?.await()
            Resource.Success(true)
        } catch (e: Exception) {
            Timber.e(e, "Update password failed")
            Resource.Error(e.message ?: "Failed to update password", exception = e)
        }
    }

    suspend fun updateEmail(newEmail: String): Resource<Boolean> {
        return try {
            currentUser?.updateEmail(newEmail)?.await()
            Resource.Success(true)
        } catch (e: Exception) {
            Timber.e(e, "Update email failed")
            Resource.Error(e.message ?: "Failed to update email", exception = e)
        }
    }

    suspend fun deleteAccount(): Resource<Boolean> {
        return try {
            currentUser?.delete()?.await()
            Resource.Success(true)
        } catch (e: Exception) {
            Timber.e(e, "Delete account failed")
            Resource.Error(e.message ?: "Failed to delete account", exception = e)
        }
    }

    fun signOut() {
        firebaseAuth.signOut()
    }

    suspend fun getIdToken(forceRefresh: Boolean = false): String? {
        return try {
            currentUser?.getIdToken(forceRefresh)?.await()?.token
        } catch (e: Exception) {
            Timber.e(e, "Get ID token failed")
            null
        }
    }
}