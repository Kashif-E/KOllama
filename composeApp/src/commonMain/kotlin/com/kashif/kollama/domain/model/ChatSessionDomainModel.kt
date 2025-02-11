package com.kashif.kollama.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.datetime.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Immutable
data class ChatSessionDomainModel @OptIn(ExperimentalUuidApi::class) constructor(
    val id: String = Uuid.random().toString(),
    val title: String,
    val createdAt: Long = Clock.System.now().toEpochMilliseconds(),
    val lastModified: Long = Clock.System.now().toEpochMilliseconds(),
)

fun ChatSessionDomainModel.toDataModel() = com.kashif.kollama.ChatSession(
    id = id,
    title = title,
    createdAt = createdAt,
    lastModified = lastModified,
)
fun List<ChatSessionDomainModel>.toDataModel() = map { it.toDataModel() }