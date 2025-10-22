package com.example.meetsphere.ui.map

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.meetsphere.domain.model.MapMarker
import com.example.meetsphere.domain.repository.ActivitiesRepository
import com.example.meetsphere.domain.repository.AuthRepository
import com.example.meetsphere.domain.repository.LocationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import javax.inject.Inject

sealed class MapNavigationEvent {
    data class ToActivityDetails(
        val activityId: String,
    ) : MapNavigationEvent()
}

data class MapUiState(
    val cameraPosition: GeoPoint = GeoPoint(0.0, 0.0),
    val zoomLevel: Double = 12.0,
    val activities: List<MapMarker> = emptyList(),
    val isLoading: Boolean = true,
    val currentUserId: String = "",
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MapViewModel
    @Inject
    constructor(
        private val activitiesRepository: ActivitiesRepository,
        private val locationRepository: LocationRepository,
        private val authRepository: AuthRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(MapUiState())
        val uiState = _uiState.asStateFlow()

        private val _navigationEvents = MutableSharedFlow<MapNavigationEvent>()
        val navigationEvents = _navigationEvents.asSharedFlow()

        private val _selectedActivity = MutableStateFlow<MapMarker?>(null)
        val selectedActivity = _selectedActivity.asStateFlow()

        private var currentUserLocation: GeoPoint? = null

        init {
            viewModelScope.launch {
                authRepository.currentUser
                    .filterNotNull()
                    .flatMapLatest { user ->
                        _uiState.update { it.copy(currentUserId = user.uid) }

                        locationRepository
                            .userLocationFlow()
                            .onEach { location ->
                                currentUserLocation = location
                                if (_uiState.value.isLoading) {
                                    _uiState.update { it.copy(cameraPosition = location) }
                                }
                            }.filterNotNull()
                            .flatMapLatest { location ->
                                activitiesRepository.getActivitiesNearby(location, onMapOnly = true)
                            }
                    }.collect { activities ->
                        _uiState.update {
                            it.copy(
                                activities = activities,
                                isLoading = false,
                            )
                        }
                    }
            }
        }

        fun onMarkerClick(marker: MapMarker) {
            if (_selectedActivity.value?.id == marker.id) {
                _selectedActivity.value = null
            } else {
                _selectedActivity.value = marker
            }
        }

        fun onMapClick() {
            _selectedActivity.value = null
        }

        fun onDetailsClick(activityId: String) {
            viewModelScope.launch {
                _navigationEvents.emit(MapNavigationEvent.ToActivityDetails(activityId))
            }
        }

        fun centerOnUserLocation() {
            currentUserLocation?.let { location ->
                _uiState.update { it.copy(cameraPosition = location, zoomLevel = 13.0) }
            }
        }

        fun onMapScrolled(
            center: GeoPoint,
            zoom: Double,
        ) {
            _uiState.update { it.copy(cameraPosition = center, zoomLevel = zoom) }
        }
    }
