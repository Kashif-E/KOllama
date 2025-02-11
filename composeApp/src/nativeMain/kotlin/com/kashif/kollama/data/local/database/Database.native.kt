package com.kashif.kollama.data.local.database

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.kashif.kollama.ChatDatabaseX


actual suspend fun createDatabaseWrapper(context: Any): ChatDatabaseX {
    val schema = ChatDatabaseX.Schema.synchronous()
    val driver = NativeSqliteDriver(schema, "chat.db")
    ChatDatabaseX.Schema.create(driver).await()
    return ChatDatabaseX(driver)
}