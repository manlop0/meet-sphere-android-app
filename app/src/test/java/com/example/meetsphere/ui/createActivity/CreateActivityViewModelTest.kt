package com.example.meetsphere.ui.createActivity

import androidx.lifecycle.SavedStateHandle
import com.example.meetsphere.MainDispatcherRule
import com.example.meetsphere.domain.model.User
import com.example.meetsphere.domain.repository.ActivitiesRepository
import com.example.meetsphere.domain.repository.AuthRepository
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CreateActivityViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun createTestUser() =
        User(
            uid = "user123",
            username = "TestUser",
            email = "test@example.com",
            profileImageUrl = null,
        )

    @Test
    fun `creates activity with coordinates when showOnMap is false`() =
        runTest {
            val savedStateHandle =
                SavedStateHandle(
                    mapOf("latitude" to "55.75", "longitude" to "37.61"),
                )

            val testUser = createTestUser()
            val authRepo = mockk<AuthRepository>()
            every {
                authRepo.getCurrentUser()
            } returns testUser

            val activitiesRepo = mockk<ActivitiesRepository>()
            coEvery {
                activitiesRepo.createActivity(
                    userId = testUser.uid,
                    userName = testUser.username,
                    description = "Test activity",
                    location = any(),
                    radius = 1000.0,
                    showOnMap = false,
                )
            } returns Result.success(Unit)

            val viewModel = CreateActivityViewModel(activitiesRepo, authRepo, savedStateHandle)
            viewModel.onDescriptionChange("Test activity")
            viewModel.onShowLocationToggle(false)
            viewModel.onCreateActivity()

            testScheduler.advanceUntilIdle()

            coVerify {
                activitiesRepo.createActivity(
                    userId = testUser.uid,
                    userName = testUser.username,
                    description = "Test activity",
                    location =
                        match {
                            it != null &&
                                it.latitude == 55.75 &&
                                it.longitude == 37.61
                        },
                    radius = 1000.0,
                    showOnMap = false,
                )
            }
        }

    @Test
    fun `sets createSuccess to true after successful creation`() =
        runTest {
            val savedStateHandle =
                SavedStateHandle(
                    mapOf("latitude" to "55.0", "longitude" to "37.0"),
                )

            val testUser = createTestUser()
            val authRepo = mockk<AuthRepository>()
            // ← ДОБАВЬТЕ hint<User?>()
            every {
                hint(User::class)
                authRepo.getCurrentUser()
            } returns testUser

            val activitiesRepo = mockk<ActivitiesRepository>()
            coEvery {
                activitiesRepo.createActivity(
                    userId = any(),
                    userName = any(),
                    description = any(),
                    location = any(),
                    radius = any(),
                    showOnMap = any(),
                )
            } returns Result.success(Unit)

            val viewModel = CreateActivityViewModel(activitiesRepo, authRepo, savedStateHandle)
            viewModel.onDescriptionChange("New activity")
            viewModel.onCreateActivity()

            var state = viewModel.uiState.value
            assertTrue("Activity should be creating", state.isCreating)
            assertFalse("Activity should not be created yet", state.createSuccess)

            testScheduler.advanceTimeBy(1500)
            testScheduler.advanceUntilIdle()

            state = viewModel.uiState.value
            assertFalse("Activity should not be creating anymore", state.isCreating)
            assertTrue("Activity should be created successfully", state.createSuccess)
        }

    @Test
    fun `updates description when onDescriptionChange is called`() =
        runTest {
            val savedStateHandle =
                SavedStateHandle(
                    mapOf("latitude" to "55.0", "longitude" to "37.0"),
                )

            val testUser = createTestUser()
            val authRepo = mockk<AuthRepository>()
            every {
                hint(User::class)
                authRepo.getCurrentUser()
            } returns testUser

            val activitiesRepo = mockk<ActivitiesRepository>()

            val viewModel = CreateActivityViewModel(activitiesRepo, authRepo, savedStateHandle)
            viewModel.onDescriptionChange("New description")

            assertEquals("New description", viewModel.uiState.value.description)
        }

    @Test
    fun `updates showLocation when onShowLocationToggle is called`() =
        runTest {
            val savedStateHandle =
                SavedStateHandle(
                    mapOf("latitude" to "55.0", "longitude" to "37.0"),
                )

            val testUser = createTestUser()
            val authRepo = mockk<AuthRepository>()
            every {
                hint(User::class)
                authRepo.getCurrentUser()
            } returns testUser

            val activitiesRepo = mockk<ActivitiesRepository>()

            val viewModel = CreateActivityViewModel(activitiesRepo, authRepo, savedStateHandle)

            assertTrue("showLocation should be true by default", viewModel.uiState.value.showLocation)

            viewModel.onShowLocationToggle(false)
            assertFalse("showLocation should be false", viewModel.uiState.value.showLocation)

            viewModel.onShowLocationToggle(true)
            assertTrue("showLocation should be true again", viewModel.uiState.value.showLocation)
        }

    @Test
    fun `updates radius when onRadiusChange is called`() =
        runTest {
            val savedStateHandle =
                SavedStateHandle(
                    mapOf("latitude" to "55.0", "longitude" to "37.0"),
                )

            val testUser = createTestUser()
            val authRepo = mockk<AuthRepository>()
            every {
                hint(User::class)
                authRepo.getCurrentUser()
            } returns testUser

            val activitiesRepo = mockk<ActivitiesRepository>()

            val viewModel = CreateActivityViewModel(activitiesRepo, authRepo, savedStateHandle)
            viewModel.onRadiusChange(500f)

            assertEquals(500f, viewModel.uiState.value.radius, 0.01f)
        }
}
