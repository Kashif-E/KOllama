package com.kashif.kollama.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ChatRequest(
    val model: String,
    val prompt: String,
    val stream: Boolean = true
)