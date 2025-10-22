package com.example.meetsphere.data.repository

import app.cash.turbine.test
import com.example.meetsphere.MainDispatcherRule
import com.example.meetsphere.data.remote.dto.ActivityDto
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.*
import com.google.firebase.firestore.GeoPoint
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ActivitiesRepositoryImplTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `getActivitiesNearby includes activities within radius`() =
        runTest {
            val firestore = mockk<FirebaseFirestore>()
            val auth = mockk<FirebaseAuth>()
            val user =
                mockk<FirebaseUser> {
                    every { uid } returns "user1"
                }
            every { auth.currentUser } returns user

            val collection = mockk<CollectionReference>()
            every { firestore.collection("activities") } returns collection

            val userLocation = org.osmdroid.util.GeoPoint(55.75, 37.61)

            val closeActivity =
                ActivityDto(
                    id = "a1",
                    creatorId = "user2",
                    creatorName = "Bob",
                    description = "Close activity",
                    location = GeoPoint(55.755, 37.615), // ~500м от пользователя
                    radius = 1000.0,
                    showOnMap = true,
                )

            val farActivity =
                ActivityDto(
                    id = "a2",
                    creatorId = "user3",
                    creatorName = "Charlie",
                    description = "Far activity",
                    location = GeoPoint(55.77, 37.64), // ~2000м от пользователя
                    radius = 1000.0,
                    showOnMap = true,
                )

            val doc1 =
                mockk<DocumentSnapshot> {
                    every { toObject(ActivityDto::class.java) } returns closeActivity
                }
            val doc2 =
                mockk<DocumentSnapshot> {
                    every { toObject(ActivityDto::class.java) } returns farActivity
                }

            val snapshot =
                mockk<QuerySnapshot> {
                    every { documents } returns listOf(doc1, doc2)
                }

            val listenerSlot = slot<EventListener<QuerySnapshot>>()
            every { collection.addSnapshotListener(capture(listenerSlot)) } answers {
                val listener = listenerSlot.captured
                listener.onEvent(snapshot, null)
                mockk<ListenerRegistration>()
            }

            val repo = ActivitiesRepositoryImpl(firestore, auth)

            repo.getActivitiesNearby(userLocation, onMapOnly = false).test {
                val markers = awaitItem()

                assertEquals(1, markers.size)
                assertEquals("a1", markers[0].id)
                assertEquals("Bob", markers[0].creatorName)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `getActivitiesNearby filters by showOnMap when onMapOnly is true`() =
        runTest {
            val firestore = mockk<FirebaseFirestore>()
            val auth = mockk<FirebaseAuth>()
            val user =
                mockk<FirebaseUser> {
                    every { uid } returns "user1"
                }
            every { auth.currentUser } returns user

            val collection = mockk<CollectionReference>()
            val query = mockk<Query>()
            every { firestore.collection("activities") } returns collection
            every { collection.whereEqualTo("showOnMap", true) } returns query

            val userLocation = org.osmdroid.util.GeoPoint(55.75, 37.61)

            val visibleActivity =
                ActivityDto(
                    id = "a1",
                    creatorId = "user2",
                    creatorName = "Bob",
                    description = "Visible activity",
                    location = GeoPoint(55.751, 37.611),
                    radius = 1000.0,
                    showOnMap = true,
                )

            val doc =
                mockk<DocumentSnapshot> {
                    every { toObject(ActivityDto::class.java) } returns visibleActivity
                }

            val snapshot =
                mockk<QuerySnapshot> {
                    every { documents } returns listOf(doc)
                }

            val listenerSlot = slot<EventListener<QuerySnapshot>>()
            every { query.addSnapshotListener(capture(listenerSlot)) } answers {
                val listener = listenerSlot.captured
                listener.onEvent(snapshot, null)
                mockk<ListenerRegistration>()
            }

            val repo = ActivitiesRepositoryImpl(firestore, auth)

            repo.getActivitiesNearby(userLocation, onMapOnly = true).test {
                val markers = awaitItem()
                assertEquals(1, markers.size)
                assertEquals("a1", markers[0].id)
                cancelAndIgnoreRemainingEvents()
            }

            verify { collection.whereEqualTo("showOnMap", true) }
        }

    @Test
    fun `getActivitiesNearby excludes activities outside radius`() =
        runTest {
            val firestore = mockk<FirebaseFirestore>()
            val auth = mockk<FirebaseAuth>()
            val user =
                mockk<FirebaseUser> {
                    every { uid } returns "user1"
                }
            every { auth.currentUser } returns user

            val collection = mockk<CollectionReference>()
            every { firestore.collection("activities") } returns collection

            val userLocation = org.osmdroid.util.GeoPoint(55.75, 37.61)

            val farActivity =
                ActivityDto(
                    id = "a1",
                    creatorId = "user2",
                    creatorName = "Bob",
                    description = "Far with small radius",
                    location = GeoPoint(55.76, 37.63), // ~1500м от пользователя
                    radius = 500.0,
                    showOnMap = true,
                )

            val doc =
                mockk<DocumentSnapshot> {
                    every { toObject(ActivityDto::class.java) } returns farActivity
                }

            val snapshot =
                mockk<QuerySnapshot> {
                    every { documents } returns listOf(doc)
                }

            val listenerSlot = slot<EventListener<QuerySnapshot>>()
            every { collection.addSnapshotListener(capture(listenerSlot)) } answers {
                val listener = listenerSlot.captured
                listener.onEvent(snapshot, null)
                mockk<ListenerRegistration>()
            }

            val repo = ActivitiesRepositoryImpl(firestore, auth)

            repo.getActivitiesNearby(userLocation, onMapOnly = false).test {
                val markers = awaitItem()

                assertTrue(markers.isEmpty())

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `getActivitiesNearby skips activities with null location or radius`() =
        runTest {
            val firestore = mockk<FirebaseFirestore>()
            val auth = mockk<FirebaseAuth>()
            val user =
                mockk<FirebaseUser> {
                    every { uid } returns "user1"
                }
            every { auth.currentUser } returns user

            val collection = mockk<CollectionReference>()
            every { firestore.collection("activities") } returns collection

            val userLocation = org.osmdroid.util.GeoPoint(55.75, 37.61)

            val validActivity =
                ActivityDto(
                    id = "a1",
                    creatorId = "user2",
                    creatorName = "Bob",
                    description = "Valid",
                    location = GeoPoint(55.751, 37.611),
                    radius = 1000.0,
                    showOnMap = true,
                )

            val nullLocationActivity =
                ActivityDto(
                    id = "a2",
                    creatorId = "user3",
                    creatorName = "Charlie",
                    description = "No location",
                    location = null,
                    radius = 1000.0,
                    showOnMap = true,
                )

            val nullRadiusActivity =
                ActivityDto(
                    id = "a3",
                    creatorId = "user4",
                    creatorName = "Dave",
                    description = "No radius",
                    location = GeoPoint(55.751, 37.611),
                    radius = null,
                    showOnMap = true,
                )

            val doc1 =
                mockk<DocumentSnapshot> {
                    every { toObject(ActivityDto::class.java) } returns validActivity
                }
            val doc2 =
                mockk<DocumentSnapshot> {
                    every { toObject(ActivityDto::class.java) } returns nullLocationActivity
                }
            val doc3 =
                mockk<DocumentSnapshot> {
                    every { toObject(ActivityDto::class.java) } returns nullRadiusActivity
                }

            val snapshot =
                mockk<QuerySnapshot> {
                    every { documents } returns listOf(doc1, doc2, doc3)
                }

            val listenerSlot = slot<EventListener<QuerySnapshot>>()
            every { collection.addSnapshotListener(capture(listenerSlot)) } answers {
                val listener = listenerSlot.captured
                listener.onEvent(snapshot, null)
                mockk<ListenerRegistration>()
            }

            val repo = ActivitiesRepositoryImpl(firestore, auth)

            repo.getActivitiesNearby(userLocation, onMapOnly = false).test {
                val markers = awaitItem()

                assertEquals(1, markers.size)
                assertEquals("a1", markers[0].id)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `getActivitiesNearby truncates long descriptions to 30 chars`() =
        runTest {
            val firestore = mockk<FirebaseFirestore>()
            val auth = mockk<FirebaseAuth>()
            val user =
                mockk<FirebaseUser> {
                    every { uid } returns "user1"
                }
            every { auth.currentUser } returns user

            val collection = mockk<CollectionReference>()
            every { firestore.collection("activities") } returns collection

            val userLocation = org.osmdroid.util.GeoPoint(55.75, 37.61)

            val longDescActivity =
                ActivityDto(
                    id = "a1",
                    creatorId = "user2",
                    creatorName = "Bob",
                    description = "This is a very long description that should be truncated",
                    location = GeoPoint(55.751, 37.611),
                    radius = 1000.0,
                    showOnMap = true,
                )

            val doc =
                mockk<DocumentSnapshot> {
                    every { toObject(ActivityDto::class.java) } returns longDescActivity
                }

            val snapshot =
                mockk<QuerySnapshot> {
                    every { documents } returns listOf(doc)
                }

            val listenerSlot = slot<EventListener<QuerySnapshot>>()
            every { collection.addSnapshotListener(capture(listenerSlot)) } answers {
                val listener = listenerSlot.captured
                listener.onEvent(snapshot, null)
                mockk<ListenerRegistration>()
            }

            val repo = ActivitiesRepositoryImpl(firestore, auth)

            repo.getActivitiesNearby(userLocation, onMapOnly = false).test {
                val markers = awaitItem()
                assertEquals(1, markers.size)
                assertEquals("This is a very long descriptio...", markers[0].shortDescription)
                cancelAndIgnoreRemainingEvents()
            }
        }
}
