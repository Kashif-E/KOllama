package com.kashif.deepseek.data.local.service

data class ChatSession(
    val messages: MutableList<Message> = mutableListOf(),
    var context: List<Int>? = null
)
