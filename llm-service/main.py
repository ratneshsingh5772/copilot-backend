"""
Telecom Plan Advisor — Python FastAPI LLM Microservice
======================================================
Endpoints:
  GET  /health                   — liveness + Ollama connectivity
  POST /api/v1/intent            — classify customer query into structured intent
  POST /api/v1/recommendation    — recommend best plan given customer context
  POST /api/v1/billing/prorate   — calculate pro-rated billing amount
  POST /api/v1/summary           — summarise a completed copilot conversation
"""

import os
import json
import math
import logging
from contextlib import asynccontextmanager
from datetime import date, datetime
from typing import Any, Optional

import httpx
from dotenv import load_dotenv
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field

# ─── Configuration ────────────────────────────────────────────────────────────

load_dotenv()

OLLAMA_BASE_URL: str = os.getenv("OLLAMA_BASE_URL", "http://localhost:11434")
OLLAMA_MODEL: str    = os.getenv("OLLAMA_MODEL", "llama3.2")
PORT: int            = int(os.getenv("PORT", "8001"))

# LLM generation timeouts (seconds)
CONNECT_TIMEOUT = 5.0
GENERATE_TIMEOUT = 120.0
HEALTH_TIMEOUT   = 3.0

logging.basicConfig(level=logging.INFO, format="%(asctime)s  %(levelname)s  %(message)s")
log = logging.getLogger(__name__)

# ─── Pydantic Models ──────────────────────────────────────────────────────────

# ── /api/v1/intent ────────────────────────────────────────────────────────────

class IntentRequest(BaseModel):
    customer_id: str
    customer_query: str


class IntentResponse(BaseModel):
    customer_id: str
    identified_intent: str
    confidence: str
    reasoning: str


# ── /api/v1/recommendation ────────────────────────────────────────────────────

class PlanInfo(BaseModel):
    plan_id: int
    plan_name: str
    plan_type: str
    monthly_cost: float
    data_limit_gb: int
    description: str


class CustomerContext(BaseModel):
    customer_id: str
    name: str
    tenure_months: int
    current_plan: PlanInfo
    data_used_gb: float
    roaming_used_mb: float
    billing_cycle_date: str


class RecommendationRequest(BaseModel):
    customer_context: CustomerContext
    customer_query: str
    destination_country: Optional[str] = None
    available_plans: list[PlanInfo]


class PlanSummary(BaseModel):
    plan_id: int
    plan_name: str
    monthly_cost: float
    data_limit_gb: int
    key_benefits: list[str] = Field(default_factory=list)
    suitable_for: str


class RecommendationResponse(BaseModel):
    recommended_plan_id: int
    recommended_plan_name: str
    reasoning: str
    current_plan_summary: PlanSummary
    recommended_plan_summary: PlanSummary
    personalized_message: str


# ── /api/v1/billing/prorate ───────────────────────────────────────────────────

class BillingPlanInfo(BaseModel):
    plan_id: int
    plan_name: str
    plan_type: str
    monthly_cost: float
    data_limit_gb: int
    description: str


class ProrateRequest(BaseModel):
    customer_id: str
    billing_cycle_date: str          # "YYYY-MM-DD"
    change_date: str                 # "YYYY-MM-DD"
    current_plan: BillingPlanInfo
    new_plan: BillingPlanInfo
    applied_discount_percentage: float = 0.0


class ProrateResponse(BaseModel):
    customer_id: str
    old_plan_name: str
    new_plan_name: str
    days_remaining_in_cycle: int
    credit_for_unused_days: float
    charge_for_new_plan: float
    discount_applied: float
    prorated_billed_amount: float
    explanation: str


# ── /api/v1/summary ───────────────────────────────────────────────────────────

class ConversationMessage(BaseModel):
    role: str   # "user" | "assistant"
    content: str


class SummaryRequest(BaseModel):
    customer_id: str
    interaction_id: str
    identified_intent: str
    conversation_history: list[ConversationMessage]


class SummaryResponse(BaseModel):
    customer_id: str
    interaction_id: str
    identified_intent: str
    llm_summary: str
    outcome: str
    follow_up_required: bool


# ─── Ollama Client ────────────────────────────────────────────────────────────

