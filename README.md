# AI Apps (Android + Modal + MCP)

This repository provides a starter template for an Android AI app backed by Modal serverless functions and an MCP (Model Context Protocol) server. It includes a Jetpack Compose chat UI, a Retrofit client, and Python services for text generation, summarization, and sentiment analysis.

## Project structure

```
AI_Apps/
├── android-app/              # Kotlin + Jetpack Compose
│   ├── app/src/main/java/com/yourapp/
│   │   ├── MainActivity.kt
│   │   ├── AIAppApplication.kt
│   │   ├── features/         # Feature modules (auth, chat, etc.)
│   │   ├── common/           # Shared UI components or utilities
│   │   ├── navigation/       # Jetpack Navigation setup
│   │   ├── repository/       # Data repositories
│   │   ├── api/              # Retrofit API service
│   │   │   ├── ModalApiService.kt
│   │   │   └── RetrofitClient.kt
│   │   ├── mcp/              # MCP client
│   │   │   └── MCPClient.kt
│   │   ├── data/             # Data models
│   │   │   └── model/Models.kt
│   │   ├── ui/               # UI screens
│   │   │   ├── screens/ChatScreen.kt
│   │   │   ├── viewmodel/ChatViewModel.kt
│   │   │   └── theme/
│   │   └── res/              # Android resources
│   ├── build.gradle.kts
│   └── settings.gradle.kts
│
├── backend/                   # Modal serverless functions
│   ├── modal_functions.py     # Text gen, summarization, sentiment
│   └── .env.example
│
├── mcp-server/               # MCP server implementation
│   ├── server.py
│   └── requirements.txt
│
├── shared-mcp/               # Shared configuration
│   └── tools-config.json
│
├── README.md                 # Full documentation
├── setup.sh                  # Automated setup script
└── .gitignore
```

## Quick start

1. **Android app**
   - Open `android-app/` in Android Studio.
   - Update `RetrofitClient.BASE_URL` to your deployed Modal URL.
   - Set `AuthRepository.updateToken(...)` after login/signup to send auth headers.
   - Run on device or emulator.

2. **Modal backend**
   - Create a Python virtual environment and install dependencies.
   - Deploy the `backend/modal_functions.py` with Modal.

3. **MCP server**
   - Install requirements in `mcp-server/`.
   - Run the server and configure your client to point at it.

## Tooling notes

- The Android app calls the Modal API directly via Retrofit.
- Chat history is stored locally in the app files directory (`chat_history.json`).
- Voice input uses Android's speech recognizer and requires microphone permission.
- `shared-mcp/tools-config.json` defines MCP tool metadata that can be shared across services.
- Update `.env.example` with your Modal token and any API keys you need.

## CI

- GitHub Actions builds a debug APK on every push/PR and uploads it as an artifact.

## Scripts

Use `setup.sh` to bootstrap both Python services and print next steps.
