package com.example.meetsphere.ui.activities

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.meetsphere.domain.model.MapMarker
import com.example.meetsphere.domain.repository.ActivitiesRepository
import com.example.meetsphere.domain.repository.AuthRepository
import com.example.meetsphere.domain.repository.ChatRepository
import com.example.meetsphere.domain.repository.LocationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import javax.inject.Inject

sealed class ActivitiesNavigationEvent {
    data class ToActivityDetails(
        val activityId: String,
    ) : ActivitiesNavigationEvent()

    data class ToChat(
        val chatId: String,
    ) : ActivitiesNavigationEvent()

    data class ToCreateActivity(
        val location: GeoPoint,
    ) : ActivitiesNavigationEvent()
}

data class UiState(
    val loading: Boolean = true,
    val myActivity: MapMarker? = null,
    val othersActivities: List<MapMarker> = emptyList(),
    val error: String? = null,
    val isCreatingChat: Boolean = false,
    val chatError: String? = null,
)

@HiltViewModel
class ActivitiesViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val locationRepository: LocationRepository,
        private val activitiesRepository: ActivitiesRepository,
        private val chatRepository: ChatRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(UiState())
        val uiState: StateFlow<UiState> = _uiState.asStateFlow()

        private val _navigationEvents = MutableSharedFlow<ActivitiesNavigationEvent>()
        val navigationEvents: SharedFlow<ActivitiesNavigationEvent> = _navigationEvents.asSharedFlow()

        private var currentUserLocation: GeoPoint? = null
        private var currentUserId: String = ""

        init {
            observeActivities()
        }

        @OptIn(ExperimentalCoroutinesApi::class)
        private fun observeActivities() {
            viewModelScope.launch {
                authRepository.currentUserFlow
                    .filterNotNull()
                    .flatMapLatest { user ->
                        currentUserId = user.uid
                        Log.d("ActivitiesViewModel", "Current user updated: ${user.username} (${user.uid})")

                        locationRepository
                            .userLocationFlow()
                            .onEach { loc -> currentUserLocation = loc }
                            .filterNotNull()
                            .distinctUntilChanged()
                            .flatMapLatest { loc ->
                                activitiesRepository.getActivitiesNearby(loc, onMapOnly = false)
                            }
                    }.onStart { _uiState.update { it.copy(loading = true, error = null) } }
                    .catch { e ->
                        Log.e("ActivitiesViewModel", "Error loading activities", e)
                        _uiState.update { it.copy(loading = false, error = e.message) }
                    }.collect { markers ->
                        Log.d("ActivitiesViewModel", "Loaded ${markers.size} activities for user: $currentUserId")
                        markers.forEach { marker ->
                            Log.d(
                                "ActivitiesViewModel",
                                "Activity: ${marker.creatorName} (${marker.creatorId}), isMine: ${marker.creatorId == currentUserId}",
                            )
                        }

                        val my = markers.firstOrNull { it.creatorId == currentUserId }
                        val others = markers.filter { it.creatorId != currentUserId }

                        Log.d("ActivitiesViewModel", "My activity: ${my?.creatorName}, Others: ${others.size}")

                        _uiState.update {
                            it.copy(
                                loading = false,
                                myActivity = my,
                                othersActivities = others,
                                error = null,
                            )
                        }
                    }
            }
        }

        fun onCloseMyActivity() {
            val id = _uiState.value.myActivity?.id ?: return
            viewModelScope.launch {
                _uiState.update { it.copy(loading = true) }
                val res = runCatching { activitiesRepository.closeActivity(id) }
                if (res.isFailure) {
                    _uiState.update { it.copy(loading = false, error = "Failed to close activity") }
                }
            }
        }

        fun onOpenDetails(activityId: String) {
            viewModelScope.launch {
                _navigationEvents.emit(ActivitiesNavigationEvent.ToActivityDetails(activityId))
            }
        }

        fun onOpenChat(creatorId: String) {
            if (creatorId.isBlank()) {
                _uiState.update { it.copy(chatError = "Invalid creator ID") }
                return
            }
            viewModelScope.launch {
                _uiState.update { it.copy(isCreatingChat = true, chatError = null) }
                val result = chatRepository.createOrGetChat(creatorId)
                _uiState.update { it.copy(isCreatingChat = false) }

                if (result.isSuccess) {
                    val chatId =
                        result.getOrNull() ?: run {
                            _uiState.update { it.copy(chatError = "Failed to get chat ID") }
                            return@launch
                        }
                    _navigationEvents.emit(ActivitiesNavigationEvent.ToChat(chatId))
                } else {
                    _uiState.update {
                        it.copy(chatError = result.exceptionOrNull()?.message ?: "Failed to create chat")
                    }
                }
            }
        }

        fun onCreateActivity() {
            val location = currentUserLocation ?: return
            viewModelScope.launch {
                _navigationEvents.emit(ActivitiesNavigationEvent.ToCreateActivity(location))
            }
        }

        fun clearChatError() {
            _uiState.update { it.copy(chatError = null) }
        }
    }
