package com.kashif.kollama.data.local.service.model

import kotlinx.serialization.Serializable


@Serializable
data class ModelRequest(
    val name: String
)

@Serializable
data class ModelResponse(
    val status: String
)