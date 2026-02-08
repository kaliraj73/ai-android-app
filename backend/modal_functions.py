"""Modal serverless backend for AI text utilities.

Deploy with:
  modal deploy modal_functions.py
"""
from __future__ import annotations

from typing import Dict

import modal
from fastapi import FastAPI
from pydantic import BaseModel

app = modal.App("ai-app-backend-fastapi")

web_app = FastAPI(title="AI App Backend", version="1.0.0")


class GenerateRequest(BaseModel):
    text: str
    max_tokens: int = 512
    temperature: float = 0.7


class SummarizeRequest(BaseModel):
    text: str
    max_length: int = 150


class SentimentRequest(BaseModel):
    text: str


@web_app.get("/health")
async def health() -> Dict[str, str]:
    return {"status": "ok", "service": "ai-backend", "version": "1.0.0"}


@web_app.post("/generate")
async def generate(request: GenerateRequest) -> Dict[str, str]:
    # Placeholder response. Replace with your model inference.
    response = f"AI response to: {request.text}"
    return {"prompt": request.text, "response": response, "model": "placeholder"}


@web_app.post("/summarize")
async def summarize(request: SummarizeRequest) -> Dict[str, str | int]:
    text = request.text.strip()
    summary = text[: request.max_length]
    return {"original_length": len(text), "summary": summary}


@web_app.post("/sentiment")
async def sentiment(request: SentimentRequest) -> Dict[str, str | float]:
    text = request.text.lower()
    score = 0.5
    if any(word in text for word in ("good", "great", "love", "awesome")):
        score = 0.9
        sentiment_label = "positive"
    elif any(word in text for word in ("bad", "hate", "terrible", "awful")):
        score = 0.1
        sentiment_label = "negative"
    else:
        sentiment_label = "neutral"
    return {"text": request.text, "sentiment": sentiment_label, "confidence": score}


@app.function(image=modal.Image.debian_slim().pip_install("fastapi", "pydantic"))
@modal.asgi_app()
def fastapi_app():
    return web_app
