package com.example.meetsphere.ui.activityDetails

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.meetsphere.data.remote.dto.ActivityDto
import com.example.meetsphere.domain.model.Activity
import com.example.meetsphere.domain.repository.ActivitiesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ActivityDetailsViewModel
    @Inject
    constructor(
        private val activitiesRepository: ActivitiesRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<ActivityDetailsUiState>(ActivityDetailsUiState.Loading)
        val uiState: StateFlow<ActivityDetailsUiState> = _uiState.asStateFlow()

        fun loadActivityDetails(activityId: String) {
            viewModelScope.launch {
                try {
                    val activity = activitiesRepository.getActivityById(activityId)
                    if (activity != null) {
                        _uiState.value =
                            ActivityDetailsUiState.Success(
                                activity = activity,
                                creatorName = activity.creatorName,
                            )
                    } else {
                        _uiState.value = ActivityDetailsUiState.Error("Activity wasn't found")
                    }
                } catch (e: Exception) {
                    _uiState.value = ActivityDetailsUiState.Error(e.message ?: "Unknown error")
                }
            }
        }
    }

sealed class ActivityDetailsUiState {
    object Loading : ActivityDetailsUiState()

    data class Success(
        val activity: Activity,
        val creatorName: String,
    ) : ActivityDetailsUiState()

    data class Error(
        val message: String,
    ) : ActivityDetailsUiState()
}
