#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

printf "\nSetting up Python virtual environment...\n"
python3 -m venv "$ROOT_DIR/.venv"
source "$ROOT_DIR/.venv/bin/activate"

printf "\nInstalling MCP server dependencies...\n"
pip install -r "$ROOT_DIR/mcp-server/requirements.txt"

printf "\nSetup complete. Next steps:\n"
printf "1) Deploy Modal backend: cd backend && modal deploy modal_functions.py\n"
printf "2) Run MCP server: cd mcp-server && uvicorn server:app --reload --port 8001\n"
printf "3) Open android-app/ in Android Studio and update RetrofitClient.BASE_URL\n\n"
