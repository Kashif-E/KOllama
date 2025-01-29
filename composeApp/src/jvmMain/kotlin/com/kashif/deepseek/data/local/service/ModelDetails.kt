package com.kashif.deepseek.data.local.service

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class ModelDetails(
    @SerialName("parent_model")
    val parentModel: String,
    val format: String,
    val family: String,
    val families: List<String>,
    @SerialName("parameter_size")
    val parameterSize: String,
    @SerialName("quantization_level")
    val quantizationLevel: String
)