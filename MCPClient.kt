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
