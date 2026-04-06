package com.telecom.copilot_backend.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telecom.copilot_backend.exception.LlmServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Helper mapper responsible for parsing raw JSON responses returned by the
 * FastAPI LLM microservice and converting them into usable Java values.
 *
 * <p><b>Single Responsibility:</b> this class has one job — translate LLM
 * service JSON payloads into application-level objects/text. All HTTP transport
 * concerns remain in {@link com.telecom.copilot_backend.service.OllamaRestClient}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LlmResponseMapper {

    private final ObjectMapper objectMapper;

    // ─── Field-name constants ────────────────────────────────────────────────────

    private static final String FIELD_REC_PLAN_NAME    = "recommended_plan_name";
    private static final String FIELD_REC_PLAN_ID      = "recommended_plan_id";
    private static final String FIELD_REC_SUMMARY      = "recommended_plan_summary";
    private static final String FIELD_CUR_SUMMARY      = "current_plan_summary";
    private static final String FIELD_MONTHLY_COST     = "monthly_cost";
    private static final String FIELD_DATA_LIMIT_GB    = "data_limit_gb";
    private static final String FIELD_SUITABLE_FOR     = "suitable_for";
    private static final String FIELD_REASONING        = "reasoning";
    private static final String FIELD_MESSAGE          = "personalized_message";
    private static final String FIELD_STATUS           = "status";

    // ─── Public API ──────────────────────────────────────────────────────────────

    /**
     * Parses the FastAPI {@code RecommendationResponse} JSON and formats it into
     * a rich, human-readable advisor text block including the current-plan vs
     * recommended-plan comparison.
     *
     * @param responseBody raw JSON string from the LLM microservice
     * @return formatted recommendation text ready for the API response
     * @throws LlmServiceException if the JSON cannot be parsed
     */
    public String toAdvisorText(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);

            // ── Recommended plan ──────────────────────────────────────────────
            String recName  = root.path(FIELD_REC_PLAN_NAME).asText("Unknown");
            int    recId    = root.path(FIELD_REC_PLAN_ID).asInt(0);
            JsonNode recSum = root.path(FIELD_REC_SUMMARY);
            double recCost  = recSum.path(FIELD_MONTHLY_COST).asDouble(0);
            int    recData  = recSum.path(FIELD_DATA_LIMIT_GB).asInt(0);
            String recFor   = recSum.path(FIELD_SUITABLE_FOR).asText("");

            // ── Current plan ──────────────────────────────────────────────────
            JsonNode curSum  = root.path(FIELD_CUR_SUMMARY);
            String curName   = curSum.path("plan_name").asText("Unknown");
            double curCost   = curSum.path(FIELD_MONTHLY_COST).asDouble(0);
            int    curData   = curSum.path(FIELD_DATA_LIMIT_GB).asInt(0);
            String curFor    = curSum.path(FIELD_SUITABLE_FOR).asText("");

            String reasoning = root.path(FIELD_REASONING).asText("");
            String message   = root.path(FIELD_MESSAGE).asText("");

            log.debug("LLM response parsed: recPlanId={}, recPlanName={}", recId, recName);

            // ── Format ────────────────────────────────────────────────────────
            StringBuilder sb = new StringBuilder();
            sb.append(message).append("\n\n");

            sb.append("── Current Plan ─────────────────────────────────────\n");
            sb.append(String.format("  %-20s $%.2f/month | %s GB data%n", curName, curCost,
                    curData == 9999 ? "Unlimited" : String.valueOf(curData)));
            if (!curFor.isBlank()) {
                sb.append("  Best for: ").append(curFor).append("\n");
            }

            sb.append("\n── Recommended Plan ─────────────────────────────────\n");
            sb.append(String.format("  %-20s $%.2f/month | %s GB data%n", recName, recCost,
                    recData == 9999 ? "Unlimited" : String.valueOf(recData)));
            if (!recFor.isBlank()) {
                sb.append("  Best for: ").append(recFor).append("\n");
            }

            if (!reasoning.isBlank()) {
                sb.append("\n── Why this plan? ───────────────────────────────────\n");
                sb.append("  ").append(reasoning);
            }

            return sb.toString().trim();

        } catch (Exception e) {
            throw new LlmServiceException(
                    "Failed to parse LLM service response: " + responseBody, e);
        }
    }

    /**
     * Checks whether a FastAPI {@code /health} response JSON reports status
     * {@code "ok"}.
     *
     * @param healthResponseBody raw JSON from {@code GET /health}
     * @return {@code true} if status is {@code "ok"}, {@code false} otherwise
     */
    public boolean isHealthy(String healthResponseBody) {
        try {
            JsonNode root = objectMapper.readTree(healthResponseBody);
            return "ok".equals(root.path(FIELD_STATUS).asText());
        } catch (Exception e) {
            log.warn("Failed to parse health response: {}", e.getMessage());
            return false;
        }
    }
}

