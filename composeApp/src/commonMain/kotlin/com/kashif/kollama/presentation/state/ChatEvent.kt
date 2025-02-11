package com.kashif.kollama.presentation.state

import com.kashif.kollama.data.model.OllamaModel
import com.kashif.kollama.ChatMessage
import com.kashif.kollama.ChatSession
import com.kashif.kollama.domain.model.ChatMessageDomainModel
import com.kashif.kollama.domain.model.ChatSessionDomainModel

sealed interface ChatEvent {
    sealed interface UserEvent : ChatEvent {
        data class SendMessage(val content: String) : UserEvent
        data class SelectSession(val session: ChatSessionDomainModel) : UserEvent
        data class SelectModel(val model: OllamaModel) : UserEvent
        data class RetryMessage(val message: ChatMessageDomainModel) : UserEvent
        data object CreateNewChat : UserEvent
        data object ClearError : UserEvent
        data object DismissModelList : ChatEvent
        data object ShowModelList : ChatEvent
    }
}