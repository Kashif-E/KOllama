package com.kashif.kollama.data.model

import kotlinx.serialization.Serializable


@Serializable
data class OllamaModelsResponse(
    val models: List<OllamaModel>
)
