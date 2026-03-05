package com.telecom.copilot_backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "copilot_interactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CopilotInteraction {

    @Id
    @Column(name = "interaction_id", length = 100)
    private String interactionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    /**
     * Gemini's identified intent, e.g. TRAVEL_INQUIRY, PLAN_UPGRADE, DATA_BOOSTER.
     */
    @Column(name = "identified_intent", length = 50)
    private String identifiedIntent;

    @Column(name = "llm_summary", columnDefinition = "TEXT")
    private String llmSummary;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}

