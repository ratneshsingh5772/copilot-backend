package com.telecom.copilot_backend.service;

import com.telecom.copilot_backend.dto.AdvisorRequest;
import com.telecom.copilot_backend.dto.AdvisorResponse;
import com.telecom.copilot_backend.dto.PromotionDto;
import com.telecom.copilot_backend.entity.Customer;
import com.telecom.copilot_backend.entity.CustomerUsage;
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

    private final GeminiRestClient geminiRestClient;
    private final CustomerService customerService;
    private final PlanService planService;
    private final PromotionService promotionService;
    private final CustomerUsageService customerUsageService;
    private final CopilotInteractionService copilotInteractionService;
    private final PlanTransactionService planTransactionService;

    /**
     * Core advisor flow:
     * 1. Load customer + live usage data.
     * 2. Fetch eligible promotions.
     * 3. Calculate pro-rated cost for any potential upgrade.
     * 4. Build enriched system prompt and call Gemini.
     * 5. Detect intent from response.
     * 6. Log the interaction to copilot_interactions for auditing.
     */
    public AdvisorResponse advise(AdvisorRequest request) {
        String interactionId = UUID.randomUUID().toString();
        Customer customer = customerService.findEntityByCustomerId(request.getCustomerId());

        // --- Usage data ---
        CustomerUsage usage = customerUsageService.findCurrentUsageEntity(customer);

        // --- Eligible promotions ---
        List<PromotionDto> eligiblePromos = promotionService.getEligiblePromotions(customer.getTenureMonths());

        // --- Pro-rated cost (based on current plan price, if available) ---
        BigDecimal proRated = BigDecimal.ZERO;
        if (customer.getCurrentPlan() != null) {
            proRated = planTransactionService.calculateProRatedAmount(customer, customer.getCurrentPlan());
        }

        // --- Plan catalogue ---
        String planCatalogue = planService.buildPlanCatalogueText();

        // --- Build system prompt ---
        String systemPrompt = buildSystemPrompt(customer, usage, eligiblePromos, proRated, planCatalogue);

        log.info("Calling Gemini for customer={}, interactionId={}", customer.getCustomerId(), interactionId);

        String aiRecommendation = geminiRestClient.generate(systemPrompt, request.getPrompt());

        // --- Identify intent (keyword-based extraction from AI response) ---
        String intent = detectIntent(request.getPrompt());

        // --- Audit log → copilot_interactions ---
        copilotInteractionService.logInteraction(
                interactionId, customer, intent, aiRecommendation);

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
    // Helpers
    // -------------------------------------------------------------------------

    private String buildSystemPrompt(Customer customer,
                                     CustomerUsage usage,
                                     List<PromotionDto> promos,
                                     BigDecimal proRated,
                                     String planCatalogue) {
        String planName = customer.getCurrentPlan() != null
                ? customer.getCurrentPlan().getPlanName() : "None";
        String planCost = customer.getCurrentPlan() != null && customer.getCurrentPlan().getMonthlyCost() != null
                ? "$" + customer.getCurrentPlan().getMonthlyCost() + "/month" : "N/A";
        String dataLimit = customer.getCurrentPlan() != null && customer.getCurrentPlan().getDataLimitGb() != null
                ? (customer.getCurrentPlan().getDataLimitGb() == 9999
                    ? "Unlimited" : customer.getCurrentPlan().getDataLimitGb() + " GB")
                : "N/A";

        String usageInfo = "No current usage data available.";
        if (usage != null) {
            usageInfo = String.format("Data Used: %s GB | Roaming Used: %s MB (updated: %s)",
                    usage.getDataUsedGb(),
                    usage.getRoamingUsedMb(),
                    usage.getLastUpdated());
        }

        StringBuilder promosText = new StringBuilder();
        if (promos.isEmpty()) {
            promosText.append("No active promotions available for this customer.");
        } else {
            promos.forEach(p -> promosText.append(String.format(
                    "  - %s: %d%% off (requires %d months tenure)%n",
                    p.getPromoName(), p.getDiscountPercentage(), p.getMinTenureMonths())));
        }

        return String.format("""
                You are a helpful and empathetic telecom plan advisor for a self-service copilot.
                Your goal is to help the customer find the best plan, answer billing questions, \
                and assist with travel add-ons — all without needing to transfer to a human agent.

                === Customer Profile ===
                Customer ID    : %s
                Name           : %s
                Phone          : %s
                Tenure         : %d months
                Billing Date   : %s
                Current Plan   : %s (%s | Data: %s)

                === Current Usage (This Billing Period) ===
                %s

                === Pro-Rated Cost Estimate ===
                If the customer changes plan today, the pro-rated charge for the \
                remaining billing period is approximately $%.2f USD.

                === Eligible Promotions ===
                %s

                === %s

                Instructions:
                - Answer the customer's question clearly and concisely.
                - Reference specific plan names, IDs, and prices from the catalogue.
                - Always show the pro-rated cost when a plan change is relevant.
                - Suggest applicable promotions to maximise savings.
                - If the customer wants to confirm a change, tell them to use the \
                  POST /api/v1/transactions/execute endpoint with the plan ID.
                """,
                customer.getCustomerId(),
                customer.getName(),
                customer.getPhoneNumber() != null ? customer.getPhoneNumber() : "N/A",
                customer.getTenureMonths() != null ? customer.getTenureMonths() : 0,
                customer.getBillingCycleDate() != null ? customer.getBillingCycleDate().toString() : "N/A",
                planName, planCost, dataLimit,
                usageInfo,
                proRated.doubleValue(),
                promosText,
                planCatalogue
        );
    }

    /**
     * Simple keyword-based intent detection from the user's prompt.
     * In production this could be a secondary Gemini call with a structured output schema.
     */
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
