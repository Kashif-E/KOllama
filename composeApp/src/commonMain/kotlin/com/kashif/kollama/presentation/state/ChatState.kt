package com.kashif.kollama.presentation.state

import com.kashif.kollama.data.model.OllamaModel
import com.kashif.kollama.ChatMessage
import com.kashif.kollama.ChatSession
import com.kashif.kollama.domain.model.ChatMessageDomainModel
import com.kashif.kollama.domain.model.ChatSessionDomainModel

data class ChatUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentSession: ChatSessionDomainModel? = null,
    val chatSessions: List<ChatSessionDomainModel> = emptyList(),
    val selectedModel: OllamaModel? = null,
    val availableModels: List<OllamaModel> = emptyList(),
    val thinkingMessage: ChatMessageDomainModel? = null,
    val showModelList: Boolean = false,
) {

}
