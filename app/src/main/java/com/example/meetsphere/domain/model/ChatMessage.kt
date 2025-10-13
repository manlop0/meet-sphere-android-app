package com.example.meetsphere.domain.model

import java.time.LocalDateTime

data class ChatMessage(
    val senderId: String,
    val text: String,
    val timestamp: LocalDateTime,
    val isSentByCurrentUser: Boolean,
)
