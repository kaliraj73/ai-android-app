package com.yourapp.ui.screens

import android.Manifest
import android.content.Intent
import android.provider.OpenableColumns
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yourapp.data.model.ChatMessage
import com.yourapp.data.model.UiState
import com.yourapp.mcp.MCPToolInfo
import com.yourapp.ui.viewmodel.ChatViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val selectedTool by viewModel.selectedTool.collectAsStateWithLifecycle()
    val availableTools = viewModel.availableTools

    var inputText by remember { mutableStateOf("") }
    var isVoiceReplyEnabled by remember { mutableStateOf(true) }
    var isListening by remember { mutableStateOf(false) }
    var lastSpokenMessageId by remember { mutableStateOf<String?>(null) }
    var ttsReady by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()

    val textToSpeech = remember {
        TextToSpeech(context) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
        }
    }

    val speechRecognizer = remember {
        SpeechRecognizer.createSpeechRecognizer(context)
    }

    DisposableEffect(textToSpeech) {
        onDispose {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
    }

    DisposableEffect(speechRecognizer) {
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: android.os.Bundle?) = Unit
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit
            override fun onError(error: Int) {
                isListening = false
                viewModel.addSystemMessage("Voice input failed. Please try again.")
            }

            override fun onResults(results: android.os.Bundle?) {
                val matches = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                if (!matches.isNullOrBlank()) {
                    inputText = matches
                }
                isListening = false
            }

            override fun onPartialResults(partialResults: android.os.Bundle?) = Unit
            override fun onEvent(eventType: Int, params: android.os.Bundle?) = Unit
        })

        onDispose {
            speechRecognizer.destroy()
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            }
            isListening = true
            speechRecognizer.startListening(intent)
        } else {
            viewModel.addSystemMessage("Microphone permission denied.")
        }
    }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val filename = getDisplayName(context, uri) ?: "document.pdf"
                val base64 = readBase64FromUri(context, uri)
                if (base64 != null) {
                    viewModel.summarizePdf(filename, base64)
                } else {
                    viewModel.addSystemMessage("Unable to read PDF content.")
                }
            }
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val filename = getDisplayName(context, uri) ?: "image"
                val base64 = readBase64FromUri(context, uri)
                if (base64 != null) {
                    viewModel.describeImage(filename, base64)
                } else {
                    viewModel.addSystemMessage("Unable to read image content.")
                }
            }
        }
    }

    // Scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    LaunchedEffect(messages, isVoiceReplyEnabled, ttsReady) {
        if (!isVoiceReplyEnabled || !ttsReady) return@LaunchedEffect

        val latestMessage = messages.lastOrNull { !it.isUser && !it.isLoading && it.error == null }
        if (latestMessage != null && latestMessage.id != lastSpokenMessageId) {
            textToSpeech.language = Locale.getDefault()
            textToSpeech.speak(
                latestMessage.content,
                TextToSpeech.QUEUE_FLUSH,
                null,
                latestMessage.id
            )
            lastSpokenMessageId = latestMessage.id
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Chat") },
                actions = {
                    // Connection status indicator
                    when (connectionState) {
                        is UiState.Success -> {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Connected",
                                tint = Color.Green,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }
                        is UiState.Error -> {
                            IconButton(onClick = { viewModel.checkConnection() }) {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = "Connection error",
                                    tint = Color.Red
                                )
                            }
                        }
                        is UiState.Loading -> {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(24.dp)
                                    .padding(horizontal = 8.dp),
                                strokeWidth = 2.dp
                            )
                        }
                        else -> {}
                    }
                    
                    // Clear chat button
                    IconButton(onClick = { viewModel.clearMessages() }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear chat"
                        )
                    }

                    IconToggleButton(
                        checked = isVoiceReplyEnabled,
                        onCheckedChange = { isVoiceReplyEnabled = it }
                    ) {
                        Icon(
                            imageVector = if (isVoiceReplyEnabled) {
                                Icons.Default.VolumeUp
                            } else {
                                Icons.Default.VolumeOff
                            },
                            contentDescription = if (isVoiceReplyEnabled) {
                                "Disable voice reply"
                            } else {
                                "Enable voice reply"
                            }
                        )
                    }
                }
            )
        },
        bottomBar = {
            Column {
                // Tool selector
                ToolSelector(
                    tools = availableTools,
                    selectedTool = selectedTool,
                    onToolSelected = { viewModel.selectTool(it) }
                )
                
                // Input area
                ChatInput(
                    value = inputText,
                    onValueChange = { inputText = it },
                    onSend = {
                        if (inputText.isNotBlank()) {
                            viewModel.sendMessage(inputText)
                            inputText = ""
                            keyboardController?.hide()
                        }
                    },
                    onVoiceInput = {
                        if (SpeechRecognizer.isRecognitionAvailable(context)) {
                            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        } else {
                            viewModel.addSystemMessage("Speech recognition is unavailable on this device.")
                        }
                    },
                    onPickPdf = { pdfPickerLauncher.launch("application/pdf") },
                    onPickImage = { imagePickerLauncher.launch("image/*") },
                    isPdfToolSelected = selectedTool == "pdf_summarizer",
                    isImageToolSelected = selectedTool == "image_understanding",
                    isListening = isListening,
                    isEnabled = connectionState is UiState.Success
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (messages.isEmpty()) {
                EmptyChatMessage()
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages, key = { it.id }) { message ->
                        ChatMessageItem(message = message)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatMessageItem(
    message: ChatMessage,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (message.isUser) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    
    val textColor = if (message.isUser) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val alignment = if (message.isUser) Alignment.End else Alignment.Start

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(backgroundColor)
                .padding(12.dp)
        ) {
            when {
                message.isLoading -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = textColor
                        )
                        Text(
                            text = message.content,
                            color = textColor,
                            fontSize = 14.sp
                        )
                    }
                }
                message.error != null -> {
                    Column {
                        Text(
                            text = message.content,
                            color = textColor,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Error: ${message.error}",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    }
                }
                else -> {
                    Text(
                        text = message.content,
                        color = textColor,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onVoiceInput: () -> Unit,
    onPickPdf: () -> Unit,
    onPickImage: () -> Unit,
    isPdfToolSelected: Boolean,
    isImageToolSelected: Boolean,
    isListening: Boolean,
    isEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = onVoiceInput,
                enabled = isEnabled && !isListening
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = if (isListening) "Listening..." else "Voice input"
                )
            }

            if (isPdfToolSelected) {
                IconButton(
                    onClick = onPickPdf,
                    enabled = isEnabled
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = "Upload PDF"
                    )
                }
            }

            if (isImageToolSelected) {
                IconButton(
                    onClick = onPickImage,
                    enabled = isEnabled
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = "Upload image"
                    )
                }
            }

            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        when {
                            isPdfToolSelected -> "Upload a PDF to summarize..."
                            isImageToolSelected -> "Upload an image to analyze..."
                            else -> "Type a message..."
                        }
                    )
                },
                enabled = isEnabled,
                singleLine = false,
                maxLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() }),
                shape = RoundedCornerShape(24.dp)
            )
            
            FloatingActionButton(
                onClick = onSend,
                modifier = Modifier.size(48.dp),
                enabled = isEnabled && value.isNotBlank()
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send"
                )
            }
        }
    }
}

