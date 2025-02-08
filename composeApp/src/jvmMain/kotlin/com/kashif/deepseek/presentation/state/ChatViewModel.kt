package com.kashif.deepseek.presentation.state

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kashif.deepseek.data.local.service.OllamaModel
import com.kashif.deepseek.data.local.service.OllamaService
import com.kashif.deepseek.domain.model.ChatMessage
import com.kashif.deepseek.domain.model.ChatSession
import com.kashif.deepseek.domain.model.MessageStatus
import com.kashif.deepseek.domain.repository.ChatRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.util.UUID

class ChatViewModel(
    private val repository: ChatRepository,
    private val ollamaService: OllamaService
) : ViewModel() {

    private val _state = MutableStateFlow(ChatUiState())
    val state = _state.asStateFlow()

    private val _messages = mutableStateListOf<ChatMessage>()
    val messages: SnapshotStateList<ChatMessage> = _messages
    private var currentSessionObservationJob: Job? = null

    init {
        loadInitialData()
        observeSessions()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            try {
                updateState { it.copy(isLoading = true) }
                val models = ollamaService.listModels().fold(onFailure = {
                    emptyList()
                }, onSuccess = {
                    it
                })
                updateState {
                    it.copy(
                        availableModels = models,
                        selectedModel = models.firstOrNull(),
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                handleError("Failed to load models: ${e.message}")
            }
        }
    }

    fun handleEvent(event: ChatEvent) {
        when (event) {
            is ChatEvent.UserEvent.SendMessage -> sendMessage(event.content)
            is ChatEvent.UserEvent.SelectSession -> selectSession(event.session)
            is ChatEvent.UserEvent.SelectModel -> selectModel(event.model)
            is ChatEvent.UserEvent.CreateNewChat -> createNewChat()
            is ChatEvent.UserEvent.RetryMessage -> retryMessage(event.message)
            is ChatEvent.UserEvent.ClearError -> clearError()
        }
    }

    private fun sendMessage(content: String) {
        viewModelScope.launch {
            try {
                val model = state.value.selectedModel?.model
                    ?: throw IllegalStateException("No model selected")
                val session = state.value.currentSession ?: createSession(content)

                if (session.title == "New Chat") {
                    renameSession(session.id, content)
                }

                val userMessage = ChatMessage(
                    content = content,
                    isUser = true,
                    sessionId = session.id
                )
                repository.insertMessage(userMessage)
                _messages.add(userMessage)

                val assistantMessage = ChatMessage(
                    content = "",
                    isUser = false,
                    sessionId = session.id,
                    status = MessageStatus.SENDING
                )
                repository.insertMessage(assistantMessage)
                _messages.add(assistantMessage)

                val responseBuilder = StringBuilder()
                val thinkingStack = mutableListOf<String>()
                var isInThinkingBlock = false

                ollamaService.chat(model, content, session.id)
                    .catch { e ->
                        updateMessage(assistantMessage.copy(status = MessageStatus.ERROR))
                        throw e
                    }
                    .collect { chunk ->
                        when {
                            chunk.contains("<think>") -> {
                                isInThinkingBlock = true
                                val beforeThinking = chunk.substringBefore("<think>")
                                if (beforeThinking.isNotBlank()) {
                                    responseBuilder.append(beforeThinking)
                                    updateMessage(assistantMessage.copy(content = responseBuilder.toString()))
                                }

                                val thinkingContent = chunk.substringAfter("<think>")
                                if (!chunk.contains("</think>")) {
                                    thinkingStack.add(thinkingContent)
                                    updateThinkingState(thinkingStack.joinToString(""))
                                }
                            }

                            chunk.contains("</think>") -> {
                                val thinkingContent = chunk.substringBefore("</think>")
                                if (thinkingContent.isNotBlank()) {
                                    thinkingStack.add(thinkingContent)
                                    updateThinkingState(thinkingStack.joinToString(""))
                                }

                                isInThinkingBlock = false
                                thinkingStack.clear()
                                updateState { it.copy(thinkingMessage = null) }

                                val afterThinking = chunk.substringAfter("</think>")
                                if (afterThinking.isNotBlank()) {
                                    responseBuilder.append(afterThinking)
                                    updateMessage(assistantMessage.copy(content = responseBuilder.toString()))
                                }
                            }

                            isInThinkingBlock -> {
                                thinkingStack.add(chunk)
                                updateThinkingState(thinkingStack.joinToString(""))
                            }

                            else -> {
                                responseBuilder.append(chunk)
                                updateMessage(
                                    assistantMessage.copy(
                                        content = responseBuilder.toString()
                                    )
                                )
                            }
                        }
                    }

                updateMessage(
                    assistantMessage.copy(
                        content = responseBuilder.toString(),
                        status = MessageStatus.SENT
                    )
                )

            } catch (e: Exception) {
                handleError("Failed to send message: ${e.message}")
            }
        }
    }

    private fun selectModel(model: OllamaModel) {
        updateState { it.copy(selectedModel = model) }
    }


    private fun updateThinkingState(content: String) {


        updateState {
            it.copy(
                thinkingMessage = ChatMessage(
                    id = "thinking-${System.currentTimeMillis()}",
                    content = "\uD83E\uDD14 Thinking...$content",
                    isUser = false,
                    sessionId = "thinking",
                    status = MessageStatus.SENDING
                )
            )
        }
    }

    private fun updateMessage(message: ChatMessage) {
        val index = _messages.indexOfFirst { it.id == message.id }
        if (index != -1) {
            _messages[index] = message
            viewModelScope.launch {
                repository.updateMessage(message)
            }
        }
    }

    private fun clearError() {
        updateState { it.copy(error = null) }
    }

    private fun handleError(message: String) {
        updateState { it.copy(error = message, isLoading = false) }
    }

    private fun updateState(update: (ChatUiState) -> ChatUiState) {
        _state.value = update(_state.value)
    }


    private fun observeSessions() {
        viewModelScope.launch {
            repository.getAllSessions()
                .catch { e -> handleError(e.message ?: "Failed to load sessions") }
                .collect { sessions ->
                    val currentSessionId = state.value.currentSession?.id

                    val updatedCurrentSession = sessions.find { it.id == currentSessionId }
                    updateState {
                        it.copy(
                            chatSessions = sessions,
                            currentSession = updatedCurrentSession ?: it.currentSession
                        )
                    }
                }
        }
    }

    private fun observeSessionMessages(sessionId: String) {

        currentSessionObservationJob?.cancel()

        currentSessionObservationJob = viewModelScope.launch {
            repository.getMessages(sessionId)
                .catch { e -> handleError(e.message ?: "Failed to load messages") }
                .collect { messages ->
                    _messages.clear()
                    _messages.addAll(messages)
                }
        }
    }

    private fun selectSession(session: ChatSession) {
        updateState { it.copy(currentSession = session) }
        observeSessionMessages(session.id)
    }

    private fun createNewChat() {
        viewModelScope.launch {
            try {
                currentSessionObservationJob?.cancel()
                _messages.clear()
                val newSession = ChatSession(
                    id = UUID.randomUUID().toString(),
                    title = "New Chat"
                )


                updateState {
                    it.copy(
                        currentSession = newSession,
                        thinkingMessage = null
                    )
                }


                repository.createSession(newSession)


                observeSessionMessages(newSession.id)

            } catch (e: Exception) {
                handleError(e.message ?: "Failed to create new chat")
            }
        }
    }

    private suspend fun createSession(content: String): ChatSession {
        try {
            val newSession = ChatSession(
                id = UUID.randomUUID().toString(),
                title = if (content.length > 20) content.substring(0..20) else "New Chat"
            )


            updateState {
                it.copy(
                    currentSession = newSession
                )
            }


            repository.createSession(newSession)


            observeSessionMessages(newSession.id)

            return newSession
        } catch (e: Exception) {
            updateState { it.copy(thinkingMessage = null) }
            throw e
        }
    }



    private fun renameSession(sessionId: String, newTitle: String) {
        viewModelScope.launch {
            try {
                val session =
                    state.value.chatSessions.find { it.id == sessionId } ?: return@launch
                val updatedSession = session.copy(title = newTitle)

                repository.updateSession(updatedSession)

                updateState {
                    it.copy(
                        currentSession = if (it.currentSession?.id == sessionId) updatedSession else it.currentSession,
                        chatSessions = it.chatSessions.map { s -> if (s.id == sessionId) updatedSession else s }
                    )
                }
            } catch (e: Exception) {
                handleError(e.message ?: "Failed to rename chat")
            }
        }
    }


    private fun retryMessage(message: ChatMessage) {
        if (!message.isUser && message.status == MessageStatus.ERROR) {
            sendMessage(message.content)
        }
    }


}