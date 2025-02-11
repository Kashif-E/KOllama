package com.kashif.kollama.data.local.service

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

actual fun getEngine(): HttpClientEngine = OkHttp.create()
actual val IO: CoroutineDispatcher= Dispatchers.IO