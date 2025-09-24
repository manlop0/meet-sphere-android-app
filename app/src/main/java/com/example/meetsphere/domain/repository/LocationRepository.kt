package com.example.meetsphere.domain.repository

import kotlinx.coroutines.flow.Flow
import org.osmdroid.util.GeoPoint

interface LocationRepository {
    fun userLocationFlow(): Flow<GeoPoint>

    fun startLocationUpdates()

    fun stopLocationUpdates()
}