async def ollama_generate(system_prompt: str, user_prompt: str) -> str:
    """Call Ollama /api/generate and return the response text."""
    payload = {
        "model": OLLAMA_MODEL,
        "system": system_prompt,
        "prompt": user_prompt,
        "stream": False,
        "options": {"temperature": 0.3, "num_predict": 1024},
    }
    url = f"{OLLAMA_BASE_URL}/api/generate"
    log.info("Calling Ollama: url=%s model=%s", url, OLLAMA_MODEL)

    try:
        async with httpx.AsyncClient(
            timeout=httpx.Timeout(connect=CONNECT_TIMEOUT, read=GENERATE_TIMEOUT, write=30.0, pool=5.0)
        ) as client:
            resp = await client.post(url, json=payload)
            resp.raise_for_status()
            data = resp.json()
            text = data.get("response", "").strip()
            if not text:
                raise ValueError(f"Empty response from Ollama: {data}")
            log.info("Ollama response length=%d", len(text))
            return text
    except httpx.ConnectError as e:
        log.error("Cannot reach Ollama at %s: %s", url, e)
        raise HTTPException(
            status_code=503,
            detail=f"Ollama is not reachable at {OLLAMA_BASE_URL}. "
                   f"Run 'ollama serve' and 'ollama pull {OLLAMA_MODEL}'."
        )
    except httpx.ReadTimeout:
        raise HTTPException(status_code=504, detail="Ollama timed out generating a response.")
    except Exception as e:
        log.error("Ollama error: %s", e)
        raise HTTPException(status_code=502, detail=f"LLM error: {e}")


async def ollama_is_reachable() -> bool:
    """Quick health ping to Ollama /api/tags."""
    try:
        async with httpx.AsyncClient(
            timeout=httpx.Timeout(connect=HEALTH_TIMEOUT, read=HEALTH_TIMEOUT, write=HEALTH_TIMEOUT, pool=HEALTH_TIMEOUT)
        ) as client:
            resp = await client.get(f"{OLLAMA_BASE_URL}/api/tags")
            return resp.status_code == 200
    except Exception:
        return False


def parse_json_from_llm(text: str) -> dict:
    """
    Extract the first JSON object from an LLM response.
    LLMs often wrap JSON in markdown fences — this strips them.
    """
    # Strip markdown code fences
    cleaned = text.strip()
    for fence in ("```json", "```"):
        if cleaned.startswith(fence):
            cleaned = cleaned[len(fence):]
    if cleaned.endswith("```"):
        cleaned = cleaned[:-3]
    cleaned = cleaned.strip()

    # Find first { ... }
    start = cleaned.find("{")
    end   = cleaned.rfind("}")
    if start == -1 or end == -1:
        raise ValueError(f"No JSON object found in LLM output: {text[:300]}")
    return json.loads(cleaned[start : end + 1])


# ─── FastAPI App ──────────────────────────────────────────────────────────────

@asynccontextmanager
async def lifespan(app: FastAPI):
    reachable = await ollama_is_reachable()
    status = "reachable" if reachable else "NOT reachable"
    log.info("Ollama (%s / %s) is %s at startup", OLLAMA_BASE_URL, OLLAMA_MODEL, status)
    yield


app = FastAPI(
    title="Telecom Plan Advisor — LLM Service",
    description=(
        "Python FastAPI microservice providing LLM-powered endpoints for the "
        "Telecom Plan Advisor copilot.\n\n"
        "**Base URL:** http://localhost:8001\n\n"
        "Requires a running Ollama server: `ollama serve` + `ollama pull llama3.2`"
    ),
    version="1.0.0",
    lifespan=lifespan,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)


# ─── Endpoints ────────────────────────────────────────────────────────────────

# 1. Health ───────────────────────────────────────────────────────────────────

@app.get("/health", tags=["Health"], summary="Service liveness + Ollama connectivity")
async def health():
    """
    Returns service status and whether the local Ollama LLM server is reachable.

    - `status`: **ok** | **degraded**
    - `ollama_model`: configured model name
    - `ollama_reachable`: boolean
    """
    reachable = await ollama_is_reachable()
    return {
        "status": "ok" if reachable else "degraded",
        "ollama_model": OLLAMA_MODEL,
        "ollama_reachable": reachable,
    }


# 2. Intent Detection ─────────────────────────────────────────────────────────

INTENT_SYSTEM_PROMPT = """\
You are an intent classification engine for a telecom customer service copilot.

Classify the customer's query into exactly ONE of these intents:
  TRAVEL_INQUIRY        — questions about roaming, international travel, or country-specific plans
  DATA_BOOSTER_REQUEST  — asking for a data top-up or booster pack
  PLAN_UPGRADE          — wanting to move to a higher-tier plan
  PLAN_DOWNGRADE        — wanting to move to a cheaper or lower-tier plan
  BILLING_QUERY         — questions about charges, bills, or invoices
  PROMOTION_INQUIRY     — questions about discounts, loyalty rewards, or offers
  GENERAL_INQUIRY       — anything that does not fit the above categories

You MUST respond with ONLY a valid JSON object — no prose, no markdown fences.
Schema:
{
  "identified_intent": "<INTENT>",
  "confidence": "HIGH" | "MEDIUM" | "LOW",
  "reasoning": "<one sentence explaining why>"
}
"""

