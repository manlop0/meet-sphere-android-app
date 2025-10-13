package com.example.meetsphere.data

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.meetsphere.data.remote.dto.ActivityDto
import com.example.meetsphere.data.remote.dto.ChatMessageDto
import com.example.meetsphere.data.remote.dto.ChatPreviewDto
import com.example.meetsphere.data.remote.dto.UserDto
import com.example.meetsphere.domain.model.Activity
import com.example.meetsphere.domain.model.ChatMessage
import com.example.meetsphere.domain.model.ChatPreview
import com.example.meetsphere.domain.model.User
import org.osmdroid.util.GeoPoint
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

fun ChatMessageDto.toChatMessage(currentUserId: String): ChatMessage =
    ChatMessage(
        senderId = this.senderId,
        text = this.text,
        timestamp = this.timestamp?.toLocalDateTime() ?: LocalDateTime.now(),
        isSentByCurrentUser = this.senderId == currentUserId,
    )

fun ChatPreviewDto.toChatPreview(currentUserId: String): ChatPreview? {
    val recipientId = this.participantsIds.firstOrNull { it != currentUserId } ?: return null

    val recipientInfo = this.participantsInfo[recipientId]

    return ChatPreview(
        chatId = this.id,
        recipientName = recipientInfo?.name ?: "Unknown User",
        recipientImageUrl = recipientInfo?.profileImageUrl ?: "",
        lastMessage = this.lastMessage,
        lastMessageTimestamp = this.lastMessageTimestamp?.toLocalDateTime() ?: LocalDateTime.now(),
    )
}

fun ActivityDto.toActivity(id: String): Activity {
    val fullDescription = this.description ?: ""
    val shortDescription =
        if (fullDescription.length < 30) {
            fullDescription
        } else {
            fullDescription.substring(0, 30) + "..."
        }

    val locationGeoPoint =
        this.location?.let { geoPoint ->
            GeoPoint(geoPoint.latitude, geoPoint.longitude)
        }

    return Activity(
        id = id,
        creatorId = this.creatorId ?: "",
        creatorName = this.creatorName ?: "Unknown",
        shortDescription = shortDescription,
        fullDescription = fullDescription,
        location = locationGeoPoint,
        radius = this.radius,
        showOnMap = this.showOnMap,
        createdAt = this.createdAt,
    )
}

fun UserDto.toUser(): User =
    User(
        uid = this.uid,
        username = this.username,
        email = this.email,
        profileImageUrl = this.profileImageUrl,
    )

private fun java.util.Date.toLocalDateTime(): LocalDateTime =
    Instant
        .ofEpochMilli(this.time)
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime()
