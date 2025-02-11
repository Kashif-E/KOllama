package com.kashif.kollama.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ModelDetails(
    @SerialName("parent_model")
    val parentModel: String,
    val format: String,
    val family: String,
    val families: List<String>?,
    @SerialName("parameter_size")
    val parameterSize: String,
    @SerialName("quantization_level")
    val quantizationLevel: String
)