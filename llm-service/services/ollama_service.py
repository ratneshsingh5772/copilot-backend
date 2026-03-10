"""
Ollama service layer — all communication with the local Ollama server lives here.
The Java backend never calls Ollama directly; it only calls *this* FastAPI service.
"""

import json
import logging
from typing import Any

import httpx

from config import settings

logger = logging.getLogger(__name__)


# ── Internal helpers ───────────────────────────────────────────────────────────

def _build_chat_payload(system_prompt: str, user_prompt: str, temperature: float = 0.2) -> dict:
    """Build the JSON body for Ollama's /api/chat endpoint."""
    return {
        "model": settings.ollama_model,
        "stream": False,
        "options": {"temperature": temperature},
        "messages": [
            {"role": "system", "content": system_prompt},
            {"role": "user",   "content": user_prompt},
        ],
    }


def _extract_content(raw: dict) -> str:
    """Pull the assistant message content from Ollama's response envelope."""
    try:
        return raw["message"]["content"].strip()
    except KeyError as exc:
        raise ValueError(f"Unexpected Ollama response shape: {raw}") from exc


def _parse_json_response(raw_text: str) -> dict:
    """
    Parse the LLM output as JSON.
    Handles the common case where the model wraps JSON in a markdown code-fence.
    """
    text = raw_text.strip()
    # Strip code-fence if present  ```json ... ```
    if text.startswith("```"):
        lines = text.splitlines()
        text = "\n".join(
            line for line in lines
            if not line.strip().startswith("```")
        ).strip()
    try:
        return json.loads(text)
    except json.JSONDecodeError as exc:
        logger.error("JSON parse failure. Raw LLM text:\n%s", raw_text)
        raise ValueError(f"LLM returned non-JSON output: {exc}") from exc


# ── Public API ─────────────────────────────────────────────────────────────────

async def chat_json(system_prompt: str, user_prompt: str, temperature: float = 0.2) -> dict[str, Any]:
    """
    Send a system + user prompt to Ollama and return parsed JSON.
    All four endpoint handlers call this function.

    Raises:
        httpx.HTTPStatusError  — if Ollama returns a non-2xx response.
        ValueError             — if the LLM output cannot be parsed as JSON.
    """
    payload = _build_chat_payload(system_prompt, user_prompt, temperature)

    async with httpx.AsyncClient(timeout=settings.ollama_timeout_seconds) as client:
        logger.debug("POST %s/api/chat  model=%s", settings.ollama_base_url, settings.ollama_model)
        response = await client.post(
            f"{settings.ollama_base_url}/api/chat",
            json=payload,
        )
        response.raise_for_status()

    raw_text = _extract_content(response.json())
    logger.debug("Raw LLM output:\n%s", raw_text)
    return _parse_json_response(raw_text)


async def check_ollama_health() -> bool:
    """Return True if Ollama is reachable and the configured model is available."""
    try:
        async with httpx.AsyncClient(timeout=5) as client:
            resp = await client.get(f"{settings.ollama_base_url}/api/tags")
            resp.raise_for_status()
            tags = resp.json()
            available_models = [m["name"] for m in tags.get("models", [])]
            # Accept 'llama3.2' even if Ollama reports 'llama3.2:latest'
            return any(
                settings.ollama_model in m
                for m in available_models
            )
    except Exception:
        return False