@app.post("/api/v1/intent", response_model=IntentResponse, tags=["Intent Detection"])
async def detect_intent(req: IntentRequest):
    """
    Classifies a customer's free-form query into a structured intent using the local LLM.
    """
    raw = await ollama_generate(INTENT_SYSTEM_PROMPT, req.customer_query)

    try:
        data = parse_json_from_llm(raw)
    except Exception as e:
        log.error("Failed to parse intent JSON: %s | raw=%s", e, raw[:300])
        raise HTTPException(status_code=502, detail=f"LLM returned invalid JSON: {e}")

    return IntentResponse(
        customer_id=req.customer_id,
        identified_intent=data.get("identified_intent", "GENERAL_INQUIRY"),
        confidence=data.get("confidence", "LOW"),
        reasoning=data.get("reasoning", ""),
    )


# 3. Plan Recommendation ──────────────────────────────────────────────────────

def build_recommendation_prompt(req: RecommendationRequest) -> str:
    ctx = req.customer_context
    current = ctx.current_plan
    data_limit = "Unlimited" if current.data_limit_gb >= 9999 else f"{current.data_limit_gb} GB"

    plans_text = "\n".join(
        f"  - [ID:{p.plan_id}] {p.plan_name} | {p.plan_type} | ${p.monthly_cost}/month "
        f"| Data: {'Unlimited' if p.data_limit_gb >= 9999 else str(p.data_limit_gb) + ' GB'} "
        f"| {p.description}"
        for p in req.available_plans
    )

    destination = f"Destination country: {req.destination_country}" if req.destination_country else "No travel destination specified."

    return f"""\
Customer Profile:
  ID            : {ctx.customer_id}
  Name          : {ctx.name}
  Tenure        : {ctx.tenure_months} months
  Current Plan  : [{current.plan_id}] {current.plan_name} | ${current.monthly_cost}/month | Data: {data_limit}
  Data Used     : {ctx.data_used_gb} GB this cycle
  Roaming Used  : {ctx.roaming_used_mb} MB this cycle
  Billing Date  : {ctx.billing_cycle_date}
  {destination}

Customer Query:
  "{req.customer_query}"

Available Plans:
{plans_text}

Based on the above, respond with ONLY a valid JSON object (no markdown, no extra text):
{{
  "recommended_plan_id": <integer>,
  "recommended_plan_name": "<string>",
  "reasoning": "<2-3 sentence explanation referencing the customer's usage and query>",
  "current_plan_summary": {{
    "plan_id": <int>,
    "plan_name": "<string>",
    "monthly_cost": <float>,
    "data_limit_gb": <int>,
    "key_benefits": ["<benefit1>", "..."],
    "suitable_for": "<string>"
  }},
  "recommended_plan_summary": {{
    "plan_id": <int>,
    "plan_name": "<string>",
    "monthly_cost": <float>,
    "data_limit_gb": <int>,
    "key_benefits": ["<benefit1>", "..."],
    "suitable_for": "<string>"
  }},
  "personalized_message": "<friendly 1-2 sentence message to the customer>"
}}
"""

RECOMMENDATION_SYSTEM_PROMPT = """\
You are a helpful telecom plan advisor. Your job is to recommend the single best plan \
from a provided catalogue based on a customer's usage pattern, tenure, travel needs, and query.
Always prefer value-for-money. Suggest a downgrade if the customer is over-paying.
Respond ONLY with a valid JSON object matching the schema provided — no markdown, no prose.
"""

@app.post("/api/v1/recommendation", response_model=RecommendationResponse, tags=["Plan Recommendation"])
async def recommend_plan(req: RecommendationRequest):
    """
    Recommends the best telecom plan for a customer based on their context and query.
    """
    user_prompt = build_recommendation_prompt(req)
    raw = await ollama_generate(RECOMMENDATION_SYSTEM_PROMPT, user_prompt)

    try:
        data = parse_json_from_llm(raw)
    except Exception as e:
        log.error("Failed to parse recommendation JSON: %s | raw=%s", e, raw[:300])
        raise HTTPException(status_code=502, detail=f"LLM returned invalid JSON: {e}")

    # Look up the recommended plan from available_plans to fill in summary fields reliably
    rec_id = int(data.get("recommended_plan_id", 0))
    rec_plan = next((p for p in req.available_plans if p.plan_id == rec_id), req.available_plans[0])
    cur_plan = req.customer_context.current_plan

    def make_summary(p: PlanInfo, llm_summary: dict) -> PlanSummary:
        return PlanSummary(
            plan_id=p.plan_id,
            plan_name=p.plan_name,
            monthly_cost=p.monthly_cost,
            data_limit_gb=p.data_limit_gb,
            key_benefits=llm_summary.get("key_benefits", []),
            suitable_for=llm_summary.get("suitable_for", ""),
        )

    return RecommendationResponse(
        recommended_plan_id=rec_plan.plan_id,
        recommended_plan_name=rec_plan.plan_name,
        reasoning=data.get("reasoning", ""),
        current_plan_summary=make_summary(cur_plan, data.get("current_plan_summary", {})),
        recommended_plan_summary=make_summary(rec_plan, data.get("recommended_plan_summary", {})),
        personalized_message=data.get("personalized_message", ""),
    )


