package com.kashif.kollama.domain.interfaces


import com.kashif.kollama.ChatMessage
import com.kashif.kollama.ChatSession
import com.kashif.kollama.domain.model.ChatMessageDomainModel
import com.kashif.kollama.domain.model.ChatSessionDomainModel
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    suspend fun getAllSessions(): Flow<List<ChatSessionDomainModel>>
    suspend fun createSession(session: ChatSession)
    suspend fun updateSession(session: ChatSession)
    suspend fun deleteSession(session: ChatSession)
    suspend fun getMessages(sessionId: String): Flow<List<ChatMessageDomainModel>>
    suspend fun insertMessage(message: ChatMessage)
    suspend fun updateMessage(message: ChatMessage)
}
