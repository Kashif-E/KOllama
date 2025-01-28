package com.kashif.deepseek.domain.repository

import com.kashif.deepseek.domain.model.ChatMessage
import com.kashif.deepseek.domain.model.ChatSession
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun getAllSessions(): Flow<List<ChatSession>>
    suspend fun createSession(session: ChatSession)
    suspend fun updateSession(session: ChatSession)
    suspend fun deleteSession(session: ChatSession)
    fun getMessages(sessionId: String): Flow<List<ChatMessage>>
    suspend fun insertMessage(message: ChatMessage)
    suspend fun updateMessage(message: ChatMessage)
}
