package com.kashif.kollama.data.local.database

import android.content.Context
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.kashif.kollama.ChatDatabaseX

actual suspend fun createDatabaseWrapper(context: Any): ChatDatabaseX {
    val syncSchema = ChatDatabaseX.Schema.synchronous()
    val driver = AndroidSqliteDriver(
        schema = syncSchema,
        context = context as Context,
        name = "test.db"
    )
    ChatDatabaseX.Schema.create(driver).await()
    return ChatDatabaseX(driver)
}