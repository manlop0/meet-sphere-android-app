package com.example.meetsphere.ui.activities

import com.example.meetsphere.MainDispatcherRule
import com.example.meetsphere.domain.model.MapMarker
import com.example.meetsphere.domain.model.User
import com.example.meetsphere.domain.repository.ActivitiesRepository
import com.example.meetsphere.domain.repository.AuthRepository
import com.example.meetsphere.domain.repository.ChatRepository
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
class ActivitiesViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `filters my activity and others activities correctly`() =
        runTest {
            val curUser = User(uid = "user1", username = "Alice", email = "alice@test.com", profileImageUrl = null)
            val currentUserFlow = MutableStateFlow<User?>(curUser)

            val authRepo = mockk<AuthRepository>(relaxed = true)
            every { authRepo.currentUserFlow } returns currentUserFlow

            val location = GeoPoint(55.0, 37.0)
            val locationRepo =
                mockk<LocationRepository> {
                    every { userLocationFlow() } returns flowOf(location)
                }

            val myMarker =
                MapMarker(
                    id = "a1",
                    position = location,
                    creatorName = "Alice",
                    creatorId = "user1",
                    shortDescription = "My activity",
                )

            val otherMarker1 =
                MapMarker(
                    id = "a2",
                    position = location,
                    creatorName = "Bob",
                    creatorId = "user2",
                    shortDescription = "Bob's activity",
                )

            val otherMarker2 =
                MapMarker(
                    id = "a3",
                    position = location,
                    creatorName = "Charlie",
                    creatorId = "user3",
                    shortDescription = "Charlie's activity",
                )

            val activitiesRepo =
                mockk<ActivitiesRepository> {
                    every { getActivitiesNearby(location, false) } returns
                        flowOf(
                            listOf(myMarker, otherMarker1, otherMarker2),
                        )
                }

            val chatRepo = mockk<ChatRepository>()

            val viewModel = ActivitiesViewModel(authRepo, locationRepo, activitiesRepo, chatRepo)

            testScheduler.advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(false, state.loading)
            assertEquals("a1", state.myActivity?.id)
            assertEquals(2, state.othersActivities.size)
            assertEquals(listOf("a2", "a3"), state.othersActivities.map { it.id })
        }

    @Test
    fun `shows no my activity when user has none`() =
        runTest {
            val curUser = User(uid = "user1", username = "Alice", email = "alice@test.com", profileImageUrl = null)
            val curUserFlow = MutableStateFlow<User?>(curUser)

            val authRepo =
                mockk<AuthRepository> {
                    every { currentUserFlow } returns curUserFlow
                }

            val location = GeoPoint(55.0, 37.0)
            val locationRepo =
                mockk<LocationRepository> {
                    every { userLocationFlow() } returns flowOf(location)
                }

            val otherMarker =
                MapMarker(
                    id = "a2",
                    position = location,
                    creatorName = "Bob",
                    creatorId = "user2",
                    shortDescription = "Bob's activity",
                )

            val activitiesRepo =
                mockk<ActivitiesRepository> {
                    every { getActivitiesNearby(location, false) } returns flowOf(listOf(otherMarker))
                }

            val chatRepo = mockk<ChatRepository>()

            val viewModel = ActivitiesViewModel(authRepo, locationRepo, activitiesRepo, chatRepo)

            testScheduler.advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(null, state.myActivity)
            assertEquals(1, state.othersActivities.size)
        }
}
