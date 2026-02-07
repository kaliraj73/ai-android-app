package com.yourapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourapp.data.model.ChatMessage
import com.yourapp.data.model.UiState
import com.yourapp.mcp.MCPClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the Chat screen
 */
class ChatViewModel : ViewModel() {

    private val mcpClient = MCPClient()

    // Chat messages
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    // Connection state
    private val _connectionState = MutableStateFlow<UiState<Boolean>>(UiState.Idle)
    val connectionState: StateFlow<UiState<Boolean>> = _connectionState.asStateFlow()

    // Current tool selection
    private val _selectedTool = MutableStateFlow<String?>(null)
    val selectedTool: StateFlow<String?> = _selectedTool.asStateFlow()

    // Available tools
    val availableTools = mcpClient.availableTools

    init {
        // Add welcome message
        _messages.value = listOf(
            ChatMessage(
                content = "Hello! I'm your AI assistant. Select a tool below or just chat with me!",
                isUser = false
            )
        )
        
        // Check connection
        checkConnection()
    }

    /**
     * Check backend connection
     */
    fun checkConnection() {
        viewModelScope.launch {
            _connectionState.value = UiState.Loading
            
            mcpClient.healthCheck()
                .onSuccess {
                    _connectionState.value = UiState.Success(true)
                }
                .onFailure { error ->
                    _connectionState.value = UiState.Error(error.message ?: "Connection failed")
                }
        }
    }

    /**
     * Select a tool to use
     */
    fun selectTool(toolName: String?) {
        _selectedTool.value = toolName
    }

    /**
     * Send a message and get AI response
     */
    fun sendMessage(content: String) {
        if (content.isBlank()) return

        viewModelScope.launch {
            // Add user message
            val userMessage = ChatMessage(content = content, isUser = true)
            _messages.value = _messages.value + userMessage

            // Show loading
            val loadingMessage = ChatMessage(
                content = "",
                isUser = false,
                isLoading = true
            )
            _messages.value = _messages.value + loadingMessage

            // Get response based on selected tool
            val result = when (_selectedTool.value) {
                "generate_text" -> mcpClient.generateText(content)
                "summarize_text" -> mcpClient.summarizeText(content)
                "analyze_sentiment" -> mcpClient.analyzeSentiment(content)
                else -> mcpClient.generateText(content) // Default to text generation
            }

            // Remove loading message and add response
            _messages.value = _messages.value.filterNot { it.isLoading }

            result
                .onSuccess { response ->
                    val responseText = when (response) {
                        is com.yourapp.data.model.GenerateResponse -> response.response
                        is com.yourapp.data.model.SummarizeResponse -> response.summary
                        is com.yourapp.data.model.SentimentResponse -> 
                            "${response.sentiment} (confidence: ${"%.2f".format(response.confidence)})"
                        else -> "Response received"
                    }
                    
                    val aiMessage = ChatMessage(
                        content = responseText,
                        isUser = false
                    )
                    _messages.value = _messages.value + aiMessage
                }
                .onFailure { error ->
                    val errorMessage = ChatMessage(
                        content = "Sorry, something went wrong. Please try again.",
                        isUser = false,
                        error = error.message
                    )
                    _messages.value = _messages.value + errorMessage
                }
        }
    }

    /**
     * Clear all messages
     */
    fun clearMessages() {
        _messages.value = listOf(
            ChatMessage(
                content = "Chat cleared. How can I help you?",
                isUser = false
            )
        )
    }

    /**
     * Execute a specific tool with parameters
     */
    fun executeTool(toolName: String, params: Map<String, Any>) {
        viewModelScope.launch {
            // Show loading
            val loadingMessage = ChatMessage(
                content = "Processing with ${availableTools.find { it.name == toolName }?.displayName ?: toolName}...",
                isUser = false,
                isLoading = true
            )
            _messages.value = _messages.value + loadingMessage

            val result = mcpClient.executeTool(toolName, params)

            // Remove loading message
            _messages.value = _messages.value.filterNot { it.isLoading }

            result
                .onSuccess { response ->
                    val aiMessage = ChatMessage(
                        content = response,
                        isUser = false
                    )
                    _messages.value = _messages.value + aiMessage
                }
                .onFailure { error ->
                    val errorMessage = ChatMessage(
                        content = "Tool execution failed. Please try again.",
                        isUser = false,
                        error = error.message
                    )
                    _messages.value = _messages.value + errorMessage
                }
        }
    }
}
