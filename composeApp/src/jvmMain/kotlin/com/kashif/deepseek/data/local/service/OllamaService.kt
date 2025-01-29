package com.kashif.deepseek.data.local.service


import com.kashif.deepseek.data.model.OllamaModelsResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.PrintWriter
import java.net.Socket
import java.nio.charset.StandardCharsets

class OllamaService(
    private val host: String = "localhost",
    private val port: Int = 11434
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    private fun createSocket(): Socket = Socket(host, port)

    private fun buildHttpRequest(
        method: String,
        path: String,
        body: String? = null,
        headers: Map<String, String> = emptyMap()
    ): String {
        val requestBuilder = StringBuilder()
        requestBuilder.append("$method $path HTTP/1.1\r\n")
        requestBuilder.append("Host: $host\r\n")

        if (body != null) {
            requestBuilder.append("Content-Type: application/json\r\n")
            requestBuilder.append("Content-Length: ${body.toByteArray(StandardCharsets.UTF_8).size}\r\n")
        }

        headers.forEach { (key, value) ->
            requestBuilder.append("$key: $value\r\n")
        }

        requestBuilder.append("\r\n")
        if (body != null) {
            requestBuilder.append(body)
        }

        return requestBuilder.toString()
    }

    private fun extractResponseBody(reader: BufferedReader): String {
        var line: String?
        var contentLength = 0

        // Read headers
        while (true) {
            line = reader.readLine()
            if (line == null || line.isEmpty()) break

            if (line.lowercase().startsWith("content-length:")) {
                contentLength = line.substring(15).trim().toInt()
            }
        }

        // Read body
        if (contentLength > 0) {
            val buffer = CharArray(contentLength)
            reader.read(buffer, 0, contentLength)
            return String(buffer)
        }

        // For chunked or unspecified length, read until connection closes
        return buildString {
            while (true) {
                val char = reader.read()
                if (char == -1) break
                append(char.toChar())
            }
        }
    }

    fun chat(model: String, prompt: String): Flow<String> = flow {
        createSocket().use { socket ->
            val writer = PrintWriter(socket.getOutputStream())
            val reader = BufferedReader(socket.getInputStream().reader())

            val chatRequest = ChatRequest(model, prompt)
            val requestBody = json.encodeToString(ChatRequest.serializer(), chatRequest)

            val request = buildHttpRequest(
                method = "POST",
                path = "/api/generate",
                body = requestBody
            )

            writer.print(request)
            writer.flush()

            // Skip HTTP headers
            var line: String?
            while (true) {
                line = reader.readLine()
                if (line == null || line.isEmpty()) break
            }

            // Read and emit streaming response
            while (true) {
                line = reader.readLine()
                if (line == null) break

                if (line.isNotBlank()) {
                    try {
                        val response = json.decodeFromString(ChatResponse.serializer(), line)
                        response.response?.let {
                            if (it.isNotBlank()) emit(it)
                        }
                        if (response.done) break
                    } catch (e: Exception) {
                        // Skip malformed JSON
                        continue
                    }
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    suspend fun listModels(): List<OllamaModel> = withContext(Dispatchers.IO) {
        createSocket().use { socket ->
            val writer = PrintWriter(socket.getOutputStream())
            val reader = BufferedReader(socket.getInputStream().reader())

            val request = buildHttpRequest(
                method = "GET",
                path = "/api/tags"
            )

            writer.print(request)
            writer.flush()

            val response = extractResponseBody(reader)
            try {
                val modelsResponse =
                    json.decodeFromString(OllamaModelsResponse.serializer(), response)
                modelsResponse.models
            } catch (e: Exception) {
                println("Error decoding response: ${e.message}")
                emptyList()
            }
        }
    }

    suspend fun pullModel(modelName: String) = withContext(Dispatchers.IO) {
        createSocket().use { socket ->
            val writer = PrintWriter(socket.getOutputStream())

            val requestBody = """{"name": "$modelName"}"""
            val request = buildHttpRequest(
                method = "POST",
                path = "/api/pull",
                body = requestBody
            )

            writer.print(request)
            writer.flush()
        }
    }

    suspend fun removeModel(modelName: String) = withContext(Dispatchers.IO) {
        createSocket().use { socket ->
            val writer = PrintWriter(socket.getOutputStream())

            val requestBody = """{"name": "$modelName"}"""
            val request = buildHttpRequest(
                method = "DELETE",
                path = "/api/delete",
                body = requestBody
            )

            writer.print(request)
            writer.flush()
        }
    }

    suspend fun showModelInfo(modelName: String): OllamaModel = withContext(Dispatchers.IO) {
        createSocket().use { socket ->
            val writer = PrintWriter(socket.getOutputStream())
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