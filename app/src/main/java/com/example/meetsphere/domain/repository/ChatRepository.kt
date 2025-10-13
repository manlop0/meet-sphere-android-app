package com.example.meetsphere.domain.repository

import com.example.meetsphere.domain.model.ChatMessage
import com.example.meetsphere.domain.model.ChatPreview
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun getChats(): Flow<List<ChatPreview>> // ChatPreview - новая модель для списка

    fun getMessages(chatId: String): Flow<List<ChatMessage>> // ChatMessage - модель сообщения

    suspend fun sendMessage(
        chatId: String,
        text: String,
    ): Result<Unit>

    suspend fun createOrGetChat(recipientId: String): Result<String> // Возвращает ID чата
}
