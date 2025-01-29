package com.kashif.deepseek.data.local.service

import kotlinx.serialization.Serializable

@Serializable
data class ChatRequest(
    val model: String,
    val prompt: String,
    val stream: Boolean = true
)