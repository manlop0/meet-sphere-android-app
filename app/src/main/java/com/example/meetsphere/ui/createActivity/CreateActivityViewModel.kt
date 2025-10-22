package com.example.meetsphere.ui.createActivity

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.meetsphere.domain.repository.ActivitiesRepository
import com.example.meetsphere.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import javax.inject.Inject

data class CreateActivityUiState(
    val description: String = "",
    val showLocation: Boolean = true,
    val radius: Float = 1000f,
    val isCreating: Boolean = false,
    val createSuccess: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class CreateActivityViewModel
    @Inject
    constructor(
        private val activitiesRepository: ActivitiesRepository,
        private val authRepository: AuthRepository,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(CreateActivityUiState())
        val uiState = _uiState.asStateFlow()

        private val latitude: Double = checkNotNull(savedStateHandle["latitude"]).toString().toDouble()
        private val longitude: Double = checkNotNull(savedStateHandle["longitude"]).toString().toDouble()

        fun onDescriptionChange(newDescription: String) {
            _uiState.update { it.copy(description = newDescription) }
        }

        fun onShowLocationToggle(show: Boolean) {
            _uiState.update { it.copy(showLocation = show) }
        }

        fun onRadiusChange(newRadius: Float) {
            _uiState.update { it.copy(radius = newRadius) }
        }

        fun onCreateActivity() {
            viewModelScope.launch {
                _uiState.update { it.copy(isCreating = true, error = null) }

                val currentUser = authRepository.getCurrentUser()

                if (currentUser == null) {
                    _uiState.update {
                        it.copy(
                            isCreating = false,
                            error = "User not authenticated",
                        )
                    }
                    return@launch
                }

                val location =
//                    if (_uiState.value.showLocation) {
                    GeoPoint(latitude, longitude)
//                    } else {
//                        null
//                    }

                val result =
                    activitiesRepository.createActivity(
                        userId = currentUser.uid,
                        userName = currentUser.username,
                        description = _uiState.value.description.trim(),
                        location = location,
                        radius = _uiState.value.radius.toDouble(),
                        showOnMap = _uiState.value.showLocation,
                    )

                result
                    .onSuccess {
                        kotlinx.coroutines.delay(1500)
                        _uiState.update { it.copy(isCreating = false, createSuccess = true) }
                    }.onFailure { exception ->
                        Log.e("CreateActivityViewModel", "Failed to create activity", exception)
                        _uiState.update {
                            it.copy(
                                isCreating = false,
                                error = exception.message ?: "Failed to create activity",
                            )
                        }
                    }
            }
        }
    }
