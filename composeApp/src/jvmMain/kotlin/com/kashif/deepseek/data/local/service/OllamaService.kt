package com.kashif.deepseek.data.local.service


import com.kashif.deepseek.data.model.OllamaModelsResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
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
    val connectionTimeout: Int = 30000,  // 30 seconds for connection
    val readTimeout: Int = 120000,       // 2 minutes for read operations
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
    private val logger = { it: String -> println("[OllamaService] $it") }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    private data class SocketConnection(
        var socket: Socket? = null,
        var writer: PrintWriter? = null,
        var reader: BufferedReader? = null,
        var session: ChatSession = ChatSession()
    ) {
        override fun toString(): String =
            "Socket[connected=${socket?.isConnected}, closed=${socket?.isClosed}]"
    }

    private val chatSessions = mutableMapOf<String, SocketConnection>()

    /**
     * Creates a configured socket with timeout settings
     */
    private fun createSocket(): Socket = Socket().apply {
        soTimeout = config.readTimeout
        keepAlive = true
        tcpNoDelay = true
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
        var isFirstResponse = true
        var isDone = false

        while (true) {
            line = withContext(Dispatchers.IO) { reader.readLine() }
            if (line == null || line.isEmpty()) break
            logger("Read header line: $line")
        }

        logger("Headers completed, reading response body...")

        try {
            while (!isDone) {
                line = withContext(Dispatchers.IO) { reader.readLine() }
                if (line == null) break

                if (line.isBlank()) continue

                if (line.matches(Regex("[0-9a-fA-F]+")) && line != "0") {
                    val chunkSize = line.toInt(16)
                    if (chunkSize > 0) {
                        val chunk = CharArray(chunkSize)
                        var bytesRead = 0
                        while (bytesRead < chunkSize) {
                            val count = withContext(Dispatchers.IO) {
                                reader.read(chunk, bytesRead, chunkSize - bytesRead)
                            }
                            if (count == -1) break
                            bytesRead += count
                        }
                        val currentChunk = String(chunk)

                        withContext(Dispatchers.IO) { reader.readLine() }

                        try {
                            if (currentChunk.startsWith("{")) {
                                val response = json.decodeFromString<ChatResponse>(currentChunk)

                                if (isFirstResponse) {
                                    onResponse("</think>")
                                    isFirstResponse = false
                                }

                                val content = response.message?.content
                                if (!content.isNullOrBlank()) {
                                    onResponse(content)
                                }

                                if (response.done) {
                                    isDone = true
                                    break
                                }
                            }
                        } catch (e: Exception) {
                            logger("Error processing chunk: ${e.message}")
                        }
                    }
                } else if (line == "0") {
                    break
                }
            }

            while (reader.ready()) {
                reader.read()
            }

        } catch (e: Exception) {
            logger("Error reading response: ${e.message}")
            throw e
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
    fun chat(model: String, prompt: String, sessionId: String): Flow<String> = flow {
        executeWithRetry("chat") {
            val connection = chatSessions.getOrPut(sessionId) {
                logger("Creating new connection for session: $sessionId")
                SocketConnection()
            }

            try {
                if (connection.socket == null || connection.socket?.isClosed == true || !connection.socket?.isConnected!!) {
                    logger("Connection needs reset for session $sessionId: $connection")
                    val oldSession = connection.session
                    closeConnection(sessionId, clearHistory = false)
                    connection.socket = createSocket()
                    connection.writer =
                        PrintWriter(connection.socket!!.getOutputStream().buffered())
                    connection.reader =
                        BufferedReader(connection.socket!!.getInputStream().reader(Charsets.UTF_8))
                    connection.session = oldSession
                    logger("New connection established: $connection")
                } else {
                    logger("Reusing existing connection: $connection")
                }

                val newMessage = Message(role = "user", content = prompt)
                connection.session.messages.add(newMessage)

                val chatRequest = ChatRequest(
                    model = model,
                    messages = connection.session.messages.toList(),
                    stream = true,
                    context = connection.session.context
                )
                val requestBody = json.encodeToString(ChatRequest.serializer(), chatRequest)
                logger("Prepared request body with ${connection.session.messages.size} messages...")

                val request = buildHttpRequest(
                    method = "POST",
                    path = "/api/chat",
                    body = requestBody,
                    headers = mapOf(
                        "Accept" to "application/x-ndjson",
                        "Content-Type" to "application/json"
                    )
                )

                logger("Sending chat request for model: $model in session: $sessionId")
                connection.writer?.print(request)
                connection.writer?.flush()

                emit("<think>Thinking...</think>")

                var assistantMessage = Message(role = "assistant", content = "")
                processResponse(connection.reader!!) { response ->
                    emit(response)
                    assistantMessage =
                        assistantMessage.copy(content = assistantMessage.content + response)
                }

                connection.session.messages.add(assistantMessage)
                connection.session.context = chatRequest.context

                logger("Chat request completed successfully")
            } catch (e: Exception) {
                logger("Error during chat request: ${e.message}")
                closeConnection(sessionId)
                throw e
            }
        }
    }
        .buffer(Channel.BUFFERED)
        .flowOn(Dispatchers.IO)
        .catch { e ->
            logger("Error in chat flow for session: $sessionId - ${e.message}")
            closeConnection(sessionId)
            throw when (e) {
                is IOException -> OllamaError.ConnectionError(
                    "Network error during chat: ${e.message}",
                    e
                )

                else -> e
            }
        }

    private fun closeConnection(sessionId: String, clearHistory: Boolean = true) {
        try {
            logger("Closing connection for session: $sessionId")
            chatSessions[sessionId]?.let { connection ->
                try {
                    connection.reader?.let { reader ->
                        while (reader.ready()) {
                            reader.read()
                        }
                    }
                } catch (e: Exception) {
                    logger("Error consuming remaining data: ${e.message}")
                }
                connection.writer?.close()
                connection.reader?.close()
                connection.socket?.close()
                if (clearHistory) {
                    chatSessions.remove(sessionId)
                    logger("Connection closed and removed from sessions")
                } else {
                    connection.socket = null
                    connection.writer = null
                    connection.reader = null
                    logger("Connection closed but keeping session history")
                }
            }
        } catch (e: Exception) {
            logger("Error closing connection for session $sessionId: ${e.message}")
        }
    }

    fun closeAllConnections() {
        chatSessions.keys.toList().forEach { sessionId ->
            closeConnection(sessionId)
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