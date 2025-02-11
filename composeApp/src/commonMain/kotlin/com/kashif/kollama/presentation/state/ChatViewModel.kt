package com.kashif.kollama.presentation.state

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kashif.kollama.ChatMessage
import com.kashif.kollama.data.local.database.asDomainModel
import com.kashif.kollama.data.local.service.OllamaService
import com.kashif.kollama.data.local.service.toChatMessage
import com.kashif.kollama.data.model.OllamaModel
import com.kashif.kollama.domain.model.ChatMessageDomainModel
import com.kashif.kollama.domain.model.ChatSessionDomainModel
import com.kashif.kollama.domain.model.MessageStatus
import com.kashif.kollama.domain.model.toDataModel
import com.kashif.kollama.domain.interfaces.ChatRepository
import com.kashif.kollama.presentation.utils.ChatJobManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class ChatViewModel(
    private val repository: ChatRepository,
    private val ollamaService: OllamaService
) : ViewModel() {
    private val _state = MutableStateFlow(ChatUiState())
    val state = _state.asStateFlow()

    val scope = CoroutineScope(Dispatchers.Default)
    private val _messagesBySession = mutableMapOf<String, SnapshotStateList<ChatMessageDomainModel>>()
    private val sessionObservers = mutableMapOf<String, Job>()
    private val jobManager = ChatJobManager()

    val messages: SnapshotStateList<ChatMessageDomainModel>
        get() = _messagesBySession.getOrPut(state.value.currentSession?.id ?: "") {
            mutableStateListOf()
        }

    init {
        loadInitialData()
        observeSessions()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            try {
                updateState { it.copy(isLoading = true) }
                val models = ollamaService.listModels().fold(
                    onFailure = {
                        handleError("Failed to load models: ${it.message}")
                        emptyList()
                    },
                    onSuccess = { it }
                )
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

    private fun sendMessage(content: String) {
        viewModelScope.launch {
            try {
                val model = state.value.selectedModel?.model
                    ?: throw IllegalStateException("No model selected")
                val session = state.value.currentSession ?: createSession(content)
                val sessionMessages = _messagesBySession.getOrPut(session.id) { mutableStateListOf() }

                if (session.title == "New Chat") {
                    renameSession(session.id, content.take(50))
                }

                val userMessage = ChatMessageDomainModel(
                    content = content,
                    isUser = true,
                    sessionId = session.id
                )
                repository.insertMessage(userMessage.toDataModel())
                sessionMessages.add(userMessage)

                val assistantMessage = ChatMessageDomainModel(
                    content = "",
                    isUser = false,
                    sessionId = session.id,
                    status = MessageStatus.SENDING
                )
                repository.insertMessage(assistantMessage.toDataModel())
                sessionMessages.add(assistantMessage)

                val job = Job()
                val jobState = jobManager.addJob(session.id, job)

                var currentContent = ""

                viewModelScope.launch(job) {
                    try {
                        println("Starting chat with model: $model")
                        ollamaService.chat(
                            model = model,
                            messages = sessionMessages.takeLast(20).toChatMessage(),
                        ).catch { e ->
                            println("Error in chat stream: ${e.message}")
                            updateMessage(
                                assistantMessage.copy(
                                    status = MessageStatus.ERROR,
                                    content = "Error: ${e.message}"
                                ).toDataModel()
                            )
                            throw e
                        }.collect { response ->
                            println("Received response chunk: ${response.message.content}")
                            if (response.message.content.isNotBlank()) {
                                currentContent += response.message.content
                                if (!response.done) {
                                    updateMessage(
                                        assistantMessage.copy(
                                            content = currentContent,
                                            status = MessageStatus.SENDING
                                        ).toDataModel()
                                    )
                                }
                            }

                            if (response.done) {
                                println("Chat stream complete")
                                updateMessage(
                                    assistantMessage.copy(
                                        content = currentContent,
                                        status = MessageStatus.SENT
                                    ).toDataModel()
                                )
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        handleError("Failed to process message: ${e.message}")
                    } finally {
                        jobManager.removeJob(session.id)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                handleError("Failed to send message: ${e.message}")
            }
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    private suspend fun createSession(content: String): ChatSessionDomainModel {
        val newSession = ChatSessionDomainModel(
            id = Uuid.random().toString(),
            title = content.take(50).ifBlank { "New Chat" }
        )

        updateState { it.copy(currentSession = newSession) }
        repository.createSession(newSession.toDataModel())
        observeSessionMessages(newSession.id)

        return newSession
    }

    private fun observeSessionMessages(sessionId: String) {
        sessionObservers[sessionId]?.cancel()
        if (!_messagesBySession.containsKey(sessionId)) {
            _messagesBySession[sessionId] = mutableStateListOf()
        }

        sessionObservers[sessionId] = viewModelScope.launch {
            repository.getMessages(sessionId)
                .catch { e -> handleError("Failed to load messages: ${e.message}") }
                .collect { messages ->
                    _messagesBySession[sessionId]?.apply {
                        clear()
                        addAll(messages)
                    }
                }
        }
    }

    private fun updateMessage(message: ChatMessage) {
        viewModelScope.launch {
            try {
                repository.updateMessage(message)
                val sessionMessages = _messagesBySession[message.sessionId] ?: return@launch
                val index = sessionMessages.indexOfFirst { it.id == message.id }
                if (index != -1) {
                    sessionMessages[index] = message.asDomainModel()
                }
            } catch (e: Exception) {
                handleError("Failed to update message: ${e.message}")
            }
        }
    }

    private fun handleError(message: String) {
        updateState { it.copy(error = message, isLoading = false) }
    }

    private fun updateState(update: (ChatUiState) -> ChatUiState) {
        _state.value = update(_state.value)
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            jobManager.cancelAllJobs()
            sessionObservers.values.forEach { it.cancel() }
            _messagesBySession.clear()
        }
    }


    private inline fun <T> MutableMap<T, Job>.removeIf(predicate: (Map.Entry<T, Job>) -> Boolean) {
        val iter = entries.iterator()
        while (iter.hasNext()) {
            val entry = iter.next()
            if (predicate(entry)) {
                entry.value.cancel()
                iter.remove()
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
            is ChatEvent.UserEvent.ClearError -> {
                updateState { it.copy(error = null) }
            }

            ChatEvent.UserEvent.DismissModelList -> {
                updateState { it.copy(showModelList = false) }
            }

            ChatEvent.UserEvent.ShowModelList -> {
                println("Show model list event")
                updateState { it.copy(showModelList = true) }
                println("Show model list event done ${state.value.showModelList}")
            }
        }
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
                            currentSession = (updatedCurrentSession ?: it.currentSession)
                        )
                    }
                }
        }
    }

    private fun selectSession(session: ChatSessionDomainModel) {
        viewModelScope.launch {
            try {
                updateState { it.copy(currentSession = session) }
                if (!sessionObservers.containsKey(session.id)) {
                    observeSessionMessages(session.id)
                }
            } catch (e: Exception) {
                handleError("Failed to switch session: ${e.message}")
            }
        }
    }

    private fun selectModel(model: OllamaModel) {
        updateState { it.copy(selectedModel = model, showModelList = false) }
    }


    private fun processMessageChunk(
        chunk: String,
        jobState: ChatJobManager.JobState,
        assistantMessage: ChatMessageDomainModel
    ) {
        with(jobState) {
            println(chunk)
            when {
                chunk.contains("<think>") -> {
                    isInThinkingBlock = true
                    val beforeThinking = chunk.substringBefore("<think>")
                    if (beforeThinking.isNotBlank()) {
                        responseBuilder.append(beforeThinking)
                        updateMessage(
                            assistantMessage.copy(content = responseBuilder.toString())
                                .toDataModel()
                        )
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
                        updateMessage(
                            assistantMessage.copy(content = responseBuilder.toString())
                                .toDataModel()
                        )
                    }
                }

                isInThinkingBlock -> {
                    thinkingStack.add(chunk)
                    updateThinkingState(thinkingStack.joinToString(""))
                }

                else -> {
                    responseBuilder.append(chunk)
                    updateMessage(
                        assistantMessage.copy(content = responseBuilder.toString()).toDataModel()
                    )
                }
            }
        }
    }


    private fun updateThinkingState(content: String) {
        updateState {
            it.copy(
                thinkingMessage = ChatMessageDomainModel(
                    id = "thinking-${Clock.System.now().toEpochMilliseconds()}",
                    content = "\uD83E\uDD14 Thinking...$content",
                    isUser = false,
                    sessionId = "thinking",
                    status = MessageStatus.SENDING
                )
            )
        }
    }

    private fun cleanupInactiveSessions() {
        viewModelScope.launch {
            val activeSessionIds = jobManager.getActiveSessionIds()
            sessionObservers.removeIf { (sessionId, _) ->
                if (!activeSessionIds.contains(sessionId)) {
                    _messagesBySession.remove(sessionId)
                    true
                } else false
            }
        }
    }

    private fun renameSession(sessionId: String, newTitle: String) {
        viewModelScope.launch {
            try {
                val session = state.value.chatSessions.find { it.id == sessionId } ?: return@launch
                val updatedSession = session.copy(title = newTitle)
                repository.updateSession(updatedSession.toDataModel())

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

    private fun retryMessage(message: ChatMessageDomainModel) {
        if (!message.isUser && message.status == MessageStatus.ERROR) {
            sendMessage(message.content)
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun createNewChat() {
        viewModelScope.launch {
            try {
                val newSession = ChatSessionDomainModel(
                    id = Uuid.random().toString(),
                    title = "New Chat"
                )


                _messagesBySession[newSession.id] = mutableStateListOf()

                updateState {
                    it.copy(
                        currentSession = newSession,
                        thinkingMessage = null
                    )
                }

                repository.createSession(newSession.toDataModel())
                observeSessionMessages(newSession.id)

            } catch (e: Exception) {
                handleError(e.message ?: "Failed to create new chat")
            }
        }
    }
}