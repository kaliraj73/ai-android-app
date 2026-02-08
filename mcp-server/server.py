"""Simple MCP server implementation.

Run with:
  uvicorn server:app --reload --port 8001
"""
from __future__ import annotations

import json
from pathlib import Path
from typing import Any, Dict

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel

app = FastAPI(title="MCP Server", version="1.0.0")

TOOLS_PATH = Path(__file__).resolve().parent.parent / "shared-mcp" / "tools-config.json"


class ToolExecuteRequest(BaseModel):
    tool_name: str
    parameters: Dict[str, Any]


def load_tools() -> Dict[str, Any]:
    if not TOOLS_PATH.exists():
        return {"tools": []}
    return json.loads(TOOLS_PATH.read_text(encoding="utf-8"))


@app.get("/tools")
async def list_tools() -> Dict[str, Any]:
    return load_tools()


@app.post("/execute")
async def execute_tool(request: ToolExecuteRequest) -> Dict[str, Any]:
    tools = load_tools().get("tools", [])
    tool = next((item for item in tools if item.get("name") == request.tool_name), None)
    if not tool:
        raise HTTPException(status_code=404, detail="Tool not found")

    # Placeholder response for quick wiring.
    return {
        "tool": request.tool_name,
        "parameters": request.parameters,
        "result": f"Executed {request.tool_name} with {request.parameters}",
    }
