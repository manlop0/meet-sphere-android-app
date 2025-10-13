package com.example.meetsphere.data.repository

import com.example.meetsphere.data.remote.dto.UserDto
import com.example.meetsphere.data.toUser
import com.example.meetsphere.domain.model.User
import com.example.meetsphere.domain.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl
    @Inject
    constructor(
        private val firestore: FirebaseFirestore,
        private val auth: FirebaseAuth,
    ) : UserRepository {
        override suspend fun createUserProfile(user: FirebaseUser): Result<Unit> =
            try {
                val userDto =
                    UserDto(
                        uid = user.uid,
                        username =
                            user.displayName
                                ?: "New User",
                        email = user.email ?: "",
                    )
                firestore
                    .collection("users")
                    .document(user.uid)
                    .set(userDto)
                    .await()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }

        override suspend fun getUserProfile(userId: String): Result<User> =
            try {
                val document =
                    firestore
                        .collection("users")
                        .document(userId)
                        .get()
                        .await()
                val userDto = document.toObject(UserDto::class.java)
                if (userDto != null) {
                    Result.success(userDto.toUser())
                } else {
                    Result.failure(Exception("User profile not found"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }

        override suspend fun getCurrentUserProfile(): Result<User> {
            val currentUserId = auth.currentUser?.uid
            return if (currentUserId != null) {
                getUserProfile(currentUserId)
            } else {
                Result.failure(Exception("No authenticated user"))
            }
        }
    }
