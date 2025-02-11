package com.kashif.kollama.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.datetime.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Immutable
data class ChatMessageDomainModel @OptIn(ExperimentalUuidApi::class) constructor(
    val id: String = Uuid.random().toString(),
    val sessionId: String,
    val content: String,
    val isUser: Boolean,
    val status: MessageStatus = MessageStatus.SENT,
    val timestamp: Long = Clock.System.now().toEpochMilliseconds()
)

enum class MessageStatus { SENT, SENDING, ERROR }

fun ChatMessageDomainModel.toDataModel() = com.kashif.kollama.ChatMessage(
    id = id,
    sessionId = sessionId,
    content = content,
    isUser = if (isUser) 1 else 0,
    status = when (status) {
        MessageStatus.SENT -> "SENT"
        MessageStatus.SENDING -> "SENDING"
        MessageStatus.ERROR -> "ERROR"
    },
    timestamp = timestamp
)
fun List<ChatMessageDomainModel>.toDataModel() = map { it.toDataModel() }