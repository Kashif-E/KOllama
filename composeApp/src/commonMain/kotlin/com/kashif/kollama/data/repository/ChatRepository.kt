package com.kashif.kollama.data.repository

import com.kashif.kollama.ChatMessage
import com.kashif.kollama.ChatSession
import com.kashif.kollama.data.local.database.DatabaseWrapper
import com.kashif.kollama.domain.interfaces.ChatRepository


class ChatRepositoryImpl(private val db: DatabaseWrapper) : ChatRepository {
    override suspend fun getAllSessions() = db.observeAllSessions()
    override suspend fun createSession(session: ChatSession) = db.insertSession(session)
    override suspend fun updateSession(session: ChatSession) = db.updateSession(session)
    override suspend fun deleteSession(session: ChatSession) = db.deleteSession(session)
    override suspend fun getMessages(sessionId: String) = db.getMessagesForSession(sessionId)
    override suspend fun insertMessage(message: ChatMessage) = db.insertMessage(message)
    override suspend fun updateMessage(message: ChatMessage) = db.updateMessage(message)
}