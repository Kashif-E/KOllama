package com.kashif.deepseek.presentation.state

import com.kashif.deepseek.data.local.service.OllamaModel
import com.kashif.deepseek.domain.model.ChatMessage
import com.kashif.deepseek.domain.model.ChatSession

sealed interface ChatEvent {
    sealed interface UserEvent : ChatEvent {
        data class SendMessage(val content: String) : UserEvent
        data class SelectSession(val session: ChatSession) : UserEvent
        data class SelectModel(val model: OllamaModel) : UserEvent
        data class RetryMessage(val message: ChatMessage) : UserEvent
        data object CreateNewChat : UserEvent
        data object ClearError : UserEvent
    }
}