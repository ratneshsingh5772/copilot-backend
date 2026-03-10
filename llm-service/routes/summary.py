"""
POST /api/v1/summary
Summarises a completed customer-copilot conversation for audit logging
into the Copilot_Interactions MySQL table.
"""

import logging

from fastapi import APIRouter, HTTPException

from models.schemas import SummaryRequest, SummaryResponse
from prompts.templates import SUMMARY_SYSTEM, summary_user_prompt
from services.ollama_service import chat_json

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/api/v1", tags=["Conversation Summary"])


@router.post(
    "/summary",
    response_model=SummaryResponse,
    summary="Summarise a completed copilot conversation",
    description=(
        "Feeds the full conversation history to Ollama and returns a structured "
        "summary for storage in the Copilot_Interactions table."
    ),
)
async def summarise_conversation(req: SummaryRequest) -> SummaryResponse:
    if not req.conversation_history:
        raise HTTPException(status_code=400, detail="conversation_history must not be empty")

    user_prompt = summary_user_prompt(req)

    try:
        result = await chat_json(SUMMARY_SYSTEM, user_prompt, temperature=0.2)
    except ValueError as exc:
        raise HTTPException(status_code=502, detail=f"LLM parse error: {exc}")
    except Exception as exc:
        logger.exception("Ollama call failed")
        raise HTTPException(status_code=503, detail=f"Ollama unreachable: {exc}")

    return SummaryResponse(
        customer_id=req.customer_id,
        interaction_id=req.interaction_id,
        identified_intent=result.get("identified_intent", req.identified_intent or "UNKNOWN"),
        llm_summary=result.get("llm_summary", ""),
        outcome=result.get("outcome", "No action taken"),
        follow_up_required=bool(result.get("follow_up_required", False)),
    )