@Composable
private fun ToolSelector(
    tools: List<MCPToolInfo>,
    selectedTool: String?,
    onToolSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // "Chat" option (no specific tool)
            ToolChip(
                icon = "💬",
                label = "Chat",
                isSelected = selectedTool == null,
                onClick = { onToolSelected(null) }
            )
            
            // Tool options
            tools.forEach { tool ->
                ToolChip(
                    icon = tool.icon,
                    label = tool.displayName,
                    isSelected = selectedTool == tool.name,
                    onClick = { onToolSelected(tool.name) }
                )
            }
        }
    }
}

@Composable
private fun ToolChip(
    icon: String,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    
    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = icon, fontSize = 16.sp)
            Text(
                text = label,
                color = contentColor,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun EmptyChatMessage(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Chat,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
            Text(
                text = "Start a conversation!",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

private fun getDisplayName(context: android.content.Context, uri: android.net.Uri): String? {
    val resolver = context.contentResolver
    val cursor = resolver.query(uri, null, null, null, null)
    cursor?.use {
        val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (nameIndex != -1 && it.moveToFirst()) {
            return it.getString(nameIndex)
        }
    }
    return null
}

private suspend fun readBase64FromUri(
    context: android.content.Context,
    uri: android.net.Uri
): String? = withContext(Dispatchers.IO) {
    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
    bytes?.let { Base64.encodeToString(it, Base64.NO_WRAP) }
}
