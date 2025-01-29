package com.kashif.deepseek.presentation.state

import com.kashif.deepseek.data.local.service.OllamaModel
import com.kashif.deepseek.domain.model.ChatMessage
import com.kashif.deepseek.domain.model.ChatSession

data class ChatUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentSession: ChatSession? = null,
    val chatSessions: List<ChatSession> = emptyList(),
    val selectedModel: OllamaModel? = null,
    val availableModels: List<OllamaModel> = emptyList(),
    val thinkingMessage: ChatMessage? = null
)
