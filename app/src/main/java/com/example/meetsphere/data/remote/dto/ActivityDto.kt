package com.example.meetsphere.data.remote.dto

import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class ActivityDto(
    val id: String? = null,
    val creatorId: String? = null,
    val creatorName: String? = null,
    val description: String? = null,
    val location: GeoPoint? = null,
    val showOnMap: Boolean = false,
    val radius: Double? = null,
    val geohash: String? = null,
    @ServerTimestamp
    val createdAt: Date? = null,
)
