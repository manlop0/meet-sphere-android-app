package com.example.meetsphere.data.remote.dto

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class ChatMessageDto(
    val senderId: String = "",
    val text: String = "",
    @ServerTimestamp
    val timestamp: Date? = null,
)
