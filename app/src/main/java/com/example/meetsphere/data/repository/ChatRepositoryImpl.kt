package com.example.meetsphere.data.repository

import com.example.meetsphere.data.remote.dto.ChatMessageDto
import com.example.meetsphere.data.remote.dto.ChatPreviewDto
import com.example.meetsphere.data.remote.dto.ParticipantInfoDto
import com.example.meetsphere.data.toChatMessage
import com.example.meetsphere.data.toChatPreview
import com.example.meetsphere.domain.model.ChatInfo
import com.example.meetsphere.domain.model.ChatMessage
import com.example.meetsphere.domain.model.ChatPreview
import com.example.meetsphere.domain.repository.ChatRepository
import com.example.meetsphere.domain.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.jvm.java

@Singleton
class ChatRepositoryImpl
    @Inject
    constructor(
        private val firestore: FirebaseFirestore,
        private val auth: FirebaseAuth,
        private val userRepository: UserRepository,
    ) : ChatRepository {
        override fun getChats(): Flow<List<ChatPreview>> {
            val currentUserId = auth.currentUser?.uid ?: return emptyFlow()

            return firestore
                .collection("chats")
                .whereArrayContains("participantsIds", currentUserId)
                .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING)
                .snapshots() // snapshots() возвращает Flow<QuerySnapshot> для real-time обновлений
                .map { snapshot ->
                    snapshot.documents.mapNotNull { doc ->
                        val dto = doc.toObject(ChatPreviewDto::class.java)
                        dto?.id = doc.id
                        dto?.toChatPreview(currentUserId)
                    }
                }
        }

        override fun getMessages(chatId: String): Flow<List<ChatMessage>> =
            firestore
                .collection("chats")
                .document(chatId)
                .collection("messages")
                .orderBy("timestamp")
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { doc ->
                        doc.toObject(ChatMessageDto::class.java)?.toChatMessage(auth.currentUser!!.uid)
                    }
                }

        override suspend fun sendMessage(
            chatId: String,
            text: String,
        ): Result<Unit> =
            try {
                val currentUserId = auth.currentUser!!.uid
                val message = ChatMessageDto(senderId = currentUserId, text = text)

                firestore
                    .collection("chats")
                    .document(chatId)
                    .collection("messages")
                    .add(message)
                    .await()

                firestore
                    .collection("chats")
                    .document(chatId)
                    .update(
                        mapOf(
                            "lastMessage" to text,
                            "lastMessageTimestamp" to FieldValue.serverTimestamp(),
                        ),
                    ).await()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }

        override suspend fun createOrGetChat(recipientId: String): Result<String> {
            return try {
                val currentUserId = auth.currentUser!!.uid

                if (currentUserId == recipientId) {
                    return Result.failure(IllegalArgumentException("Cannot create chat with oneself."))
                }

                val participants = listOf(currentUserId, recipientId)

                val existingChat =
                    firestore
                        .collection("chats")
                        .whereArrayContains("participantsIds", currentUserId)
                        .get()
                        .await()
                        .documents
                        .firstOrNull { doc ->
                            val ids = doc.get("participantsIds") as? List<*>
                            ids?.contains(recipientId) == true && ids.size == 2
                        }

                if (existingChat != null) {
                    Result.success(existingChat.id)
                } else {
                    val currentUserProfile = userRepository.getCurrentUserProfile().getOrThrow()
                    val recipientProfile = userRepository.getUserProfile(recipientId).getOrThrow()

                    val currentUserInfo = ParticipantInfoDto(name = currentUserProfile.username)
                    val recipientInfo = ParticipantInfoDto(name = recipientProfile.username)

                    val newChatDto =
                        ChatPreviewDto(
                            participantsIds = participants,
                            participantsInfo =
                                mapOf(
                                    currentUserId to currentUserInfo,
                                    recipientId to recipientInfo,
                                ),
                            lastMessage = "Chat was created.",
                            lastMessageTimestamp = null,
                        )

                    val newChatDocument = firestore.collection("chats").add(newChatDto).await()
                    Result.success(newChatDocument.id)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        override suspend fun getChatInfo(chatId: String): ChatInfo? {
            return try {
                val currentUserId = auth.currentUser?.uid ?: return null
                val doc =
                    firestore
                        .collection("chats")
                        .document(chatId)
                        .get()
                        .await()

                val participantsIds = doc.get("participantsIds") as? List<String> ?: return null
                val participantsInfo = doc.get("participantsInfo") as? Map<String, Map<String, Any>> ?: return null

                val companionId = participantsIds.firstOrNull { it != currentUserId } ?: return null
                val companionInfo = participantsInfo[companionId] ?: return null
                val companionName = companionInfo["name"] as? String ?: "Unknown"

                ChatInfo(companionName = companionName, companionId = companionId)
            } catch (e: Exception) {
                null
            }
        }
    }
