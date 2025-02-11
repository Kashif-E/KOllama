package com.kashif.kollama.data.local.service

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import kotlinx.coroutines.CoroutineDispatcher

actual fun getEngine(): HttpClientEngine = CIO.create()
actual val IO: CoroutineDispatcher= kotlinx.coroutines.Dispatchers.IO