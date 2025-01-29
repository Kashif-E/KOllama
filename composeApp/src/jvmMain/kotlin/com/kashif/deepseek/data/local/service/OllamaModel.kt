package com.kashif.deepseek.data.local.service

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OllamaModel(
    val name: String,
    val model: String,
    @SerialName("modified_at")
    val modifiedAt: Instant,
    val size: Long,
    val digest: String,
    val details: ModelDetails
)