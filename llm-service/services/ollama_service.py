"""
Ollama service layer — all communication with the local Ollama server lives here.
The Java backend never calls Ollama directly; it only calls *this* FastAPI service.
"""

import json
import logging
import re
from typing import Any

import httpx

from config import settings

logger = logging.getLogger(__name__)

_MAX_RETRIES = 2   # retry the LLM call if JSON parsing fails


# ── Internal helpers ───────────────────────────────────────────────────────────

def _build_chat_payload(system_prompt: str, user_prompt: str, temperature: float = 0.2) -> dict:
    """Build the JSON body for Ollama's /api/chat endpoint.

    Setting ``format='json'`` instructs Ollama's grammar-sampler to constrain
    the model's output tokens to valid JSON, eliminating most parse failures.
    """
    return {
        "model": settings.ollama_model,
        "stream": False,
        "format": "json",                          # ← force JSON grammar mode
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


def _extract_json_object(text: str) -> str:
    """
    Walk the text and return the first complete JSON object ``{…}`` or
    array ``[…]`` found, ignoring any prose before or after it.
    Falls back to the original text if nothing is found.
    """
    start = -1
    open_char = "{"
    close_char = "}"
    for i, ch in enumerate(text):
        if ch in ("{", "["):
            start = i
            open_char = ch
            close_char = "}" if ch == "{" else "]"
            break

    if start == -1:
        return text

    depth = 0
    in_string = False
    escape_next = False
    for i in range(start, len(text)):
        ch = text[i]
        if escape_next:
            escape_next = False
            continue
        if ch == "\\" and in_string:
            escape_next = True
            continue
        if ch == '"':
            in_string = not in_string
        elif not in_string:
            if ch == open_char:
                depth += 1
            elif ch == close_char:
                depth -= 1
                if depth == 0:
                    return text[start : i + 1]

    # Matching close bracket not found — return everything from the open bracket
    return text[start:]


def _repair_json(text: str) -> str:
    """
    Best-effort repair of common LLM JSON mistakes before parsing.

    Handles:
    - Markdown code-fences  (``` json ... ```)
    - Non-printable / stray control characters (0x00-0x08, 0x0b, 0x0c, 0x0e-0x1f)
    - Trailing commas before } or ]
    - Unescaped newlines, carriage-returns, and tabs inside string values
    - Leading / trailing prose around the JSON object
    """
    # 1. Strip markdown code-fence if present
    if text.startswith("```"):
        lines = text.splitlines()
        text = "\n".join(
            line for line in lines
            if not line.strip().startswith("```")
        ).strip()

    # 2. Remove non-printable control characters that are never valid in JSON
    #    (keep \n \r \t — they are handled below inside strings)
    text = re.sub(r"[\x00-\x08\x0b\x0c\x0e-\x1f\x7f]", "", text)

    # 3. Strip any leading prose / trailing prose — keep only the JSON value
    text = _extract_json_object(text)

    # 4. Remove trailing commas before closing braces/brackets
    #    e.g.  {"a": 1,}  →  {"a": 1}
    text = re.sub(r",\s*([\}\]])", r"\1", text)

    # 5. Replace literal newlines / carriage-returns / tabs inside JSON string
    #    values (walk char-by-char to only fix inside quoted strings)
    result = []
    in_string = False
    escape_next = False
    for ch in text:
        if escape_next:
            result.append(ch)
            escape_next = False
        elif ch == "\\":
            result.append(ch)
            escape_next = True
        elif ch == '"':
            in_string = not in_string
            result.append(ch)
        elif in_string and ch == "\n":
            result.append("\\n")
        elif in_string and ch == "\r":
            result.append("\\r")
        elif in_string and ch == "\t":
            result.append("\\t")
        else:
            result.append(ch)
    return "".join(result)


def _parse_json_response(raw_text: str) -> dict:
    """
    Parse the LLM output as JSON with several repair strategies.

    Strategy order:
    1. Direct parse (Ollama JSON-mode usually produces valid JSON)
    2. Repair + parse (fixes fences, control chars, trailing commas, bare newlines)
    3. Extract JSON boundary then parse (handles prose wrapping the object)
    """
    # Fast path — most of the time Ollama's grammar sampler gives clean JSON
    try:
        return json.loads(raw_text)
    except json.JSONDecodeError:
        pass

    repaired = _repair_json(raw_text)
    try:
        return json.loads(repaired)
    except json.JSONDecodeError:
        pass

    # Last-ditch: try extracting the JSON object from the repaired text again
    extracted = _extract_json_object(repaired)
    try:
        return json.loads(extracted)
    except json.JSONDecodeError as exc:
        logger.error(
            "JSON parse failure after all repair strategies.\n"
            "Original:\n%s\nRepaired:\n%s\nExtracted:\n%s",
            raw_text, repaired, extracted,
        )
        raise ValueError(f"LLM returned non-JSON output: {exc}") from exc


# ── Public API ─────────────────────────────────────────────────────────────────

async def chat_json(system_prompt: str, user_prompt: str, temperature: float = 0.2) -> dict[str, Any]:
    """
    Send a system + user prompt to Ollama and return parsed JSON.

    Retries up to ``_MAX_RETRIES`` times if the response cannot be parsed as
    valid JSON (e.g. the model occasionally produces a truncated response).

    Raises:
        httpx.HTTPStatusError  — if Ollama returns a non-2xx response.
        ValueError             — if the LLM output cannot be parsed after all retries.
    """
    payload = _build_chat_payload(system_prompt, user_prompt, temperature)
    last_error: Exception | None = None

    for attempt in range(1, _MAX_RETRIES + 2):   # attempts: 1, 2, 3
        try:
            async with httpx.AsyncClient(timeout=settings.ollama_timeout_seconds) as client:
                logger.debug("POST %s/api/chat  model=%s  attempt=%d",
                             settings.ollama_base_url, settings.ollama_model, attempt)
                response = await client.post(
                    f"{settings.ollama_base_url}/api/chat",
                    json=payload,
                )
                response.raise_for_status()

            raw_text = _extract_content(response.json())
            logger.debug("Raw LLM output (attempt %d):\n%s", attempt, raw_text)
            return _parse_json_response(raw_text)

        except ValueError as exc:
            last_error = exc
            if attempt <= _MAX_RETRIES:
                logger.warning("JSON parse failed on attempt %d — retrying: %s", attempt, exc)
            # slightly raise temperature on retries to get a different output
            payload["options"]["temperature"] = min(temperature + attempt * 0.1, 0.9)

    raise last_error  # all retries exhausted


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
