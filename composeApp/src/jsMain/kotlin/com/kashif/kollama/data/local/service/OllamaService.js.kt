package com.kashif.kollama.data.local.service

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.js.Js
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

actual fun getEngine(): HttpClientEngine = Js.create()
actual val IO: CoroutineDispatcher = Dispatchers.Default