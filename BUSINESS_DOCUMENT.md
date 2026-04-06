# Telecom Plan Advisor Copilot — Business Document

**Document Type:** Business Purpose & Functional Overview  
**Version:** 1.0  
**Date:** April 4, 2026  
**Status:** Active Development  

---

## 1. Executive Summary

The **Telecom Plan Advisor Copilot** is an AI-powered customer self-service platform that helps telecom subscribers discover, compare, and switch to the most suitable mobile plan through a natural language conversational interface. By combining a structured customer data backend with a locally-hosted Large Language Model (LLM), the system delivers personalised, context-aware plan recommendations without the cost or privacy risk of third-party AI cloud services.

---

## 2. Business Problem

Telecom customers typically face the following pain points:

| Problem | Business Impact |
|---|---|
| Large, complex plan catalogues are hard to navigate | High call-centre volume; customer frustration |
| Agents cannot instantly factor in a customer's usage, tenure, and travel plans | Suboptimal plan recommendations; churn risk |
| Customers are unaware of loyalty discounts and promotions they qualify for | Missed upsell/retention opportunities |
| Plan changes require human agents or clunky self-service portals | High operational cost; slow resolution |
| No audit trail of AI advice given to customers | Compliance and accountability gaps |

The Copilot directly addresses all five problems through a single intelligent API.

---

## 3. Solution Overview

The system is a **two-tier microservice architecture**:

```
Customer / Front-End App
        │
        ▼
┌─────────────────────────────────┐   REST / JWT
│  Spring Boot Backend (port 8081)│◄──────────────  Client Apps / Postman
│  Java 21 · MySQL · Spring JPA  │
│  - Customer & Plan management   │
│  - JWT Authentication           │
│  - Promotion & Transaction ledger│
│  - Copilot Interaction audit log │
└──────────────┬──────────────────┘
               │ HTTP (JSON)
               ▼
┌─────────────────────────────────┐
│  LLM Microservice (port 8001)   │
│  Python 3.10 · FastAPI · Ollama │
│  - Intent classification        │
│  - Plan recommendation          │
│  - Pro-rated billing calculation│
│  - Conversation summarisation   │
└──────────────┬──────────────────┘
               │ Ollama API
               ▼
        Local LLM (llama3.2)
```

