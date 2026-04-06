"""
Prompt templates for all Telecom Copilot LLM tasks.
Each function returns a fully-formed system+user prompt pair.
"""

from models.schemas import (
    IntentRequest,
    RecommendationRequest,
    BillingCalcRequest,
    SummaryRequest,
)


# ── Intent Detection ───────────────────────────────────────────────────────────

INTENT_SYSTEM = """You are an intent classification engine for a telecom self-service copilot.
Analyse the customer's message and return ONLY valid JSON — no markdown, no explanation outside the JSON.

Allowed intent values:
PLAN_UPGRADE | PLAN_DOWNGRADE | TRAVEL_INQUIRY | DATA_BOOSTER_REQUEST |
BILLING_QUERY | CURRENT_PLAN_INFO | PROMOTION_INQUIRY | GENERAL_FAQ | UNKNOWN

Required JSON format:
{
  "identified_intent": "<INTENT>",
  "confidence": "HIGH" | "MEDIUM" | "LOW",
  "reasoning": "<one sentence>"
}"""


def intent_user_prompt(req: IntentRequest) -> str:
    return f'Customer message: "{req.customer_query}"'


# ── Plan Recommendation ────────────────────────────────────────────────────────

RECOMMENDATION_SYSTEM = """You are an expert telecom plan advisor helping customers choose the best mobile plan.

TASK: Analyse the customer profile, their query, and the available plans list, then respond with a personalised plan recommendation.

RULES:
1. You MUST choose a real plan from the "Available Plans" list — use its exact plan_id (integer) and plan_name (string).
2. If the customer is asking about their current plan or is satisfied, recommend their CURRENT plan (return its plan_id and plan_name).
3. If the customer asks about travel or roaming, recommend the most suitable TRAVEL_ADD_ON plan.
4. If the customer needs more data, recommend a DATA_BOOSTER or a higher BASE_PLAN.
5. Always populate every field — never leave a field empty or use 0 for plan_id unless plan_id is genuinely 0.
6. personalized_message must be a warm, helpful 2-3 sentence response directly addressing the customer's query.
7. reasoning must explain why this specific plan was chosen.
8. Output ONLY a JSON object — no markdown fences, no text before or after the JSON.

Required JSON format (fill every field with real values from the available plans):
{
  "recommended_plan_id": <integer — plan_id from the available plans list>,
  "recommended_plan_name": "<exact plan_name from the available plans list>",
  "reasoning": "<2-3 sentences explaining why this plan suits the customer>",
  "current_plan_id": <integer — the customer's current plan_id>,
  "current_plan_name": "<the customer's current plan_name>",
  "current_monthly_cost": <float — current plan monthly cost>,
  "current_data_gb": <integer — current plan data_limit_gb>,
  "current_suitable_for": "<one sentence about who the current plan suits>",
  "rec_monthly_cost": <float — recommended plan monthly cost>,
  "rec_data_gb": <integer — recommended plan data_limit_gb>,
  "rec_suitable_for": "<one sentence about who the recommended plan suits>",
  "personalized_message": "<warm 2-3 sentence response directly addressing the customer's question>"
}"""


def recommendation_user_prompt(req: RecommendationRequest) -> str:
    ctx = req.customer_context
    plans_text = "\n".join(
        f"  - [plan_id={p.plan_id}] {p.plan_name} ({p.plan_type.value}): "
        f"${p.monthly_cost}/mo, {p.data_limit_gb}GB data — {p.description}"
        for p in req.available_plans
    )
    destination = f"\nTravel destination: {req.destination_country}" if req.destination_country else ""
    data_pct = round((ctx.data_used_gb / ctx.current_plan.data_limit_gb) * 100, 1) \
        if ctx.current_plan.data_limit_gb and ctx.current_plan.data_limit_gb < 9999 else None
    usage_note = f" ({data_pct}% of allowance used)" if data_pct is not None else " (unlimited plan)"
    return f"""Customer Profile:
  Name: {ctx.name} | Tenure: {ctx.tenure_months} months
  Current Plan: [plan_id={ctx.current_plan.plan_id}] {ctx.current_plan.plan_name} — ${ctx.current_plan.monthly_cost}/mo, {ctx.current_plan.data_limit_gb}GB
  Data used this cycle: {ctx.data_used_gb} GB{usage_note}
  Roaming used: {ctx.roaming_used_mb} MB
  Billing cycle start: {ctx.billing_cycle_date}{destination}

Customer Query: "{req.customer_query}"

Available Plans (choose recommended_plan_id and recommended_plan_name ONLY from this list):
{plans_text}

Remember: return the current plan as recommended if the customer only wants info about their plan or is already on the best plan for their needs."""


# ── Pro-Rated Billing Calculation ─────────────────────────────────────────────

BILLING_SYSTEM = """You are a precise telecom billing engine.
Calculate the pro-rated charge when a customer switches plans mid-billing-cycle.
Billing cycle is always 30 days. Use the formula:

  days_remaining = 30 - (change_date - cycle_start).days
  credit          = old_monthly_cost * (days_remaining / 30)
  charge          = new_monthly_cost * (days_remaining / 30)
  discount        = charge * (discount_pct / 100)
  prorated_total  = charge - credit - discount   (minimum 0)

Return ONLY valid JSON — no markdown, no extra text.

Required JSON format:
{{
  "days_remaining_in_cycle": <int>,
  "credit_for_unused_days": <float, 2 decimal places>,
  "charge_for_new_plan": <float, 2 decimal places>,
  "discount_applied": <float, 2 decimal places>,
  "prorated_billed_amount": <float, 2 decimal places>,
  "explanation": "<plain-English 2-3 sentence summary>"
}}"""


def billing_user_prompt(req: BillingCalcRequest) -> str:
    return f"""Billing cycle start: {req.billing_cycle_date}
Plan change date:    {req.change_date}
Old plan: {req.current_plan.plan_name} — ${req.current_plan.monthly_cost}/month
New plan: {req.new_plan.plan_name}     — ${req.new_plan.monthly_cost}/month
Discount percentage: {req.applied_discount_percentage}%"""


# ── Conversation Summary ───────────────────────────────────────────────────────

SUMMARY_SYSTEM = """You are a telecom copilot interaction summariser.
Read the full conversation and produce a concise summary for audit logs.
Return ONLY valid JSON — no markdown, no extra text.

Required JSON format:
{{
  "identified_intent": "<INTENT_TYPE string>",
  "llm_summary": "<2-4 sentence summary of what happened>",
  "outcome": "<e.g. 'Plan upgraded to Premium Unlimited', 'FAQ answered', 'No action taken'>",
  "follow_up_required": <true | false>
}}"""


def summary_user_prompt(req: SummaryRequest) -> str:
    history = "\n".join(
        f"  [{m.role.upper()}]: {m.content}" for m in req.conversation_history
    )
    intent_hint = f"\nPre-identified intent hint: {req.identified_intent}" if req.identified_intent else ""
    return f"""Customer ID: {req.customer_id}
Interaction ID: {req.interaction_id}{intent_hint}

Conversation:
{history}"""
