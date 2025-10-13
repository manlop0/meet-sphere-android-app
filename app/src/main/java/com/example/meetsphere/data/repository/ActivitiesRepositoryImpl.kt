package com.example.meetsphere.data.repository

import android.util.Log
import com.example.meetsphere.data.remote.dto.ActivityDto
import com.example.meetsphere.data.toActivity
import com.example.meetsphere.domain.model.Activity
import com.example.meetsphere.domain.model.MapMarker
import com.example.meetsphere.domain.repository.ActivitiesRepository
import com.example.meetsphere.util.Constants.MAX_ACTIVITY_RADIUS_METERS
import com.firebase.geofire.GeoFireUtils
import com.firebase.geofire.GeoLocation
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import org.osmdroid.util.GeoPoint
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActivitiesRepositoryImpl
    @Inject
    constructor(
        private val firestore: FirebaseFirestore,
        private val auth: FirebaseAuth,
    ) : ActivitiesRepository {
        override fun getActivitiesNearby(
            centerPoint: GeoPoint,
            onMapOnly: Boolean,
        ): Flow<List<MapMarker>> =
            callbackFlow {
                val currentUser = auth.currentUser
                checkNotNull(currentUser) { "User must be authenticated to get an activities" }
                val center = GeoLocation(centerPoint.latitude, centerPoint.longitude)

                var baseQuery: Query = firestore.collection("activities")
                if (onMapOnly) {
                    baseQuery = baseQuery.whereEqualTo("showOnMap", true)
                }

                val listener =
                    baseQuery.addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            Log.e("ActivitiesRepository", "Listen failed", error)
                            close(error)
                            return@addSnapshotListener
                        }

                        val matchingActivities = mutableListOf<MapMarker>()
                        snapshot?.documents?.forEach { doc ->
                            val activityDto = doc.toObject(ActivityDto::class.java) ?: return@forEach
                            val activityLocation = activityDto.location ?: return@forEach
                            val activityRadius = activityDto.radius ?: return@forEach

                            val distanceToActivity =
                                GeoFireUtils.getDistanceBetween(
                                    center,
                                    GeoLocation(activityLocation.latitude, activityLocation.longitude),
                                )

                            if (distanceToActivity <= activityRadius) {
                                matchingActivities.add(
                                    MapMarker(
                                        id = doc.id,
                                        position =
                                            GeoPoint(
                                                activityLocation.latitude,
                                                activityLocation.longitude,
                                            ),
                                        creatorName = activityDto.creatorName ?: "Unknown",
                                        creatorId = currentUser.uid,
                                        shortDescription = (
                                            if (activityDto.description != null && activityDto.description.length < 30) {
                                                activityDto.description
                                            } else if (activityDto.description != null) {
                                                activityDto.description.substring(0, 30) + "..."
                                            } else {
                                                ""
                                            }
                                        ),
                                    ),
                                )
                            }
                        }

                        trySend(matchingActivities).isSuccess
                    }
                awaitClose {
                    listener.remove()
                }
            }

        override suspend fun createActivity(
            description: String,
            location: GeoPoint?,
            radius: Double,
            showOnMap: Boolean,
        ): Result<Unit> =
            try {
                val currentUser = auth.currentUser
                checkNotNull(currentUser) { "User must be authenticated to create an activity" }

                var geohash: String? = null
                var firestoreLocation: com.google.firebase.firestore.GeoPoint? = null

                if (location != null && showOnMap) {
                    firestoreLocation =
                        com.google.firebase.firestore
                            .GeoPoint(location.latitude, location.longitude)
                    geohash =
                        GeoFireUtils.getGeoHashForLocation(
                            GeoLocation(location.latitude, location.longitude),
                        )
                }

                val docRef = firestore.collection("activities").document()

                val newActivity =
                    ActivityDto(
                        id = docRef.id,
                        creatorId = currentUser.uid,
                        creatorName = currentUser.displayName ?: "Anonymous",
                        description = description,
                        location = firestoreLocation,
                        radius = radius,
                        showOnMap = showOnMap,
                        geohash = geohash,
                    )

                docRef.set(newActivity).await()

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }

        override suspend fun getActivityById(id: String): Activity? {
            return try {
                val document =
                    firestore
                        .collection("activities")
                        .document(id)
                        .get()
                        .await()

                if (!document.exists()) return null

                val dto = document.toObject(ActivityDto::class.java) ?: return null
                dto.toActivity(id)
            } catch (e: Exception) {
                Log.e("ActivitiesRepository", "Error fetching activity by id: $id", e)
                null
            }
        }

        override suspend fun closeActivity(activityId: String) {
            val uid = auth.currentUser?.uid ?: throw IllegalStateException("Auth required")
            val ref = firestore.collection("activities").document(activityId)

            firestore
                .runTransaction { tx ->
                    val snap = tx.get(ref)
                    if (!snap.exists()) {
                        throw NoSuchElementException("Activity not found")
                    }
                    val creatorId =
                        snap.getString("creatorId")
                            ?: throw IllegalStateException("Invalid activity: missing creatorId")
                    if (creatorId != uid) {
                        throw SecurityException("Only creator can delete activity")
                    }
                    tx.delete(ref)
                }.await()
        }
    }
