package com.kashif.deepseek.data.repository

import com.kashif.deepseek.data.local.dao.ChatDao
import com.kashif.deepseek.domain.model.ChatMessage
import com.kashif.deepseek.domain.repository.ChatRepository


class ChatRepositoryImpl(private val dao: ChatDao) : ChatRepository {
    override fun getAllSessions() = dao.observeAllSessions()
    override suspend fun createSession(session: com.kashif.deepseek.domain.model.ChatSession) = dao.insertSession(session)
    override suspend fun updateSession(session: com.kashif.deepseek.domain.model.ChatSession) = dao.updateSession(session)
    override suspend fun deleteSession(session: com.kashif.deepseek.domain.model.ChatSession) = dao.deleteSession(session)
    override fun getMessages(sessionId: String) = dao.getMessagesForSession(sessionId)
    override suspend fun insertMessage(message: ChatMessage) = dao.insertMessage(message)
    override suspend fun updateMessage(message: ChatMessage) = dao.updateMessage(message)
}