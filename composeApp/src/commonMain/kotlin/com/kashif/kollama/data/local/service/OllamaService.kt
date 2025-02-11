package com.kashif.kollama.data.local.service


import com.kashif.kollama.data.local.service.model.OllamaConfig
import com.kashif.kollama.data.local.service.model.OllamaError
import com.kashif.kollama.data.model.ModelDetails
import com.kashif.kollama.data.model.OllamaModel
import com.kashif.kollama.domain.model.ChatMessageDomainModel
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSocketException
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.encodeToJsonElement
import kotlin.coroutines.cancellation.CancellationException

expect val IO: CoroutineDispatcher

expect fun getEngine(): HttpClientEngine


@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val stream: Boolean = true,
    val format: JsonObject? = null,
    val options: Map<String, JsonElement>? = null,
    val keepAlive: String? = null
)

@Serializable
data class ChatMessage(
    val role: String,
    val content: String,
    val images: List<String>? = null,
    val toolCalls: List<ToolCall>? = null
)

fun ChatMessageDomainModel.toChatMessage() = ChatMessage(
    role = if (isUser) "user" else "assistant",
    content = content
)

fun List<ChatMessageDomainModel>.toChatMessage() = map { it.toChatMessage() }

@Serializable
data class ToolCall(
    val function: FunctionCall
)

@Serializable
data class FunctionCall(
    val name: String,
    val arguments: JsonObject
)

@Serializable
data class ChatResponse(
    val model: String,
    val createdAt: String = Clock.System.now().toEpochMilliseconds().toString(),
    val message: ChatMessage,
    val done: Boolean,
    val totalDuration: Long? = null,
    val loadDuration: Long? = null,
    val promptEvalCount: Int? = null,
    val promptEvalDuration: Long? = null,
    val evalCount: Int? = null,
    val evalDuration: Long? = null
)

@Serializable
data class Model(
    val name: String,
    val modifiedAt: String,
    val size: Long,
    val digest: String,
    val details: ModelDetails? = null
)

@Serializable
data class ModelList(
    val models: List<OllamaModel>
)

@Serializable
data class GenerateRequest(
    val model: String,
    val prompt: String,
    val system: String? = null,
    val template: String? = null,
    val context: List<Int>? = null,
    val stream: Boolean = true,
    val raw: Boolean = false,
    val format: JsonObject? = null,
    val options: Map<String, JsonElement>? = null,
    val keepAlive: String? = null
)

@Serializable
data class GenerateResponse(
    val model: String,
    val createdAt: String,
    val response: String,
    val done: Boolean,
    val context: List<Int>? = null,
    val totalDuration: Long? = null,
    val loadDuration: Long? = null,
    val promptEvalCount: Int? = null,
    val promptEvalDuration: Long? = null,
    val evalCount: Int? = null,
    val evalDuration: Long? = null
)

@Serializable
data class EmbeddingRequest(
    val model: String,
    val input: JsonElement,
    val options: Map<String, JsonElement>? = null,
    val keepAlive: String? = null
)

@Serializable
data class EmbeddingResponse(
    val embeddings: List<List<Float>>,
    val totalDuration: Long? = null,
    val loadDuration: Long? = null,
    val promptEvalCount: Int? = null
)

