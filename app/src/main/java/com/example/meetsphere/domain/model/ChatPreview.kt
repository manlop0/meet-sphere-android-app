package com.example.meetsphere.domain.model

import java.time.LocalDateTime

data class ChatPreview(
    val chatId: String,
    val recipientName: String,
    val recipientImageUrl: String,
    val lastMessage: String,
    val lastMessageTimestamp: LocalDateTime,
)
