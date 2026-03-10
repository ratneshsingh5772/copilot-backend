"""
POST /api/v1/intent
Classifies a free-form customer query into a structured intent enum.
Called by Java before any further processing.
"""

import logging

from fastapi import APIRouter, HTTPException

from models.schemas import IntentRequest, IntentResponse, IntentType
from prompts.templates import INTENT_SYSTEM, intent_user_prompt
from services.ollama_service import chat_json

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/api/v1", tags=["Intent Detection"])


@router.post(
    "/intent",
    response_model=IntentResponse,
    summary="Classify customer query intent",
    description=(
        "Sends the customer's free-form message to the local Ollama model and returns "
        "a structured intent classification (e.g. TRAVEL_INQUIRY, PLAN_UPGRADE)."
    ),
)
async def detect_intent(req: IntentRequest) -> IntentResponse:
    user_prompt = intent_user_prompt(req)

    try:
        result = await chat_json(INTENT_SYSTEM, user_prompt, temperature=0.1)
    except ValueError as exc:
        raise HTTPException(status_code=502, detail=f"LLM parse error: {exc}")
    except Exception as exc:
        logger.exception("Ollama call failed")
        raise HTTPException(status_code=503, detail=f"Ollama unreachable: {exc}")

    # Safely coerce intent string; fall back to UNKNOWN
    raw_intent = result.get("identified_intent", "UNKNOWN")
    try:
        intent = IntentType(raw_intent)
    except ValueError:
        intent = IntentType.UNKNOWN

    return IntentResponse(
        customer_id=req.customer_id,
        identified_intent=intent,
        confidence=result.get("confidence", "LOW"),
        reasoning=result.get("reasoning", ""),
    )
