package com.kashif.deepseek.data.model

import com.kashif.deepseek.data.local.service.OllamaModel
import kotlinx.serialization.Serializable


@Serializable
data class OllamaModelsResponse(
    val models: List<OllamaModel>
)
