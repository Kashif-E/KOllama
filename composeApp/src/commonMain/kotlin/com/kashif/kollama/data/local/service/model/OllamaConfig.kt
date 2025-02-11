package com.kashif.kollama.data.local.service.model

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.Serializable

/**
 * Configuration for OllamaService with KMP-compatible defaults
 */
@Serializable
data class OllamaConfig(
    val baseUrl: String = "http://localhost:11434",
    val host: String = "localhost",
    val port: Int = 11434,
    val connectionTimeout: Long = 200_000,
    val requestTimeout: Long = 200_000,
    val maxContentLength: Long = 20L * 1024 * 1024,
    val maxRetries: Int = 3,
    val bufferSize: Int = 8192,
    val defaultModel: String = "gpt2",
    val scope: CoroutineScope = CoroutineScope(Dispatchers.Default+ SupervisorJob())
)
