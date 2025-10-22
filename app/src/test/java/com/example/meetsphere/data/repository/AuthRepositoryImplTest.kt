package com.example.meetsphere.data.repository

import com.example.meetsphere.domain.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthRepositoryImplTest {
    @Test
    fun `getCurrentUser returns mapped user when logged in`() {
        val mockUser =
            mockk<FirebaseUser> {
                every { uid } returns "user123"
                every { displayName } returns "TestUser"
                every { email } returns "test@example.com"
            }
        val auth =
            mockk<FirebaseAuth> {
                every { currentUser } returns mockUser
                every { addAuthStateListener(any()) } answers {}
            }
        val firestore = mockk<FirebaseFirestore>()
        val userRepo = mockk<UserRepository>()

        val repo = AuthRepositoryImpl(auth, firestore, userRepo)

        val user = repo.getCurrentUser()

        assertEquals("user123", user?.uid)
        assertEquals("TestUser", user?.username)
        assertEquals("test@example.com", user?.email)
    }

    @Test
    fun `getCurrentUser returns null when not logged in`() {
        val auth =
            mockk<FirebaseAuth> {
                every { currentUser } returns null
                every { addAuthStateListener(any()) } answers {}
            }
        val firestore = mockk<FirebaseFirestore>()
        val userRepo = mockk<UserRepository>()

        val repo = AuthRepositoryImpl(auth, firestore, userRepo)

        val user = repo.getCurrentUser()

        assertNull(user)
    }
}
