package com.kashif.kollama.data.local.service

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

actual fun getEngine(): HttpClientEngine = Darwin.create()
actual val IO: CoroutineDispatcher = Dispatchers.IO