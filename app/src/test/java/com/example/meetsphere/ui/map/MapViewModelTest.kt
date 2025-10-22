package com.example.meetsphere.ui.map

import com.example.meetsphere.MainDispatcherRule
import com.example.meetsphere.domain.model.MapMarker
import com.example.meetsphere.domain.model.User
import com.example.meetsphere.domain.repository.ActivitiesRepository
import com.example.meetsphere.domain.repository.AuthRepository
import com.example.meetsphere.domain.repository.LocationRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.osmdroid.util.GeoPoint

@OptIn(ExperimentalCoroutinesApi::class)
class MapViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `updates currentUserId when user changes`() =
        runTest {
            val userFlow = MutableStateFlow<User?>(User("user1", "Alice", "alice@test.com", null))
            val authRepo =
                mockk<AuthRepository> {
                    every { currentUserFlow } returns userFlow
                }

            val location = GeoPoint(55.0, 37.0)
            val locationRepo =
                mockk<LocationRepository> {
                    every { userLocationFlow() } returns flowOf(location)
                }

            val activitiesRepo =
                mockk<ActivitiesRepository> {
                    every { getActivitiesNearby(location, true) } returns flowOf(emptyList())
                }

            val viewModel = MapViewModel(activitiesRepo, locationRepo, authRepo)
            testScheduler.advanceUntilIdle()

            val state1 = viewModel.uiState.value
            assertEquals("user1", state1.currentUserId)

            userFlow.value = User(uid = "user2", username = "Bob", email = "bob@test.com", profileImageUrl = null)
            testScheduler.advanceUntilIdle()

            val state2 = viewModel.uiState.value
            assertEquals("user2", state2.currentUserId)
        }

    @Test
    fun `loads activities for current user location`() =
        runTest {
            val user = User("user1", "Alice", "alice@test.com", null)
            val authRepo =
                mockk<AuthRepository> {
                    every { currentUserFlow } returns MutableStateFlow(user)
                }

            val location = GeoPoint(55.0, 37.0)
            val locationRepo =
                mockk<LocationRepository> {
                    every { userLocationFlow() } returns flowOf(location)
                }

            val markers =
                listOf(
                    MapMarker(
                        id = "a1",
                        position = location,
                        creatorName = "Alice",
                        creatorId = "user1",
                        shortDescription = "My activity",
                    ),
                    MapMarker(
                        id = "a2",
                        position = location,
                        creatorName = "Bob",
                        creatorId = "user2",
                        shortDescription = "Bob's activity",
                    ),
                )

            val activitiesRepo =
                mockk<ActivitiesRepository> {
                    every { getActivitiesNearby(location, true) } returns flowOf(markers)
                }

            val viewModel = MapViewModel(activitiesRepo, locationRepo, authRepo)

            testScheduler.advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(2, state.activities.size)
            assertEquals(false, state.isLoading)
        }
}
