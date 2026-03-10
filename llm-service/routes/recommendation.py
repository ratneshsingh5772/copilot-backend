"""
POST /api/v1/recommendation
Analyses customer usage data and available plans, returns a personalised
plan recommendation with a side-by-side comparison.
"""

import logging

from fastapi import APIRouter, HTTPException

from models.schemas import (
    RecommendationRequest,
    RecommendationResponse,
    PlanComparisonItem,
)
from prompts.templates import RECOMMENDATION_SYSTEM, recommendation_user_prompt
from services.ollama_service import chat_json

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/api/v1", tags=["Plan Recommendation"])


@router.post(
    "/recommendation",
    summary="Generate personalised plan recommendation",
    description=(
        "Passes the customer's usage context, current plan, and a list of available "
        "plans to the local Ollama model. Returns raw plan comparison and reasoning."
    ),
    responses={
        502: {"description": "LLM returned non-JSON output or response is missing required fields"},
        503: {"description": "Ollama service is unreachable"},
    },
)
async def get_recommendation(req: RecommendationRequest) -> RecommendationResponse:
    user_prompt = recommendation_user_prompt(req)

    try:
        result = await chat_json(RECOMMENDATION_SYSTEM, user_prompt, temperature=0.3)
    except ValueError as exc:
        raise HTTPException(status_code=502, detail=f"LLM parse error: {exc}")
    except Exception as exc:
        logger.exception("Ollama call failed")
        raise HTTPException(status_code=503, detail=f"Ollama unreachable: {exc}")

    ctx = req.customer_context
    try:
        return RecommendationResponse(
            recommended_plan_id=int(result["recommended_plan_id"]),
            recommended_plan_name=result["recommended_plan_name"],
            reasoning=result["reasoning"],
            current_plan_summary=PlanComparisonItem(
                plan_id=ctx.current_plan.plan_id,
                plan_name=result.get("current_plan_name", ctx.current_plan.plan_name),
                monthly_cost=result.get("current_monthly_cost", ctx.current_plan.monthly_cost),
                data_limit_gb=result.get("current_data_gb", ctx.current_plan.data_limit_gb),
                key_benefits=[],
                suitable_for=result.get("current_suitable_for", ""),
            ),
            recommended_plan_summary=PlanComparisonItem(
                plan_id=int(result["recommended_plan_id"]),
                plan_name=result["recommended_plan_name"],
                monthly_cost=result.get("rec_monthly_cost", 0.0),
                data_limit_gb=result.get("rec_data_gb", 0),
                key_benefits=[],
                suitable_for=result.get("rec_suitable_for", ""),
            ),
            personalized_message=result.get("personalized_message", ""),
        )
    except KeyError as exc:
        raise HTTPException(status_code=502, detail=f"Missing field in LLM response: {exc}")
