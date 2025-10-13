package com.example.meetsphere.ui.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.meetsphere.domain.model.ChatMessage
import com.example.meetsphere.domain.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val messageText: String = "",
)

@HiltViewModel
class ChatViewModel
    @Inject
    constructor(
        private val chatRepository: ChatRepository,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val chatId: String = checkNotNull(savedStateHandle["chatId"])

        private val _uiState = MutableStateFlow(ChatUiState())
        val uiState = _uiState.asStateFlow()

        init {
            chatRepository
                .getMessages(chatId)
                .onEach { messages ->
                    _uiState.update { it.copy(messages = messages) }
                }.launchIn(viewModelScope)
        }

        fun onMessageChange(text: String) {
            _uiState.update { it.copy(messageText = text) }
        }

        fun sendMessage() {
            val text = _uiState.value.messageText.trim()
            if (text.isBlank()) return

            viewModelScope.launch {
                chatRepository.sendMessage(chatId, text)
                _uiState.update { it.copy(messageText = "") }
            }
        }
    }
