package com.example.meetsphere.domain.repository

import com.example.meetsphere.domain.model.User
import com.google.firebase.auth.FirebaseUser

interface UserRepository {
    suspend fun createUserProfile(user: FirebaseUser): Result<Unit>

    suspend fun getUserProfile(userId: String): Result<User>

    suspend fun getCurrentUserProfile(): Result<User>
}
