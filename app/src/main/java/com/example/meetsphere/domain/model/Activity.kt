package com.example.meetsphere.domain.model

import org.osmdroid.util.GeoPoint
import java.util.Date

data class Activity(
    val id: String,
    val creatorId: String,
    val creatorName: String,
    val shortDescription: String,
    val fullDescription: String,
    val location: GeoPoint?,
    val radius: Double?,
    val showOnMap: Boolean,
    val createdAt: Date?,
)
