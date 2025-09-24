package com.example.meetsphere.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import com.example.meetsphere.domain.repository.LocationRepository
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import org.osmdroid.util.GeoPoint
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationRepositoryImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : LocationRepository {
        private val fusedLocationClient: FusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(context)

        private val locationFlow = MutableSharedFlow<GeoPoint>(replay = 1)

        override fun userLocationFlow(): Flow<GeoPoint> = locationFlow.asSharedFlow()

        private val locationCallback =
            object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    locationResult.lastLocation?.let { location ->
                        locationFlow.tryEmit(GeoPoint(location.latitude, location.longitude))
                    }
                }
            }

        @SuppressLint("MissingPermission")
        override fun startLocationUpdates() {
            val locationRequest =
                LocationRequest
                    .Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L)
                    .setWaitForAccurateLocation(false)
                    .setMinUpdateIntervalMillis(3000L)
                    .build()

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper(),
            )
        }

        override fun stopLocationUpdates() {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }
