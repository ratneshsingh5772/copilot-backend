"""
POST /api/v1/billing/prorate
Calculates the exact pro-rated charge when a customer switches plans
mid-billing-cycle, including any promotional discount.
"""

import logging
from datetime import date

from fastapi import APIRouter, HTTPException

from models.schemas import BillingCalcRequest, BillingCalcResponse
from prompts.templates import BILLING_SYSTEM, billing_user_prompt
from services.ollama_service import chat_json

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/api/v1/billing", tags=["Billing Calculation"])


def _python_fallback(req: BillingCalcRequest) -> BillingCalcResponse:
    """
    Deterministic fallback calculation executed locally in Python.
    Used when the LLM output cannot be trusted (e.g. parse error).
    Guarantees the Java layer always receives a valid billing result.
    """
    cycle_start = date.fromisoformat(req.billing_cycle_date)
    change      = date.fromisoformat(req.change_date)
    days_remaining = max(0, 30 - (change - cycle_start).days)

    credit   = round(req.current_plan.monthly_cost * (days_remaining / 30), 2)
    charge   = round(req.new_plan.monthly_cost     * (days_remaining / 30), 2)
    discount = round(charge * ((req.applied_discount_percentage or 0) / 100), 2)
    total    = round(max(0.0, charge - credit - discount), 2)

    return BillingCalcResponse(
        customer_id=req.customer_id,
        old_plan_name=req.current_plan.plan_name,
        new_plan_name=req.new_plan.plan_name,
        days_remaining_in_cycle=days_remaining,
        credit_for_unused_days=credit,
        charge_for_new_plan=charge,
        discount_applied=discount,
        prorated_billed_amount=total,
        explanation=(
            f"Switching from {req.current_plan.plan_name} to {req.new_plan.plan_name} "
            f"with {days_remaining} days remaining. Credit ${credit}, new charge ${charge}, "
            f"discount ${discount}. Total billed: ${total}."
        ),
    )


@router.post(
    "/prorate",
    response_model=BillingCalcResponse,
    summary="Calculate pro-rated bill impact of a plan switch",
    description=(
        "Asks the Ollama model to compute pro-rated billing; automatically falls back "
        "to deterministic Python arithmetic if the LLM output is unparseable."
    ),
)
async def calculate_prorate(req: BillingCalcRequest) -> BillingCalcResponse:
    user_prompt = billing_user_prompt(req)

    try:
        result = await chat_json(BILLING_SYSTEM, user_prompt, temperature=0.0)
    except Exception as exc:
        logger.warning("Ollama unavailable — using Python fallback. Reason: %s", exc)
        return _python_fallback(req)

    try:
        return BillingCalcResponse(
            customer_id=req.customer_id,
            old_plan_name=req.current_plan.plan_name,
            new_plan_name=req.new_plan.plan_name,
            days_remaining_in_cycle=int(result["days_remaining_in_cycle"]),
            credit_for_unused_days=float(result["credit_for_unused_days"]),
            charge_for_new_plan=float(result["charge_for_new_plan"]),
            discount_applied=float(result["discount_applied"]),
            prorated_billed_amount=float(result["prorated_billed_amount"]),
            explanation=result.get("explanation", ""),
        )
    except (KeyError, ValueError):
        logger.warning("LLM billing output malformed — using Python fallback")
        return _python_fallback(req)
