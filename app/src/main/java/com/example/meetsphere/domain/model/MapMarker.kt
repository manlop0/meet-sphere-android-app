package com.example.meetsphere.domain.model

import org.osmdroid.util.GeoPoint

data class MapMarker(
    val id: String,
    val position: GeoPoint,
    val creatorName: String,
    val shortDescription: String,
)
