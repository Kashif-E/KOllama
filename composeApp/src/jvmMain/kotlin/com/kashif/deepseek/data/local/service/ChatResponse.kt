package com.kashif.deepseek.data.local.service

import kotlinx.serialization.Serializable

@Serializable
data class ChatResponse(
    val message: Message? = null,
    val done: Boolean = false,
)
