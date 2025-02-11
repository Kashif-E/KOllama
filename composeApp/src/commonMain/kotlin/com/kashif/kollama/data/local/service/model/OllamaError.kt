package com.kashif.kollama.data.local.service.model

/**
 * Sealed hierarchy for Ollama-specific errors
 */
sealed class OllamaError : Exception() {
    class ConnectionError(override val message: String, override val cause: Throwable? = null) :
        OllamaError()

    class ResponseError(override val message: String, override val cause: Throwable? = null) :
        OllamaError()

    class TimeoutError(override val message: String, override val cause: Throwable? = null) :
        OllamaError()

    class ContentTooLargeError(override val message: String) : OllamaError()
}

