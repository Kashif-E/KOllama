package com.kashif.deepseek.data.local.service

import kotlinx.serialization.Serializable

@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<Message>,
    val stream: Boolean = true,
    val context: List<Int>? = null
)

@Serializable
data class Message(
    val role: String,
    val content: String,
)