# 4. Pro-Rated Billing ────────────────────────────────────────────────────────

@app.post("/api/v1/billing/prorate", response_model=ProrateResponse, tags=["Billing"])
async def prorate_billing(req: ProrateRequest):
    """
    Calculates the pro-rated billing amount when a customer switches plans mid-cycle.

    Formula:
      days_remaining = 30 - (change_date - billing_cycle_date).days
      credit         = current_plan.monthly_cost × (days_remaining / 30)
      charge         = new_plan.monthly_cost     × (days_remaining / 30)
      discount       = charge × (applied_discount_percentage / 100)
      total_billed   = charge - credit - discount
    """
    billing_dt = date.fromisoformat(req.billing_cycle_date)
    change_dt  = date.fromisoformat(req.change_date)

    days_elapsed   = (change_dt - billing_dt).days
    days_remaining = max(0, 30 - days_elapsed)

    credit   = round(req.current_plan.monthly_cost * (days_remaining / 30), 2)
    charge   = round(req.new_plan.monthly_cost     * (days_remaining / 30), 2)
    discount = round(charge * (req.applied_discount_percentage / 100), 2)
    total    = round(charge - credit - discount, 2)

    explanation = (
        f"Switching from {req.current_plan.plan_name} to {req.new_plan.plan_name} "
        f"with {days_remaining} days remaining in the billing cycle. "
        f"Credit for unused days: ${credit:.2f}. "
        f"Charge for new plan: ${charge:.2f}. "
        f"Discount applied ({req.applied_discount_percentage:.0f}%): ${discount:.2f}. "
        f"Total billed: ${total:.2f}."
    )

    return ProrateResponse(
        customer_id=req.customer_id,
        old_plan_name=req.current_plan.plan_name,
        new_plan_name=req.new_plan.plan_name,
        days_remaining_in_cycle=days_remaining,
        credit_for_unused_days=credit,
        charge_for_new_plan=charge,
        discount_applied=discount,
        prorated_billed_amount=total,
        explanation=explanation,
    )


# 5. Conversation Summary ─────────────────────────────────────────────────────

SUMMARY_SYSTEM_PROMPT = """\
You are an audit summarisation engine for a telecom customer service copilot.
Given a conversation history, produce a concise structured summary for audit storage.

Respond ONLY with a valid JSON object — no markdown fences, no prose:
{
  "llm_summary": "<2-3 sentence neutral summary of what happened in the conversation>",
  "outcome": "<short outcome label, e.g. 'Plan upgraded to Premium Unlimited', 'Billing query resolved', 'Escalated to human agent'>",
  "follow_up_required": <true | false>
}

Set follow_up_required to true ONLY if the issue was NOT resolved and a human needs to follow up.
"""

def format_conversation(messages: list[ConversationMessage]) -> str:
    return "\n".join(f"{m.role.upper()}: {m.content}" for m in messages)

@app.post("/api/v1/summary", response_model=SummaryResponse, tags=["Conversation Summary"])
async def summarise_conversation(req: SummaryRequest):
    """
    Generates a structured audit summary of a completed copilot conversation
    for storage in the Copilot_Interactions table.
    """
    conversation_text = format_conversation(req.conversation_history)
    user_prompt = (
        f"Intent identified during this interaction: {req.identified_intent}\n\n"
        f"Conversation:\n{conversation_text}\n\n"
        "Summarise the above conversation."
    )

    raw = await ollama_generate(SUMMARY_SYSTEM_PROMPT, user_prompt)

    try:
        data = parse_json_from_llm(raw)
    except Exception as e:
        log.error("Failed to parse summary JSON: %s | raw=%s", e, raw[:300])
        raise HTTPException(status_code=502, detail=f"LLM returned invalid JSON: {e}")

    return SummaryResponse(
        customer_id=req.customer_id,
        interaction_id=req.interaction_id,
        identified_intent=req.identified_intent,
        llm_summary=data.get("llm_summary", ""),
        outcome=data.get("outcome", ""),
        follow_up_required=bool(data.get("follow_up_required", False)),
    )


# ─── Entry Point ──────────────────────────────────────────────────────────────

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="0.0.0.0", port=PORT, reload=True)

