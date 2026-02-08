package com.yourapp.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.yourapp.data.model.ChatMessage
import java.io.File

class ChatHistoryStore(context: Context) {
    private val gson = Gson()
    private val storageFile = File(context.filesDir, "chat_history.json")

    fun loadMessages(): List<ChatMessage> {
        if (!storageFile.exists()) return emptyList()
        return runCatching {
            val json = storageFile.readText()
            val type = object : TypeToken<List<ChatMessage>>() {}.type
            gson.fromJson<List<ChatMessage>>(json, type) ?: emptyList()
        }.getOrDefault(emptyList())
    }

    fun saveMessages(messages: List<ChatMessage>) {
        runCatching {
            val json = gson.toJson(messages)
            storageFile.writeText(json)
        }
    }

    fun clear() {
        if (storageFile.exists()) {
            storageFile.delete()
        }
    }
}
