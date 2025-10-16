package com.example.meetsphere.ui.chatList

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.meetsphere.domain.model.ChatPreview
import com.example.meetsphere.domain.repository.ChatRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class ChatListUiState(
    val isLoading: Boolean = true,
    val isError: Boolean = false,
    val errorMessage: String? = null,
    val chats: List<ChatPreview> = emptyList(),
)

@HiltViewModel
class ChatListViewModel
    @Inject
    constructor(
        private val chatRepository: ChatRepository,
        private val auth: FirebaseAuth,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ChatListUiState())
        val uiState = _uiState.asStateFlow()

        init {
            loadChats()
        }

        private fun loadChats() {
            val currentUserId = auth.currentUser?.uid
            if (currentUserId == null) {
                _uiState.update { it.copy(isLoading = false, chats = emptyList()) }
                return
            }

            chatRepository
                .getChats()
                .onEach { chats ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isError = false,
                            errorMessage = null,
                            chats = chats,
                        )
                    }
                }.catch { exception ->
                    Log.e("ChatListViewModel", "Error loading chats", exception)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isError = true,
                            errorMessage = exception.message ?: "Failed to load chats",
                            chats = emptyList(),
                        )
                    }
                }.launchIn(viewModelScope)
        }

        fun retry() {
            _uiState.update { it.copy(isError = false, errorMessage = null, isLoading = true) }
            loadChats()
        }
    }
