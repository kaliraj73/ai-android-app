package com.yourapp.mcp

import com.yourapp.api.RetrofitClient
import com.yourapp.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * MCP (Model Context Protocol) Client for Android
 * 
 * This client connects to the Modal backend and exposes AI tools
 * that can be used by the chat interface.
 */
class MCPClient {

    private val apiService = RetrofitClient.modalApiService

    /**
     * Available MCP tools
     */
    val availableTools = listOf(
        MCPToolInfo(
            name = "generate_text",
            displayName = "AI Text Generation",
            description = "Generate creative text using AI",
            icon = "✨"
        ),
        MCPToolInfo(
            name = "summarize_text",
            displayName = "Summarize",
            description = "Summarize long text into key points",
            icon = "📝"
        ),
        MCPToolInfo(
            name = "analyze_sentiment",
            displayName = "Sentiment Analysis",
            description = "Analyze the sentiment of text",
            icon = "😊"
        ),
        MCPToolInfo(
            name = "long_term_memory",
            displayName = "Long-term Memory",
            description = "Remember chats across sessions using Supabase/Firebase",
            icon = "🧠"
        ),
        MCPToolInfo(
            name = "pdf_summarizer",
            displayName = "PDF Summarizer",
            description = "Upload a PDF and get a summary",
            icon = "📄"
        ),
        MCPToolInfo(
            name = "image_understanding",
            displayName = "Image Understanding",
            description = "Explain what's in an image",
            icon = "🖼"
        )
    )

    /**
     * Check if the backend is healthy
     */
    suspend fun healthCheck(): Result<HealthResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.healthCheck()
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Health check failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Generate text using AI
     */
    suspend fun generateText(
        prompt: String,
        maxTokens: Int = 512,
        temperature: Float = 0.7f
    ): Result<GenerateResponse> = withContext(Dispatchers.IO) {
        try {
            val request = GenerateRequest(
                text = prompt,
                maxTokens = maxTokens,
                temperature = temperature
            )
            val response = apiService.generateText(request)
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Generation failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Summarize text
     */
    suspend fun summarizeText(
        text: String,
        maxLength: Int = 150
    ): Result<SummarizeResponse> = withContext(Dispatchers.IO) {
        try {
            val request = SummarizeRequest(text = text, maxLength = maxLength)
            val response = apiService.summarizeText(request)
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Summarization failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Analyze sentiment of text
     */
    suspend fun analyzeSentiment(text: String): Result<SentimentResponse> = withContext(Dispatchers.IO) {
        try {
            val request = SentimentRequest(text = text)
            val response = apiService.analyzeSentiment(request)
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Sentiment analysis failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Chat with long-term memory persistence
     */
    suspend fun chatWithMemory(
        sessionId: String,
        message: String
    ): Result<MemoryResponse> = withContext(Dispatchers.IO) {
        try {
            val request = MemoryRequest(sessionId = sessionId, message = message)
            val response = apiService.chatWithMemory(request)
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Memory chat failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Summarize a PDF by sending its base64 content
     */
    suspend fun summarizePdf(
        filename: String,
        contentBase64: String
    ): Result<PdfSummaryResponse> = withContext(Dispatchers.IO) {
        try {
            val request = PdfSummaryRequest(filename = filename, contentBase64 = contentBase64)
            val response = apiService.summarizePdf(request)
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("PDF summarization failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Describe an image by sending its base64 content
     */
    suspend fun describeImage(
        filename: String,
        imageBase64: String
    ): Result<VisionResponse> = withContext(Dispatchers.IO) {
        try {
            val request = VisionRequest(filename = filename, imageBase64 = imageBase64)
            val response = apiService.describeImage(request)
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Image understanding failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Execute a tool by name with parameters
     */
    suspend fun executeTool(
        toolName: String,
        params: Map<String, Any>
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            when (toolName) {
                "generate_text" -> {
                    val prompt = params["prompt"] as? String ?: return@withContext Result.failure(
                        IllegalArgumentException("Missing 'prompt' parameter")
                    )
                    val maxTokens = (params["max_tokens"] as? Int) ?: 512
                    val temperature = (params["temperature"] as? Float) ?: 0.7f
                    
                    generateText(prompt, maxTokens, temperature).map { it.response }
                }
                
                "summarize_text" -> {
                    val text = params["text"] as? String ?: return@withContext Result.failure(
                        IllegalArgumentException("Missing 'text' parameter")
                    )
                    val maxLength = (params["max_length"] as? Int) ?: 150
                    
                    summarizeText(text, maxLength).map { it.summary }
                }
                
                "analyze_sentiment" -> {
                    val text = params["text"] as? String ?: return@withContext Result.failure(
                        IllegalArgumentException("Missing 'text' parameter")
                    )
                    
                    analyzeSentiment(text).map { 
                        "${it.sentiment} (confidence: ${"%.2f".format(it.confidence)})" 
                    }
                }

                "long_term_memory" -> {
                    val sessionId = params["session_id"] as? String ?: return@withContext Result.failure(
                        IllegalArgumentException("Missing 'session_id' parameter")
                    )
                    val message = params["message"] as? String ?: return@withContext Result.failure(
                        IllegalArgumentException("Missing 'message' parameter")
                    )

                    chatWithMemory(sessionId, message).map { it.reply }
                }

                "pdf_summarizer" -> {
                    val filename = params["filename"] as? String ?: return@withContext Result.failure(
                        IllegalArgumentException("Missing 'filename' parameter")
                    )
                    val contentBase64 = params["content_base64"] as? String ?: return@withContext Result.failure(
                        IllegalArgumentException("Missing 'content_base64' parameter")
                    )

                    summarizePdf(filename, contentBase64).map { it.summary }
                }

                "image_understanding" -> {
                    val filename = params["filename"] as? String ?: return@withContext Result.failure(
                        IllegalArgumentException("Missing 'filename' parameter")
                    )
                    val imageBase64 = params["image_base64"] as? String ?: return@withContext Result.failure(
                        IllegalArgumentException("Missing 'image_base64' parameter")
                    )

                    describeImage(filename, imageBase64).map { it.description }
                }
                
                else -> Result.failure(IllegalArgumentException("Unknown tool: $toolName"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Information about an MCP tool for UI display
 */
data class MCPToolInfo(
    val name: String,
    val displayName: String,
    val description: String,
    val icon: String
)
