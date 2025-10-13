package com.example.meetsphere.data.repository

import com.example.meetsphere.data.remote.dto.UserDto
import com.example.meetsphere.data.toUser
import com.example.meetsphere.domain.model.User
import com.example.meetsphere.domain.repository.AuthRepository
import com.example.meetsphere.domain.repository.UserRepository
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
        private val userRepository: UserRepository,
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
                    val currentProfileResult = userRepository.getCurrentUserProfile()
                    if (currentProfileResult.isFailure) {
                        userRepository.createUserProfile(it)
                    }
                    Result.success(
                        it.toDomainUser(),
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

                    val userDto =
                        UserDto(
                            firebaseUser.uid,
                            firebaseUser.displayName.toString(),
                            firebaseUser.email.toString(),
                            null,
                        )
                    userDocument.set(userDto).await()
                    Result.success(userDto.toUser())
                } ?: Result.failure(Exception("Sign-up failed: User is null"))
            } catch (e: Exception) {
                Result.failure(e)
            }

        override fun signOut() {
            firebaseAuth.signOut()
        }

        override fun getCurrentUser(): User? = firebaseAuth.currentUser?.toDomainUser()

        private fun FirebaseUser.toDomainUser(): User {
            val userData =
                UserDto(
                    this.uid,
                    this.displayName.toString(),
                    this.email.toString(),
                    null,
                )
            return userData.toUser()
        }
    }
