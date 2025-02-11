package com.kashif.kollama.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ChatResponse(
    val model: String? = null,
    val response: String? = null,
    val done: Boolean = false
)