package com.telecom.copilot_backend.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.telecom.copilot_backend.dto.AdvisorRequest;
import com.telecom.copilot_backend.dto.AdvisorResponse;
import com.telecom.copilot_backend.dto.PromotionDto;
import com.telecom.copilot_backend.entity.CopilotInteraction;
import com.telecom.copilot_backend.entity.Customer;
import com.telecom.copilot_backend.entity.CustomerUsage;
import com.telecom.copilot_backend.entity.PlanCatalog;
import com.telecom.copilot_backend.repository.PlanCatalogRepository;
import com.telecom.copilot_backend.service.CopilotInteractionService;
import com.telecom.copilot_backend.service.CustomerUsageService;
import com.telecom.copilot_backend.service.IAdvisorService;
import com.telecom.copilot_backend.service.ICustomerService;
import com.telecom.copilot_backend.service.ILlmClient;
import com.telecom.copilot_backend.service.IPlanTransactionService;
import com.telecom.copilot_backend.service.PromotionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation of IAdvisorService.
 * Provides AI-powered plan recommendations using Ollama LLM integration.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdvisorServiceImpl implements IAdvisorService {

    private final ICustomerService customerService;
    private final ILlmClient llmClient;
    private final PromotionService promotionService;
    private final CustomerUsageService customerUsageService;
    private final CopilotInteractionService copilotInteractionService;
    private final IPlanTransactionService planTransactionService;
    private final PlanCatalogRepository planCatalogRepository;
    private final ObjectMapper objectMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public AdvisorResponse advise(AdvisorRequest request) {
        log.info("Generating plan advice for customer: {}", request.getCustomerId());

        // 1. Load customer entity
        Customer customer = customerService.findEntityByCustomerId(request.getCustomerId());

        // 2. Get current usage (may be null for new customers)
        CustomerUsage usage = customerUsageService.findCurrentUsageEntity(customer);

        // 3. Get eligible promotions based on tenure
        List<PromotionDto> eligiblePromotions = promotionService.getEligiblePromotions(
                customer.getTenureMonths() != null ? customer.getTenureMonths() : 0
        );

        // 4. Load all active plans from catalogue
        List<PlanCatalog> allPlans = planCatalogRepository.findAll();

        // 5. Build the structured JSON payload the FastAPI LLM service expects
        String llmPayload = buildRecommendationPayload(customer, usage, request.getPrompt(), allPlans);
        log.debug("LLM payload: {}", llmPayload);

        // 6. Call LLM service
        String llmRecommendation = llmClient.generate(llmPayload);
        log.debug("LLM recommendation: {}", llmRecommendation);

        // 7. Determine intent from the customer's original query
        String identifiedIntent = extractIntent(request.getPrompt());

        // 8. Resolve the recommended plan from DB (fall back to current plan)
        PlanCatalog recommendedPlan = planCatalogRepository.findAll().stream()
                .findFirst()
                .orElse(customer.getCurrentPlan());

        // 9. Calculate pro-rated cost
        BigDecimal proRatedCost = planTransactionService.calculateProRatedAmount(
                customer,
                recommendedPlan
        );

        // 10. Log interaction for audit trail
        CopilotInteraction interaction = copilotInteractionService.logInteraction(
                customer,
                identifiedIntent,
                llmRecommendation
        );

        // 11. Build and return response
        return AdvisorResponse.builder()
                .interactionId(interaction.getInteractionId())
                .customerId(customer.getCustomerId())
                .customerName(customer.getName())
                .currentPlanName(customer.getCurrentPlan() != null ? customer.getCurrentPlan().getPlanName() : "None")
                .identifiedIntent(identifiedIntent)
                .recommendation(llmRecommendation)
                .proRatedCost(proRatedCost)
                .currency("USD")
                .eligiblePromotions(eligiblePromotions)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // JSON payload builder
    // ─────────────────────────────────────────────────────────────────────────────

    // Field-name constants to avoid duplicated literals
    private static final String F_PLAN_ID       = "plan_id";
    private static final String F_PLAN_NAME     = "plan_name";
    private static final String F_PLAN_TYPE     = "plan_type";
    private static final String F_MONTHLY_COST  = "monthly_cost";
    private static final String F_DATA_LIMIT_GB = "data_limit_gb";
    private static final String F_DESCRIPTION   = "description";
    private static final String DEFAULT_PLAN_TYPE = "BASE_PLAN";

    /**
     * Builds the JSON body expected by the FastAPI LLM service:
     *
     * <pre>
     * {
     *   "customer_context": { customer_id, name, tenure_months, current_plan,
     *                          data_used_gb, roaming_used_mb, billing_cycle_date },
     *   "customer_query": "...",
     *   "available_plans": [ { plan_id, plan_name, plan_type, monthly_cost,
     *                           data_limit_gb, description }, ... ],
     *   "destination_country": "US"   // optional
     * }
     * </pre>
     */
    private String buildRecommendationPayload(Customer customer,
                                              CustomerUsage usage,
                                              String customerQuery,
                                              List<PlanCatalog> allPlans) {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.set("customer_context", buildCustomerContext(customer, usage));
            root.put("customer_query", customerQuery);

            ArrayNode plansArray = objectMapper.createArrayNode();
            allPlans.forEach(plan -> plansArray.add(planToJsonNode(plan)));
            root.set("available_plans", plansArray);

            String destination = extractDestinationCountry(customerQuery);
            if (destination != null) {
                root.put("destination_country", destination);
            }

            return objectMapper.writeValueAsString(root);

        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("Failed to build LLM recommendation payload: " + e.getMessage(), e);
        }
    }

    /** Builds the {@code customer_context} node. */
    private ObjectNode buildCustomerContext(Customer customer, CustomerUsage usage) {
        ObjectNode ctx = objectMapper.createObjectNode();
        ctx.put("customer_id",        customer.getCustomerId());
        ctx.put("name",               customer.getName());
        ctx.put("tenure_months",      customer.getTenureMonths() != null ? customer.getTenureMonths() : 0);
        ctx.put("billing_cycle_date",
                customer.getBillingCycleDate() != null
                        ? customer.getBillingCycleDate().toString()
                        : java.time.LocalDate.now().toString());
        ctx.put("data_used_gb",       usage != null && usage.getDataUsedGb() != null
                ? usage.getDataUsedGb().doubleValue() : 0.0);
        ctx.put("roaming_used_mb",    usage != null && usage.getRoamingUsedMb() != null
                ? usage.getRoamingUsedMb().doubleValue() : 0.0);
        ctx.set("current_plan",       buildCurrentPlanNode(customer.getCurrentPlan()));
        return ctx;
    }

    /** Builds the {@code current_plan} node (handles null plan gracefully). */
    private ObjectNode buildCurrentPlanNode(PlanCatalog cp) {
        ObjectNode node = objectMapper.createObjectNode();
        if (cp != null) {
            return planToJsonNode(cp);
        }
        node.put(F_PLAN_ID,       0);
        node.put(F_PLAN_NAME,     "None");
        node.put(F_PLAN_TYPE,     DEFAULT_PLAN_TYPE);
        node.put(F_MONTHLY_COST,  0.0);
        node.put(F_DATA_LIMIT_GB, 0);
        node.put(F_DESCRIPTION,   "No active plan");
        return node;
    }

    /** Converts a {@link PlanCatalog} entity to the {@code PlanInfo} JSON node. */
    private ObjectNode planToJsonNode(PlanCatalog plan) {
        ObjectNode p = objectMapper.createObjectNode();
        p.put(F_PLAN_ID,       plan.getPlanId());
        p.put(F_PLAN_NAME,     plan.getPlanName());
        p.put(F_PLAN_TYPE,     plan.getPlanType() != null ? plan.getPlanType() : DEFAULT_PLAN_TYPE);
        p.put(F_MONTHLY_COST,  plan.getMonthlyCost() != null ? plan.getMonthlyCost().doubleValue() : 0.0);
        p.put(F_DATA_LIMIT_GB, plan.getDataLimitGb() != null ? plan.getDataLimitGb() : 0);
        p.put(F_DESCRIPTION,   plan.getDescription() != null ? plan.getDescription() : "");
        return p;
    }

    /**
     * Naively extracts a destination country from the customer's free-text query.
     * Returns {@code null} if none is detected.
     */
    private String extractDestinationCountry(String query) {
        if (query == null) return null;
        Pattern pattern = Pattern.compile(
                "\\b(?:travelling? to the|travelling? to|visiting|in)\\s+([A-Za-z][A-Za-z ]{1,29})(?=\\s+(?:next|for|this|in)|[.,!?]|$)",
                Pattern.CASE_INSENSITIVE
        );
        Matcher matcher = pattern.matcher(query);
        if (matcher.find()) {
            String country = matcher.group(1).trim();
            if (!country.equalsIgnoreCase("the") && country.length() > 1) {
                return country;
            }
        }
        return null;
    }

    /**
     * Extract identified intent from the customer's raw query text.
     */
    private String extractIntent(String query) {
        if (query == null || query.isBlank()) {
            return "GENERAL_INQUIRY";
        }
        String lower = query.toLowerCase();
        if (lower.contains("travel") || lower.contains("roaming") || lower.contains("international")) {
            return "TRAVEL_INQUIRY";
        } else if (lower.contains("upgrade")) {
            return "PLAN_UPGRADE";
        } else if (lower.contains("downgrade")) {
            return "PLAN_DOWNGRADE";
        } else if (lower.contains("data") || lower.contains("booster")) {
            return "DATA_BOOSTER_REQUEST";
        } else if (lower.contains("bill") || lower.contains("charge") || lower.contains("cost")) {
            return "BILLING_INQUIRY";
        } else if (lower.contains("discount") || lower.contains("promo")) {
            return "PROMOTION_INQUIRY";
        }

        return "GENERAL_INQUIRY";
    }
}

