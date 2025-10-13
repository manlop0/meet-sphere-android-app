package com.example.meetsphere.data.remote.dto

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class ParticipantInfoDto(
    val name: String = "",
    val profileImageUrl: String = "", // на будущее
)

data class ChatPreviewDto(
    // Мы будем хранить ID документа отдельно после его получения
    @get:JvmName("getId_") // Избегаем конфликта имен с возможным полем "id"
    var id: String = "",
    val participantsIds: List<String> = emptyList(),
    // Храним информацию об участниках прямо в чате для быстрой загрузки
    val participantsInfo: Map<String, ParticipantInfoDto> = emptyMap(),
    val lastMessage: String = "",
    @ServerTimestamp
    val lastMessageTimestamp: Date? = null,
)
