package com.kashif.deepseek.data.local.service


import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class OllamaService {
    private val ollamaPath = "/usr/local/bin/ollama"


    private fun cleanOutput(text: String): String {
        if (text.isBlank()) return ""
        println(text)
        return text
            .replace(Regex("[⠀-⣿]"), "")
            .replace(Regex("\\u001B\\[[0-9;?]*[ -/]*[@-~]"), "")
            .replace(Regex("[\u0000-\u0008\u000B-\u000C\u000E-\u001F\u007F]"), "")
            .replace(Regex("\\d+[l]"), "")


            .replace(Regex("\\.\\s*###"), ".\n\n###")
            .replace(Regex("```"), "\n```\n")
            .replace(Regex("\\.\\s*\\*"), ".\n\n*")
            .replace(Regex("\\s{2,}"), " ")



    }

    fun chat(model: String, prompt: String): Flow<String> = flow {
        try {
            println("Starting Ollama process for model: $model")
            val process = ProcessBuilder(ollamaPath, "run", model)
                .redirectErrorStream(true)
                .start()

            println("Ollama process started with PID: ${process.pid()}")

            process.outputStream.bufferedWriter().use { writer ->
                println("Writing prompt to Ollama process: $prompt")
                writer.write(prompt)
                writer.newLine()
                writer.flush()
                println("Prompt written and flushed to Ollama process")
            }

            process.inputStream.bufferedReader().use { reader ->
                println("Starting to read output from Ollama process")
                reader.lineSequence()
                    .map { cleanOutput(it) }
                    .filter { it.isNotBlank() }
                    .forEach { line ->
                        if (line.isNotBlank()) {
                            println("Emitting line from Ollama: $line")
                            emit(line)
                        }
                    }
                println("Finished reading output from Ollama process")
            }

            val exitCode = process.waitFor()
            println("Ollama process exited with code: $exitCode")
            if (exitCode != 0) {
                throw RuntimeException("Ollama process failed with exit code $exitCode")
            }
        } catch (e: Exception) {
            println("Error during Ollama chat: ${e.message}")
            throw RuntimeException("Chat failed: ${e.message}")
        }
    }.flowOn(Dispatchers.IO)

    suspend fun listModels(): List<String> = withContext(Dispatchers.IO) {
        try {
            val process = ProcessBuilder(ollamaPath, "list")
                .redirectErrorStream(true)
                .start()

            process.inputStream.bufferedReader().use { reader ->
                reader.readLines()
                    .asSequence()
                    .map { cleanOutput(it) }
                    .filter { it.isNotBlank() }
                    .map { it.split("\\s+".toRegex()).first() }
                    .filter { it.isNotBlank() }
                    .toList()
            }
        } catch (e: Exception) {
            throw RuntimeException("Failed to list models: ${e.message}")
        }
    }


    suspend fun pullModel(modelName: String) = withContext(Dispatchers.IO) {
        try {
            val process = ProcessBuilder(ollamaPath, "pull", modelName)
                .redirectErrorStream(true)
                .start()

            val exitCode = process.waitFor()
            if (exitCode != 0) {
                throw RuntimeException("Failed to pull model: Process exited with code $exitCode")
            }
        } catch (e: Exception) {
            throw RuntimeException("Failed to pull model: ${e.message}")
        }
    }

    suspend fun removeModel(modelName: String) = withContext(Dispatchers.IO) {
        try {
            val process = ProcessBuilder(ollamaPath, "rm", modelName)
                .redirectErrorStream(true)
                .start()

            val exitCode = process.waitFor()
            if (exitCode != 0) {
                throw RuntimeException("Failed to remove model: Process exited with code $exitCode")
            }
        } catch (e: Exception) {
            throw RuntimeException("Failed to remove model: ${e.message}")
        }
    }

    suspend fun showModelInfo(modelName: String): String = withContext(Dispatchers.IO) {
        try {
            val process = ProcessBuilder(ollamaPath, "show", modelName)
                .redirectErrorStream(true)
                .start()

            process.inputStream.bufferedReader().use { reader ->
                val response = reader.readText()
                if (process.waitFor() != 0) {
                    throw RuntimeException("Failed to show model info")
                }
                response
            }
        } catch (e: Exception) {
            throw RuntimeException("Failed to show model info: ${e.message}")
        }
    }


    fun setGpuLayers(layers: Int) {
        System.setProperty("OLLAMA_GPU_LAYERS", layers.toString())
    }

    fun setCudaDevices(devices: String) {
        System.setProperty("CUDA_VISIBLE_DEVICES", devices)
    }

    fun setMaxRamUsage(ramMb: Int) {
        System.setProperty("OLLAMA_RAM", ramMb.toString())
    }

    fun setMaxGpuMemory(memoryMb: Int) {
        System.setProperty("OLLAMA_GPU_MEM", memoryMb.toString())
    }
}