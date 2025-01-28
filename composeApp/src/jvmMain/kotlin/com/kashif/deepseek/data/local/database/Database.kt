package com.kashif.deepseek.data.local.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.kashif.deepseek.data.local.dao.ChatDao
import com.kashif.deepseek.domain.model.ChatMessage
import com.kashif.deepseek.domain.model.ChatSession
import kotlinx.coroutines.Dispatchers
import java.io.File

@Database(
    entities = [ChatSession::class, ChatMessage::class],
    version = 1,
    exportSchema = false
)
abstract class ChatDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao

    companion object {
        fun create(): ChatDatabase {
            return getDatabase()
        }
    }
}

private fun getDatabase(): ChatDatabase {
    val dbFile = File(System.getProperty("java.io.tmpdir"), "kollama.db")
    return Room.databaseBuilder<ChatDatabase>(
        name = dbFile.absolutePath,
    ).setQueryCoroutineContext(Dispatchers.IO).setDriver(BundledSQLiteDriver())
        .fallbackToDestructiveMigration(true).build()
}