All AI inference runs **on-premise** via [Ollama](https://ollama.com), ensuring customer data never leaves the organisation's infrastructure.

---

## 4. Key Capabilities

### 4.1 AI-Powered Plan Recommendation
A customer submits a free-text query (e.g., *"I'm travelling to the US for 3 weeks, what plan should I get?"*). The system:

1. Fetches the customer's profile, current plan, and live usage data.
2. Loads the full plan catalogue (base plans, travel add-ons, data boosters).
3. Identifies applicable loyalty/seasonal promotions based on customer tenure.
4. Sends a structured JSON context to the LLM microservice.
5. Returns a personalised recommendation with plan comparison, reasoning, and the customer's pro-rated switching cost.

### 4.2 Plan Catalogue Management
Full CRUD management of the plan catalogue, supporting:
- **BASE_PLAN** — monthly subscriptions (Starter 2 GB → Ultimate Unlimited)
- **TRAVEL_ADD_ON** — regional roaming packs (US, Europe, Asia, Global)
- **DATA_BOOSTER** — instant top-up add-ons (1 GB, 5 GB, 10 GB)

Catalogue search supports paginated, filtered queries by plan type, cost range, data allowance, and active status.

### 4.3 Promotion & Loyalty Engine
The system tracks customer tenure and automatically surfaces applicable discounts:

| Promotion | Discount | Minimum Tenure |
|---|---|---|
| Welcome Offer | 10 % | 0 months (all customers) |
| 6-Month Loyalty Reward | 15 % | 6 months |
| 1-Year Loyalty Bonus | 20 % | 12 months |
| 2-Year Champion Deal | 25 % | 24 months |
| Seasonal Summer Sale | 12 % | 0 months |

### 4.4 Pro-Rated Billing Calculation
When a customer switches plan mid-billing-cycle, the LLM service calculates:
- Credit for unused days on the old plan.
- Pro-rated charge for the new plan.
- Any applied promotional discount.
- A plain-English explanation of the billed amount.

### 4.5 Intent Classification
Before routing a query, the system classifies customer intent into structured categories:

`PLAN_UPGRADE` · `PLAN_DOWNGRADE` · `TRAVEL_INQUIRY` · `DATA_BOOSTER_REQUEST` · `BILLING_QUERY` · `CURRENT_PLAN_INFO` · `PROMOTION_INQUIRY` · `GENERAL_FAQ`

### 4.6 Conversation Audit Log
Every AI interaction is stored as a `CopilotInteraction` record, capturing:
- Customer identifier and timestamp
- Identified intent
- LLM-generated summary
- Full recommendation text

This provides a compliance-ready audit trail of all AI advice delivered to customers.

### 4.7 Secure Authentication
All advisor and management endpoints are protected with **JWT Bearer token** authentication. Customers authenticate via email/password; tokens expire after 24 hours.

---

## 5. Technical Stack

| Layer | Technology |
|---|---|
| Backend API | Java 21, Spring Boot 3.4, Spring Data JPA |
| Database | MySQL 8.x |
| ORM Mapping | Hibernate + MapStruct |
| Security | Spring Security + JJWT (HS384) |
| API Documentation | SpringDoc OpenAPI / Swagger UI (`/swagger-ui.html`) |
| LLM Microservice | Python 3.10, FastAPI, Uvicorn |
| AI / LLM Runtime | Ollama (local) — model: `llama3.2` |
| Data Validation | Pydantic (Python) + Jakarta Bean Validation (Java) |

---

## 6. Domain Data Model

```
plans_catalog          promotions
     │                      │
     │ (current_plan_id)     │ (promo_applied_id)
     ▼                      ▼
  customers ──────► plan_transactions
     │
     ├──► customer_usage         (live data consumption per billing cycle)
     └──► copilot_interactions   (AI advice audit log)
```

**Key Entities:**

- **`plans_catalog`** — 12 plans across three types; `data_limit_gb = 9999` denotes unlimited.
- **`customers`** — subscriber profile including tenure, billing cycle date, and current plan FK.
- **`customer_usage`** — GB consumed and roaming MB used in the current billing period.
- **`plan_transactions`** — immutable ledger of every plan change with pro-rated billing amount.
- **`promotions`** — tenant-qualified discounts with `is_active` flag.
- **`copilot_interactions`** — timestamped record of every AI advisor session.

---

## 7. API Surface

The Spring Boot backend exposes the following REST API groups (base: `http://localhost:8081/api/v1`):

| Group | Prefix | Description |
|---|---|---|
| Authentication | `/auth` | Register, login (returns JWT) |
| Advisor (AI) | `/advisor` | Natural language plan recommendation |
| Plans Catalogue | `/plans` | CRUD + paginated search |
| Customers | `/customers` | Customer profile management |
| Customer Usage | `/usage` | View and update data usage |
| Plan Transactions | `/transactions` | Plan change history |
| Promotions | `/promotions` | Promotion management |
| Copilot Interactions | `/interactions` | AI session audit log |
| Health | `/health` | Service liveness check |

The Python LLM service exposes a separate OpenAPI UI at `http://localhost:8001/docs`.

---

## 8. Business Value

| Benefit | Description |
|---|---|
| **Reduced Call-Centre Load** | Customers self-serve complex plan queries 24/7 without agent involvement |
| **Increased Plan Upsell** | Context-aware recommendations surface higher-value plans at the right moment |
| **Improved Retention** | Automatic loyalty promotion surfacing reduces churn at tenure milestones |
| **Regulatory Compliance** | Full audit trail of every AI recommendation made to customers |
| **Data Privacy** | All AI inference is on-premise; no customer data is sent to external cloud LLMs |
| **Cost Efficiency** | Open-source LLM stack (Ollama + llama3.2) with zero per-query API fees |

---

## 9. Intended Users

| User Type | Interaction |
|---|---|
| **Telecom Subscribers** | Query the advisor via a front-end app (mobile / web) |
| **Customer Service Agents** | Use the API via internal tooling to assist customers on calls |
| **Product / Pricing Teams** | Manage plan catalogue and promotions via admin endpoints |
| **Compliance / Audit Teams** | Review the copilot interaction log for regulatory purposes |
| **Developers / QA** | Explore APIs via Swagger UI; test via Postman collections included in the repo |

---

## 10. Running the System

### Prerequisites
- Java 21, Maven
- MySQL 8.x (database: `copilot_db`)
- Python 3.10+, pip
- [Ollama](https://ollama.com) installed with `llama3.2` model pulled

### Start the LLM Microservice
```bash
cd llm-service
pip install -r requirements.txt
uvicorn main:app --host 0.0.0.0 --port 8001 --reload
```

### Start the Spring Boot Backend
```bash
# Seed the database first (once)
mysql -u root -p copilot_db < src/main/resources/db/seed_data.sql

JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./mvnw spring-boot:run
```

### Verify
- Swagger UI: [http://localhost:8081/swagger-ui.html](http://localhost:8081/swagger-ui.html)
- LLM Service Docs: [http://localhost:8001/docs](http://localhost:8001/docs)
- Health: `GET http://localhost:8081/api/v1/health`

---

*Document maintained alongside source code in `/home/ratnesh/Documents/copilot-backend`.*

