package com.kashif.deepseek.data.local.service


import com.kashif.deepseek.data.model.OllamaModelsResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.IOException
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.charset.StandardCharsets

/**
 * Configuration class for OllamaService
 */
data class OllamaConfig(
    val host: String = "localhost",
    val port: Int = 11434,
    val connectionTimeout: Int = 10000,
    val readTimeout: Int = 30000,
    val maxContentLength: Int = 10 * 1024 * 1024,
    val maxRetries: Int = 3,
    val bufferSize: Int = 8192
)

/**
 * Sealed class hierarchy for OllamaService specific errors
 */
sealed class OllamaError : Exception() {
    data class ConnectionError(
        override val message: String,
        override val cause: Throwable? = null
    ) : OllamaError()

    data class ResponseError(override val message: String, override val cause: Throwable? = null) :
        OllamaError()

    data class TimeoutError(override val message: String, override val cause: Throwable? = null) :
        OllamaError()

    data class ContentTooLargeError(override val message: String) : OllamaError()
}

/**
 * Improved OllamaService implementation with better error handling, resource management,
 * and network resilience
 */
class OllamaService(
    private val config: OllamaConfig = OllamaConfig()
) {
    private val logger = { it: String -> println(it) }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    /**
     * Creates a configured socket with timeout settings
     */
    private fun createSocket(): Socket = Socket().apply {
        soTimeout = config.readTimeout
        connect(InetSocketAddress(config.host, config.port), config.connectionTimeout)
    }

    /**
     * Builds an HTTP request with the specified parameters
     */
    private fun buildHttpRequest(
        method: String,
        path: String,
        body: String? = null,
        headers: Map<String, String> = emptyMap()
    ): String = buildString {
        append("$method $path HTTP/1.1\r\n")
        append("Host: ${config.host}\r\n")

        if (body != null) {
            append("Content-Type: application/json\r\n")
            append("Content-Length: ${body.toByteArray(StandardCharsets.UTF_8).size}\r\n")
        }

        headers.forEach { (key, value) ->
            append("$key: $value\r\n")
        }

        append("\r\n")
        if (body != null) {
            append(body)
        }
    }

    /**
     * Extracts response body with proper content length handling and memory limits
     */
    private fun extractResponseBody(reader: BufferedReader): String {
        var contentLength = 0


        while (true) {
            val line = reader.readLine() ?: break
            if (line.isEmpty()) break

            if (line.lowercase().startsWith("content-length:")) {
                contentLength = line.substring(15).trim().toInt().also {
                    if (it > config.maxContentLength) {
                        throw OllamaError.ContentTooLargeError("Response size $it exceeds limit ${config.maxContentLength}")
                    }
                }
            }
        }

        return when {
            contentLength > 0 -> reader.use {
                CharArray(contentLength).also { buffer ->
                    var bytesRead = 0
                    while (bytesRead < contentLength) {
                        val count = reader.read(buffer, bytesRead, contentLength - bytesRead)
                        if (count == -1) break
                        bytesRead += count
                    }
                }.concatToString()
            }

            else -> reader.readText()
        }
    }

    /**
     * Processes streaming responses from the Ollama server
     */
    private suspend fun processResponse(
        reader: BufferedReader,
        onResponse: suspend (String) -> Unit
    ) {
        var line: String?
        val responseBuffer = StringBuilder()


        while (true) {
            line = withContext(Dispatchers.IO) {
                reader.readLine()
            }
            if (line.isNullOrEmpty()) break
        }


        while (true) {
            line = withContext(Dispatchers.IO) {
                reader.readLine()
            }
            if (line == null) break

            if (line.isNotBlank()) {
                try {

                    if (line.matches("[0-9a-fA-F]+".toRegex())) {
                        val decodedBytes = line.chunked(2)
                            .map { it.toInt(16).toByte() }
                            .toByteArray()
                        val decodedString = String(decodedBytes, Charsets.UTF_8)
                        responseBuffer.append(decodedString)


                        val bufferContent = responseBuffer.toString()
                        var jsonStart = bufferContent.indexOf('{')
                        var jsonEnd = bufferContent.indexOf('}', jsonStart)

                        while (jsonStart >= 0 && jsonEnd >= 0) {
                            val jsonStr = bufferContent.substring(jsonStart, jsonEnd + 1)
                            try {
                                val response =
                                    json.decodeFromString(ChatResponse.serializer(), jsonStr)
                                response.response?.takeIf { it.isNotBlank() }?.let {
                                    onResponse(it)
                                }
                                if (response.done) return


                                responseBuffer.delete(0, jsonEnd + 1)


                                val remainingContent = responseBuffer.toString()
                                jsonStart = remainingContent.indexOf('{')
                                jsonEnd = if (jsonStart >= 0) remainingContent.indexOf(
                                    '}',
                                    jsonStart
                                ) else -1
                            } catch (e: Exception) {

                                break
                            }
                        }
                    } else {

                        val response = json.decodeFromString(ChatResponse.serializer(), line)
                        response.response?.takeIf { it.isNotBlank() }?.let {
                            onResponse(it)
                        }
                        if (response.done) break
                    }
                } catch (e: Exception) {
                    logger.invoke("Failed to process response chunk: ${e.message}")
                    continue
                }
            }
        }
    }

    /**
     * Executes a network request with retry logic
     */
    private suspend fun <T> executeWithRetry(
        operation: String,
        block: suspend () -> T
    ): T {
        var retryCount = 0
        var lastException: Exception? = null

        while (retryCount < config.maxRetries) {
            try {
                return block()
            } catch (e: Exception) {
                lastException = e
                logger("Failed to execute $operation (attempt ${retryCount + 1}/${config.maxRetries}): ${e.message}")
                if (++retryCount < config.maxRetries) {
                    delay(1000L * (1 shl retryCount))
                }
            }
        }

        throw OllamaError.ConnectionError(
            "Failed to execute $operation after ${config.maxRetries} attempts",
            lastException
        )
    }

    /**
     * Initiates a chat session with the specified model
     */
    fun chat(model: String, prompt: String): Flow<String> = flow {
        executeWithRetry("chat") {
            createSocket().use { socket ->
                val writer = PrintWriter(socket.getOutputStream().buffered())
                val reader = BufferedReader(socket.getInputStream().reader())

                val chatRequest = ChatRequest(model, prompt)
                val requestBody = json.encodeToString(ChatRequest.serializer(), chatRequest)

                val request = buildHttpRequest(
                    method = "POST",
                    path = "/api/generate",
                    body = requestBody
                )

                logger("Sending chat request for model: $model")
                writer.print(request)
                writer.flush()

                processResponse(reader) { response ->
                    emit(response)
                }
            }
        }
    }
        .buffer(Channel.BUFFERED)
        .flowOn(Dispatchers.IO)
        .catch { e ->
            logger("Error in chat flow")
            throw when (e) {
                is IOException -> OllamaError.ConnectionError("Network error during chat", e)
                else -> e
            }
        }

    /**
     * Lists available models
     */
    suspend fun listModels(): Result<List<OllamaModel>> = withContext(Dispatchers.IO) {
        runCatching {
            executeWithRetry("list models") {
                createSocket().use { socket ->
                    val writer = PrintWriter(socket.getOutputStream().buffered())
                    val reader = BufferedReader(socket.getInputStream().reader())

                    val request = buildHttpRequest(
                        method = "GET",
                        path = "/api/tags"
                    )

                    writer.print(request)
                    writer.flush()

                    val response = extractResponseBody(reader)
                    json.decodeFromString(OllamaModelsResponse.serializer(), response).models
                }
            }
        }
    }

    /**
     * Pulls a model from the Ollama server
     */
    suspend fun pullModel(modelName: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            executeWithRetry("pull model") {
                createSocket().use { socket ->
                    val writer = PrintWriter(socket.getOutputStream().buffered())
                    val reader = BufferedReader(socket.getInputStream().reader())

                    val requestBody = """{"name": "$modelName"}"""
                    val request = buildHttpRequest(
                        method = "POST",
                        path = "/api/pull",
                        body = requestBody
                    )

                    writer.print(request)
                    writer.flush()

                    val response = extractResponseBody(reader)
                    if (!response.contains("\"status\":\"success\"")) {
                        throw OllamaError.ResponseError("Failed to pull model: $response")
                    }
                }
            }
        }
    }

    /**
     * Removes a model from the Ollama server
     */
    suspend fun removeModel(modelName: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            executeWithRetry("remove model") {
                createSocket().use { socket ->
                    val writer = PrintWriter(socket.getOutputStream().buffered())
                    val reader = BufferedReader(socket.getInputStream().reader())

                    val requestBody = """{"name": "$modelName"}"""
                    val request = buildHttpRequest(
                        method = "DELETE",
                        path = "/api/delete",
                        body = requestBody
                    )

                    writer.print(request)
                    writer.flush()

                    val response = extractResponseBody(reader)
                    if (!response.contains("\"status\":\"success\"")) {
                        throw OllamaError.ResponseError("Failed to remove model: $response")
                    }
                }
            }
        }
    }

    /**
     * Retrieves information about a specific model
     */
    suspend fun showModelInfo(modelName: String): Result<OllamaModel> =
        withContext(Dispatchers.IO) {
            runCatching {
                executeWithRetry("show model info") {
                    createSocket().use { socket ->
                        val writer = PrintWriter(socket.getOutputStream().buffered())
                        val reader = BufferedReader(socket.getInputStream().reader())

                        val request = buildHttpRequest(
                            method = "GET",
                            path = "/api/show?name=$modelName"
                        )

                        writer.print(request)
                        writer.flush()

                        val response = extractResponseBody(reader)
                        json.decodeFromString(OllamaModel.serializer(), response)
                    }
                }
        }
    }
}