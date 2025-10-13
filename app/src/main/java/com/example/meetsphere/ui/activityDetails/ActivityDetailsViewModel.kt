package com.example.meetsphere.ui.activityDetails

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.meetsphere.data.remote.dto.ActivityDto
import com.example.meetsphere.domain.model.Activity
import com.example.meetsphere.domain.repository.ActivitiesRepository
import com.example.meetsphere.domain.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ActivityDetailsViewModel
    @Inject
    constructor(
        private val activitiesRepository: ActivitiesRepository,
        private val chatRepository: ChatRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<ActivityDetailsUiState>(ActivityDetailsUiState.Loading)
        val uiState: StateFlow<ActivityDetailsUiState> = _uiState.asStateFlow()

        private val _isCreatingChat = MutableStateFlow(false)
        val isCreatingChat = _isCreatingChat.asStateFlow()

        private val _chatError = MutableStateFlow<String?>(null)
        val chatError = _chatError.asStateFlow()

        private val _navigateToChat = MutableSharedFlow<String?>()
        val navigateToChat = _navigateToChat.asSharedFlow()

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

        fun onMessageClick(creatorId: String) {
            if (creatorId.isBlank()) {
                _chatError.value = "Invalid creator ID"
                return
            }
            viewModelScope.launch {
                _isCreatingChat.value = true
                _chatError.value = null
                val result = chatRepository.createOrGetChat(creatorId)
                _isCreatingChat.value = false
                if (result.isSuccess) {
                    val chatId =
                        result.getOrNull() ?: run {
                            _chatError.value = "Failed to get chat ID"
                            return@launch
                        }
                    _navigateToChat.emit(chatId)
                } else {
                    _chatError.value = result.exceptionOrNull()?.message ?: "Failed to create chat"
                }
            }
        }

        fun clearChatError() {
            _chatError.value = null
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

    data class ChatState(
        val isCreatingChat: Boolean = false,
        val chatError: String? = null,
    )
}
