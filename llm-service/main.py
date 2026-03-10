"""
main.py — FastAPI entry point for the Telecom Plan Advisor LLM Service.

Exposes four endpoints consumed by the Java Spring Boot backend (via RestClient):
  POST /api/v1/intent            → intent classification
  POST /api/v1/recommendation    → personalised plan recommendation
  POST /api/v1/billing/prorate   → pro-rated billing calculation
  POST /api/v1/summary           → conversation summary for audit logging
  GET  /health                   → liveness check
"""

import logging

import uvicorn
from fastapi import FastAPI
from fastapi.responses import JSONResponse

from config import settings
from models.schemas import HealthResponse
from routes import billing, intent, recommendation, summary
from services.ollama_service import check_ollama_health

# ── Logging ────────────────────────────────────────────────────────────────────
logging.basicConfig(
    level=settings.log_level.upper(),
    format="%(asctime)s | %(levelname)-8s | %(name)s - %(message)s",
)
logger = logging.getLogger(__name__)

# ── App ────────────────────────────────────────────────────────────────────────
app = FastAPI(
    title="Telecom Plan Advisor LLM Service",
    description=(
        "Python FastAPI microservice that wraps a local Ollama model to provide "
        "telecom-specific AI capabilities to the Java Spring Boot Copilot backend."
    ),
    version="1.0.0",
    docs_url="/docs",
    redoc_url="/redoc",
)

# ── Routers ────────────────────────────────────────────────────────────────────
app.include_router(intent.router)
app.include_router(recommendation.router)
app.include_router(billing.router)
app.include_router(summary.router)


# ── Health ─────────────────────────────────────────────────────────────────────
@app.get(
    "/health",
    response_model=HealthResponse,
    tags=["Health"],
    summary="Service liveness + Ollama connectivity check",
)
async def health() -> HealthResponse:
    reachable = await check_ollama_health()
    status = "ok" if reachable else "degraded"
    return HealthResponse(
        status=status,
        ollama_model=settings.ollama_model,
        ollama_reachable=reachable,
    )


# ── Global exception handler ───────────────────────────────────────────────────
@app.exception_handler(Exception)
async def global_exception_handler(request, exc):
    logger.exception("Unhandled exception for %s", request.url)
    return JSONResponse(status_code=500, content={"detail": "Internal server error"})


# ── Dev runner ─────────────────────────────────────────────────────────────────
if __name__ == "__main__":
    uvicorn.run(
        "main:app",
        host=settings.service_host,
        port=settings.service_port,
        reload=True,
        log_level=settings.log_level,
    )
