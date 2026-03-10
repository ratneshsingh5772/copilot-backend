package com.telecom.copilot_backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.telecom.copilot_backend.dto.AdvisorRequest;
import com.telecom.copilot_backend.dto.AdvisorResponse;
import com.telecom.copilot_backend.dto.PromotionDto;
import com.telecom.copilot_backend.entity.Customer;
import com.telecom.copilot_backend.entity.CustomerUsage;
import com.telecom.copilot_backend.entity.PlanCatalog;
import com.telecom.copilot_backend.repository.PlanCatalogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AdvisorService {

    private final OllamaRestClient ollamaRestClient;
    private final CustomerService customerService;
    private final PlanService planService;
    private final PromotionService promotionService;
    private final CustomerUsageService customerUsageService;
    private final CopilotInteractionService copilotInteractionService;
    private final PlanTransactionService planTransactionService;
    private final PlanCatalogRepository planCatalogRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Core advisor flow:
     * 1. Load customer + live usage data.
     * 2. Fetch eligible promotions.
     * 3. Calculate pro-rated cost for any potential upgrade.
     * 4. Build a RecommendationRequest payload matching the FastAPI LLM service schema.
     * 5. Delegate to OllamaRestClient → FastAPI (localhost:8001) → Ollama.
     * 6. Detect intent from the customer's prompt.
     * 7. Log the interaction to copilot_interactions for auditing.
     */
    public AdvisorResponse advise(AdvisorRequest request) {
        String interactionId = UUID.randomUUID().toString();
        Customer customer = customerService.findEntityByCustomerId(request.getCustomerId());

        // --- Usage data ---
        CustomerUsage usage = customerUsageService.findCurrentUsageEntity(customer);

        // --- Eligible promotions ---
        List<PromotionDto> eligiblePromos = promotionService.getEligiblePromotions(customer.getTenureMonths());

        // --- Pro-rated cost ---
        BigDecimal proRated = BigDecimal.ZERO;
        if (customer.getCurrentPlan() != null) {
            proRated = planTransactionService.calculateProRatedAmount(customer, customer.getCurrentPlan());
        }

        // --- All available plans from the catalogue ---
        List<PlanCatalog> allPlans = planCatalogRepository.findAll();

        // --- Build FastAPI RecommendationRequest JSON payload ---
        String payload = buildRecommendationPayload(customer, usage, request.getPrompt(), allPlans);

        log.info("Calling LLM service for customer={}, interactionId={}", customer.getCustomerId(), interactionId);

        String aiRecommendation = ollamaRestClient.generate(payload);

        // --- Intent detection ---
        String intent = detectIntent(request.getPrompt());

        // --- Audit log ---
        copilotInteractionService.logInteraction(interactionId, customer, intent, aiRecommendation);

        return AdvisorResponse.builder()
                .interactionId(interactionId)
                .customerId(customer.getCustomerId())
                .customerName(customer.getName())
                .currentPlanName(customer.getCurrentPlan() != null
                        ? customer.getCurrentPlan().getPlanName() : "No Plan")
                .identifiedIntent(intent)
                .recommendation(aiRecommendation)
                .proRatedCost(proRated)
                .currency("USD")
                .eligiblePromotions(eligiblePromos)
                .build();
    }

    // -------------------------------------------------------------------------
    // Payload Builder — matches FastAPI RecommendationRequest schema
    // -------------------------------------------------------------------------

    private String buildRecommendationPayload(Customer customer,
                                               CustomerUsage usage,
                                               String customerQuery,
                                               List<PlanCatalog> allPlans) {
        try {
            PlanCatalog current = customer.getCurrentPlan();

            // customer_context
            ObjectNode context = objectMapper.createObjectNode();
            context.put("customer_id", customer.getCustomerId());
            context.put("name", customer.getName() != null ? customer.getName() : "");
            context.put("tenure_months", customer.getTenureMonths() != null ? customer.getTenureMonths() : 0);

            // current_plan inside context
            if (current != null) {
                context.set("current_plan", planNode(current));
            }

            context.put("data_used_gb",
                    usage != null && usage.getDataUsedGb() != null ? usage.getDataUsedGb().doubleValue() : 0.0);
            context.put("roaming_used_mb",
                    usage != null && usage.getRoamingUsedMb() != null ? usage.getRoamingUsedMb().doubleValue() : 0.0);
            context.put("billing_cycle_date",
                    customer.getBillingCycleDate() != null ? customer.getBillingCycleDate().toString() : "");

            // available_plans array
            ArrayNode plansArray = objectMapper.createArrayNode();
            for (PlanCatalog p : allPlans) {
                plansArray.add(planNode(p));
            }

            // root payload
            ObjectNode payload = objectMapper.createObjectNode();
            payload.set("customer_context", context);
            payload.put("customer_query", customerQuery);
            // destination_country extracted from query keywords
            payload.putNull("destination_country");
            payload.set("available_plans", plansArray);

            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new RuntimeException("Failed to build LLM recommendation payload", e);
        }
    }

    private ObjectNode planNode(PlanCatalog p) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("plan_id",       p.getPlanId() != null ? p.getPlanId() : 0);
        node.put("plan_name",     p.getPlanName() != null ? p.getPlanName() : "");
        node.put("plan_type",     p.getPlanType() != null ? p.getPlanType() : "");
        node.put("monthly_cost",  p.getMonthlyCost() != null ? p.getMonthlyCost().doubleValue() : 0.0);
        node.put("data_limit_gb", p.getDataLimitGb() != null ? p.getDataLimitGb() : 0);
        node.put("description",   p.getDescription() != null ? p.getDescription() : "");
        return node;
    }

    // -------------------------------------------------------------------------
    // Intent detection
    // -------------------------------------------------------------------------

    private String detectIntent(String prompt) {
        String lower = prompt.toLowerCase();
        if (lower.contains("travel") || lower.contains("roam") || lower.contains("international")) {
            return "TRAVEL_INQUIRY";
        } else if (lower.contains("upgrade") || lower.contains("more data") || lower.contains("better plan")) {
            return "PLAN_UPGRADE";
        } else if (lower.contains("downgrade") || lower.contains("cheaper") || lower.contains("save money")) {
            return "PLAN_DOWNGRADE";
        } else if (lower.contains("booster") || lower.contains("top up") || lower.contains("add data")) {
            return "DATA_BOOSTER";
        } else if (lower.contains("promo") || lower.contains("discount") || lower.contains("offer")) {
            return "PROMO_INQUIRY";
        } else if (lower.contains("bill") || lower.contains("charge") || lower.contains("cost")) {
            return "BILLING_INQUIRY";
        }
        return "GENERAL_INQUIRY";
    }
}
