package com.kashif.kollama.data.local.database

import app.cash.sqldelight.driver.worker.WebWorkerDriver
import com.kashif.kollama.ChatDatabaseX
import org.w3c.dom.Worker

actual suspend fun createDatabaseWrapper(context: Any): ChatDatabaseX {
    val driver = WebWorkerDriver(
        Worker(
            js("""new URL("@cashapp/sqldelight-sqljs-worker/sqljs.worker.js", import.meta.url)""")
        )
    )
    ChatDatabaseX.Schema.create(driver).await()
    return ChatDatabaseX(driver)
}