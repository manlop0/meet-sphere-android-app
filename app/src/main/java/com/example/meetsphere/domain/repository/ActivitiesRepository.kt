package com.example.meetsphere.domain.repository

import com.example.meetsphere.domain.model.Activity
import com.example.meetsphere.domain.model.MapMarker
import kotlinx.coroutines.flow.Flow
import org.osmdroid.util.GeoPoint

interface ActivitiesRepository {
    fun getActivitiesNearby(
        centerPoint: GeoPoint,
        onMapOnly: Boolean,
    ): Flow<List<MapMarker>>

    suspend fun createActivity(
        description: String,
        location: GeoPoint?,
        radius: Double,
        showOnMap: Boolean,
    ): Result<Unit>

    suspend fun getActivityById(id: String): Activity?
}
