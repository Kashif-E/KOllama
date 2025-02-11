package com.kashif.kollama.data.local.database

import app.cash.sqldelight.coroutines.asFlow
import com.kashif.kollama.ChatDatabaseQueries
import com.kashif.kollama.ChatDatabaseX
import com.kashif.kollama.ChatMessage
import com.kashif.kollama.ChatSession
import com.kashif.kollama.domain.model.ChatMessageDomainModel
import com.kashif.kollama.domain.model.ChatSessionDomainModel
import com.kashif.kollama.domain.model.MessageStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.jvm.JvmName

class DatabaseWrapper(private val context: Any) {
    private var database: ChatDatabaseX? = null
    private var queries: ChatDatabaseQueries? = null
    private val initializationMutex = Mutex()

    private suspend fun ensureDatabaseInitialized() {
        if (database == null) {
            initializationMutex.withLock {
                if (database == null) {
                    withContext(Dispatchers.Default) {
                        database = createDatabaseWrapper(context)
                        queries = database?.chatDatabaseQueries
                    }
                }
            }
        }
    }

    private fun requireDatabase(): ChatDatabaseQueries {
        return queries ?: throw IllegalStateException("Database not initialized. Please ensure to call ensureDatabaseInitialized() first")
    }

    suspend fun observeAllSessions(): Flow<List<ChatSessionDomainModel>> {
        ensureDatabaseInitialized()
        return requireDatabase()
            .selectAllSessions()
            .asFlow()
            .map { it.executeAsList().asDomainModel() }
    }

    suspend fun insertSession(session: ChatSession) {
        ensureDatabaseInitialized()
        requireDatabase().insertSession(
            id = session.id,
            title = session.title,
            createdAt = session.createdAt,
            lastModified = session.lastModified
        )
    }

    suspend fun updateSession(session: ChatSession) {
        ensureDatabaseInitialized()
        requireDatabase().updateSession(
            title = session.title,
            lastModified = session.lastModified,
            id = session.id
        )
    }

    suspend fun deleteSession(session: ChatSession) {
        ensureDatabaseInitialized()
        requireDatabase().deleteSession(session.id)
    }

    suspend fun getMessagesForSession(sessionId: String): Flow<List<ChatMessageDomainModel>> {
        ensureDatabaseInitialized()
        return requireDatabase()
            .selectMessagesBySession(sessionId)
            .asFlow()
            .map { it.executeAsList().asDomainModel() }
    }

    suspend fun insertMessage(message: ChatMessage) {
        ensureDatabaseInitialized()
        requireDatabase().insertMessage(
            id = message.id,
            sessionId = message.sessionId,
            content = message.content,
            isUser = message.isUser,
            status = message.status,
            timestamp = message.timestamp
        )
    }

    suspend fun updateMessage(message: ChatMessage) {
        ensureDatabaseInitialized()
        requireDatabase().updateMessage(
            content = message.content,
            status = message.status,
            id = message.id
        )
    }
}

expect suspend fun createDatabaseWrapper(context: Any): ChatDatabaseX


fun ChatSession.asDomainModel() = ChatSessionDomainModel(
    id = id,
    title = title,
    createdAt = createdAt,
    lastModified = lastModified
)

@JvmName("asDomainModelChatSession")
fun List<ChatSession>.asDomainModel() = map { it.asDomainModel() }

fun ChatMessage.asDomainModel() = ChatMessageDomainModel(
    id = id,
    sessionId = sessionId,
    content = content,
    isUser = isUser == 1L,
    status = when (status) {
        "SENT" -> MessageStatus.SENT
        "SENDING" -> MessageStatus.SENDING
        "ERROR" -> MessageStatus.ERROR
        else -> MessageStatus.SENT
    },
    timestamp = timestamp
)

@JvmName("asDomainModelChatMessage")
fun List<ChatMessage>.asDomainModel() = map { it.asDomainModel() }