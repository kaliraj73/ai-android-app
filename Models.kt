package com.yourapp.data.model

import com.google.gson.annotations.SerializedName

/**
 * Data models for API requests and responses
 */

// Request models

data class GenerateRequest(
    @SerializedName("text")
    val text: String,
    @SerializedName("max_tokens")
    val maxTokens: Int = 512,
    @SerializedName("temperature")
    val temperature: Float = 0.7f
)

data class SummarizeRequest(
    @SerializedName("text")
    val text: String,
    @SerializedName("max_length")
    val maxLength: Int = 150
)

data class SentimentRequest(
    @SerializedName("text")
    val text: String
)

data class MemoryRequest(
    @SerializedName("session_id")
    val sessionId: String,
    @SerializedName("message")
    val message: String
)

data class PdfSummaryRequest(
    @SerializedName("filename")
    val filename: String,
    @SerializedName("content_base64")
    val contentBase64: String
)

data class VisionRequest(
    @SerializedName("filename")
    val filename: String,
    @SerializedName("image_base64")
    val imageBase64: String
)

// Response models

data class GenerateResponse(
    @SerializedName("prompt")
    val prompt: String,
    @SerializedName("response")
    val response: String,
    @SerializedName("model")
    val model: String
)

data class SummarizeResponse(
    @SerializedName("original_length")
    val originalLength: Int,
    @SerializedName("summary")
    val summary: String
)

data class SentimentResponse(
    @SerializedName("text")
    val text: String,
    @SerializedName("sentiment")
    val sentiment: String,
    @SerializedName("confidence")
    val confidence: Float
)

data class MemoryResponse(
    @SerializedName("reply")
    val reply: String,
    @SerializedName("memory_saved")
    val memorySaved: Boolean
)

data class PdfSummaryResponse(
    @SerializedName("summary")
    val summary: String,
    @SerializedName("page_count")
    val pageCount: Int
)

data class VisionResponse(
    @SerializedName("description")
    val description: String
)

data class HealthResponse(
    @SerializedName("status")
    val status: String,
    @SerializedName("service")
    val service: String,
    @SerializedName("version")
    val version: String
)

data class BackendStatusResponse(
    @SerializedName("fastapi")
    val fastapi: String,
    @SerializedName("redis")
    val redis: String,
    @SerializedName("supabase")
    val supabase: String,
    @SerializedName("cloudflare")
    val cloudflare: String,
    @SerializedName("sentry")
    val sentry: String
)

// Error response

data class ErrorResponse(
    @SerializedName("detail")
    val detail: String
)

// MCP Tool models

data class MCPTool(
    @SerializedName("name")
    val name: String,
    @SerializedName("description")
    val description: String,
    @SerializedName("parameters")
    val parameters: Map<String, Any>
)

data class MCPToolsResponse(
    @SerializedName("tools")
    val tools: List<MCPTool>
)

// Chat message model for UI

data class ChatMessage(
    val id: String = System.currentTimeMillis().toString(),
    val content: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val isLoading: Boolean = false,
    val error: String? = null
)

// UI State sealed classes

sealed class UiState<out T> {
    data object Idle : UiState<Nothing>()
    data object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}
