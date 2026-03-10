from pydantic import BaseModel, Field
from typing import Optional
from enum import Enum


# ── Enums ──────────────────────────────────────────────────────────────────────

class PlanType(str, Enum):
    BASE_PLAN = "BASE_PLAN"
    TRAVEL_ADD_ON = "TRAVEL_ADD_ON"
    DATA_BOOSTER = "DATA_BOOSTER"


class IntentType(str, Enum):
    PLAN_UPGRADE = "PLAN_UPGRADE"
    PLAN_DOWNGRADE = "PLAN_DOWNGRADE"
    TRAVEL_INQUIRY = "TRAVEL_INQUIRY"
    DATA_BOOSTER_REQUEST = "DATA_BOOSTER_REQUEST"
    BILLING_QUERY = "BILLING_QUERY"
    CURRENT_PLAN_INFO = "CURRENT_PLAN_INFO"
    PROMOTION_INQUIRY = "PROMOTION_INQUIRY"
    GENERAL_FAQ = "GENERAL_FAQ"
    UNKNOWN = "UNKNOWN"


# ── Shared sub-models ──────────────────────────────────────────────────────────

class PlanInfo(BaseModel):
    plan_id: int
    plan_name: str
    plan_type: PlanType
    monthly_cost: float
    data_limit_gb: int             # 9999 = unlimited
    description: str


class CustomerContext(BaseModel):
    customer_id: str
    name: str
    tenure_months: int
    current_plan: PlanInfo
    data_used_gb: float
    roaming_used_mb: float
    billing_cycle_date: str        # ISO date: "2026-03-01"


# ── Intent Detection ───────────────────────────────────────────────────────────

class IntentRequest(BaseModel):
    customer_query: str = Field(..., min_length=3, description="Raw free-form text from the customer")
    customer_id: Optional[str] = None


class IntentResponse(BaseModel):
    customer_id: Optional[str]
    identified_intent: IntentType
    confidence: str                # HIGH / MEDIUM / LOW
    reasoning: str


# ── Plan Recommendation ────────────────────────────────────────────────────────

class RecommendationRequest(BaseModel):
    customer_context: CustomerContext
    customer_query: str
    available_plans: list[PlanInfo]
    destination_country: Optional[str] = None


class PlanComparisonItem(BaseModel):
    plan_id: int
    plan_name: str
    monthly_cost: float
    data_limit_gb: int
    key_benefits: list[str]
    suitable_for: str


class RecommendationResponse(BaseModel):
    recommended_plan_id: int
    recommended_plan_name: str
    reasoning: str
    current_plan_summary: PlanComparisonItem
    recommended_plan_summary: PlanComparisonItem
    personalized_message: str


# ── Pro-Rated Billing Calculation ─────────────────────────────────────────────

class BillingCalcRequest(BaseModel):
    customer_id: str
    billing_cycle_date: str        # "2026-03-01"  — start of current cycle
    current_plan: PlanInfo
    new_plan: PlanInfo
    change_date: str               # "2026-03-10"  — date of switch
    applied_discount_percentage: Optional[int] = 0


class BillingCalcResponse(BaseModel):
    customer_id: str
    old_plan_name: str
    new_plan_name: str
    days_remaining_in_cycle: int
    credit_for_unused_days: float
    charge_for_new_plan: float
    discount_applied: float
    prorated_billed_amount: float
    explanation: str


# ── Conversation Summary ───────────────────────────────────────────────────────

class Message(BaseModel):
    role: str                      # "user" or "assistant"
    content: str


class SummaryRequest(BaseModel):
    customer_id: str
    interaction_id: str
    conversation_history: list[Message]
    identified_intent: Optional[str] = None


class SummaryResponse(BaseModel):
    customer_id: str
    interaction_id: str
    identified_intent: str
    llm_summary: str
    outcome: str                   # e.g. "Plan upgraded", "FAQ answered", "No action taken"
    follow_up_required: bool


# ── Health ─────────────────────────────────────────────────────────────────────

class HealthResponse(BaseModel):
    status: str
    ollama_model: str
    ollama_reachable: bool
