package com.telecom.copilot_backend.service;

import com.telecom.copilot_backend.dto.CopilotInteractionDto;
import com.telecom.copilot_backend.entity.CopilotInteraction;
import com.telecom.copilot_backend.entity.Customer;
import com.telecom.copilot_backend.repository.CopilotInteractionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CopilotInteractionService {

    private final CopilotInteractionRepository copilotInteractionRepository;
    private final CustomerService customerService;

    public List<CopilotInteractionDto> getInteractionsForCustomer(String customerId) {
        return copilotInteractionRepository
                .findByCustomer_CustomerIdOrderByCreatedAtDesc(customerId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Persists a new interaction log entry — called internally by AdvisorService.
     */
    @Transactional
    public CopilotInteraction logInteraction(String interactionId,
                                             Customer customer,
                                             String identifiedIntent,
                                             String llmSummary) {
        CopilotInteraction interaction = CopilotInteraction.builder()
                .interactionId(interactionId)
                .customer(customer)
                .identifiedIntent(identifiedIntent)
                .llmSummary(llmSummary)
                .createdAt(LocalDateTime.now())
                .build();
        return copilotInteractionRepository.save(interaction);
    }

    // -------------------------------------------------------------------------
    // Mapping
    // -------------------------------------------------------------------------

    public CopilotInteractionDto toDto(CopilotInteraction i) {
        return CopilotInteractionDto.builder()
                .interactionId(i.getInteractionId())
                .customerId(i.getCustomer().getCustomerId())
                .identifiedIntent(i.getIdentifiedIntent())
                .llmSummary(i.getLlmSummary())
                .createdAt(i.getCreatedAt())
                .build();
    }
}

