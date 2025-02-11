package com.kashif.kollama.data.local.database

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.kashif.kollama.ChatDatabaseX

actual suspend fun createDatabaseWrapper(context: Any): ChatDatabaseX {
    val databasePath = System.getProperty("user.home") + "/chat.db"
    val driver = JdbcSqliteDriver("jdbc:sqlite:$databasePath")

    ChatDatabaseX.Schema.create(driver).await()

    return ChatDatabaseX(driver)

}