class OllamaService(
    private val config: OllamaConfig = OllamaConfig()
) {

    // Create a supervisor job that will be used to manage all coroutines
    private val serviceJob = SupervisorJob()

    // Create a scope that uses the supervisor job and IO dispatcher
    private val serviceScope = CoroutineScope(IO + serviceJob)
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
        encodeDefaults = true
        prettyPrint = false
    }

    private val client = HttpClient(getEngine()) {
        install(WebSockets) {
        }

        install(ContentNegotiation) {
            json(json)
        }

        install(HttpTimeout) {
            requestTimeoutMillis = config.requestTimeout
            connectTimeoutMillis = config.connectionTimeout
        }

        install(HttpRequestRetry) {
            maxRetries = config.maxRetries
            retryOnServerErrors(maxRetries = config.maxRetries)
            exponentialDelay()
        }
    }

    private val baseUrl = "http://${config.host}:${config.port}"
    suspend fun listModels(): Result<List<OllamaModel>> = runCatching {
        client.get("$baseUrl/api/tags").body<ModelList>().models
    }


    fun chat(
        model: String,
        messages: List<ChatMessage>,
        options: Map<String, JsonElement>? = null,
        format: JsonObject? = null,
        keepAlive: String? = null
    ): Flow<ChatResponse> = flow {
        val request = ChatRequest(
            model = model,
            messages = messages,
            stream = true,
            format = format,
            options = options,
            keepAlive = keepAlive
        )

        println("Sending chat request: $request")

        client.post("$baseUrl/api/chat") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.bodyAsChannel().apply {
            while (!isClosedForRead) {
                val line = readUTF8Line() ?: break
                if (line.isBlank()) continue

                try {
                    val response = json.decodeFromString<ChatResponse>(line)
                    println("Decoded response: ${response.message.content}")
                    emit(response)

                    if (response.done) {
                        return@apply
                    }
                } catch (e: Exception) {
                    println("Error decoding response: ${e.message}")
                    e.printStackTrace()
                }
            }
        }

    }
        .buffer(Channel.UNLIMITED)
        .flowOn(IO)
        .catch { e ->
            println("Error during chat: ${e.message}")
            println("Error stacktrace: ${e.stackTraceToString()}")
            throw when (e) {
                is WebSocketException -> OllamaError.ConnectionError("WebSocket error", e)
                is HttpRequestTimeoutException -> OllamaError.TimeoutError("Request timeout", e)
                is ConnectTimeoutException -> OllamaError.TimeoutError("Connection timeout", e)
                is CancellationException -> throw e  // Rethrow cancellation exceptions
                else -> OllamaError.ConnectionError("Network error: ${e.message}", e)
            }
        }


    fun generate(
        model: String,
        prompt: String,
        system: String? = null,
        template: String? = null,
        context: List<Int>? = null,
        options: Map<String, JsonElement>? = null,
        format: JsonObject? = null,
        raw: Boolean = false,
        keepAlive: String? = null
    ): Flow<GenerateResponse> = flow {
        val request = GenerateRequest(
            model = model,
            prompt = prompt,
            system = system,
            template = template,
            context = context,
            stream = true,
            raw = raw,
            format = format,
            options = options,
            keepAlive = keepAlive
        )

        client.post("$baseUrl/api/generate") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.bodyAsChannel().apply {
            var buffer = StringBuilder(config.bufferSize)

            while (!isClosedForRead) {
                val line = readUTF8Line() ?: break
                if (line.isBlank()) continue

                try {
                    json.decodeFromString<GenerateResponse>(line).let {
                        emit(it)
                        if (it.done) {
                            buffer.clear()
                            return@apply
                        }
                    }
                } catch (e: Exception) {
                    buffer.append(line)
                    try {
                        json.decodeFromString<GenerateResponse>(buffer.toString()).let {
                            emit(it)
                            if (it.done) {
                                buffer.clear()
                                return@apply
                            }
                            buffer.clear()
                        }
                    } catch (e: Exception) {
                        // Continue accumulating chunks
                    }
                }
            }
        }
    }
        .buffer(Channel.UNLIMITED)
        .flowOn(IO)
        .catch { e ->
            throw when (e) {
                is HttpRequestTimeoutException -> OllamaError.TimeoutError("Request timeout", e)
                is ConnectTimeoutException -> OllamaError.TimeoutError("Connection timeout", e)
                else -> OllamaError.ConnectionError("Network error: ${e.message}", e)
            }
        }

    suspend fun embed(
        model: String,
        input: Any,
        options: Map<String, JsonElement>? = null,
        keepAlive: String? = null
    ): EmbeddingResponse {
        val inputJson = when (input) {
            is String -> JsonPrimitive(input)
            is List<*> -> json.encodeToJsonElement(input)
            else -> throw IllegalArgumentException("Input must be String or List<String>")
        }

        val request = EmbeddingRequest(
            model = model,
            input = inputJson,
            options = options,
            keepAlive = keepAlive
        )

        return client.post("$baseUrl/api/embed") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }


}

