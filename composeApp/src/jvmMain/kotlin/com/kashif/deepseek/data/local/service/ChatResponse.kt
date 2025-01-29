package com.kashif.deepseek.data.local.service

import kotlinx.serialization.Serializable

@Serializable
data class ChatResponse(
    val model: String? = null,
    val response: String? = null,
    val done: Boolean = false
)