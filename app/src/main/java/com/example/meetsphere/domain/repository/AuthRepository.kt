package com.example.meetsphere.domain.repository

import com.example.meetsphere.domain.model.User
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val currentUserFlow: StateFlow<User?>

    suspend fun signInWithEmailAndPassword(
        email: String,
        password: String,
    ): Result<User>

    suspend fun signUpWithEmailAndPassword(
        email: String,
        password: String,
        username: String,
    ): Result<User>

    fun signOut(): Unit

    fun getCurrentUser(): User?
}
