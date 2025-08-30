package com.example.meetsphere.data.repository

import com.example.meetsphere.domain.model.User
import com.example.meetsphere.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl
    @Inject
    constructor(
        private val firebaseAuth: FirebaseAuth,
        private val firestore: FirebaseFirestore,
    ) : AuthRepository {
        private val _currentUser = MutableStateFlow<User?>(null)
        override val currentUser: StateFlow<User?>
            get() = _currentUser.asStateFlow()

        init {
            firebaseAuth.addAuthStateListener { auth ->
                _currentUser.value = auth.currentUser?.toDomainUser()
            }
        }

        override suspend fun signInWithEmailAndPassword(
            email: String,
            password: String,
        ): Result<User> =
            try {
                val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
                val firebaseUser = result.user
                firebaseUser?.let {
                    Result.success(
                        firebaseUser.toDomainUser(),
                    )
                } ?: Result.failure(Exception("Sign-in failed: User is null"))
            } catch (e: Exception) {
                Result.failure(e)
            }

        override suspend fun signUpWithEmailAndPassword(
            email: String,
            password: String,
            username: String,
        ): Result<User> =
            try {
                val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
                val firebaseUser = result.user

                firebaseUser?.let { user ->
                    val profileUpdates = UserProfileChangeRequest.Builder().setDisplayName(username).build()
                    firebaseUser.updateProfile(profileUpdates).await()

                    val userDocument = firestore.collection("users").document(firebaseUser.uid)
                    // TODO: Заменить на реальный класс?
                    val userData =
                        mapOf(
                            "uid" to firebaseUser.uid,
                            "email" to email,
                            "password" to password,
                            "username" to username,
                        )
                    userDocument.set(userData).await()
                    Result.success(firebaseUser.toDomainUser())
                } ?: Result.failure(Exception("Sign-up failed: User is null"))
            } catch (e: Exception) {
                Result.failure(e)
            }

        override fun signOut() {
            firebaseAuth.signOut()
        }

        override fun getCurrentUser(): User? = firebaseAuth.currentUser?.toDomainUser()

        private fun FirebaseUser.toDomainUser(): User =
            User(
                uid = this.uid.toString(),
                username = this.displayName.toString(),
            )
    }
