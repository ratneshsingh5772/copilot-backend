package com.telecom.copilot_backend;

import com.telecom.copilot_backend.dto.AdvisorRequest;
import com.telecom.copilot_backend.dto.AdvisorResponse;
import com.telecom.copilot_backend.dto.PromotionDto;
import com.telecom.copilot_backend.entity.CopilotInteraction;
import com.telecom.copilot_backend.entity.Customer;
import com.telecom.copilot_backend.entity.PlanCatalog;
import com.telecom.copilot_backend.repository.PlanCatalogRepository;
import com.telecom.copilot_backend.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdvisorServiceTest {

    @Mock private OllamaRestClient ollamaRestClient;
    @Mock private ICustomerService customerService;
    @Mock private PlanService planService;
    @Mock private PromotionService promotionService;
    @Mock private CustomerUsageService customerUsageService;
    @Mock private CopilotInteractionService copilotInteractionService;
    @Mock private PlanTransactionService planTransactionService;
    @Mock private PlanCatalogRepository planCatalogRepository;

    @InjectMocks
    private IAdvisorService advisorService;

    @Test
    void advise_returnsRecommendation() {
        // --- Arrange ---
        PlanCatalog plan = PlanCatalog.builder()
                .planId(1)
                .planName("Basic 5GB")
                .planType("BASE_PLAN")
                .monthlyCost(new BigDecimal("29.99"))
                .dataLimitGb(5)
                .build();

        Customer customer = Customer.builder()
                .customerId("CUST001")
                .name("Alice")
                .phoneNumber("+1-555-0100")
                .currentPlan(plan)
                .tenureMonths(24)
                .billingCycleDate(LocalDate.now().plusDays(10))
                .build();

        when(customerService.findEntityByCustomerId("CUST001")).thenReturn(customer);
        when(customerUsageService.findCurrentUsageEntity(customer)).thenReturn(null);
        when(promotionService.getEligiblePromotions(24)).thenReturn(List.of(
                PromotionDto.builder()
                        .promoId(1).promoName("Loyalty 20% Off")
                        .discountPercentage(20).minTenureMonths(12).isActive(true)
                        .build()
        ));
        when(planTransactionService.calculateProRatedAmount(eq(customer), eq(plan)))
                .thenReturn(new BigDecimal("9.67"));
        when(planCatalogRepository.findAll()).thenReturn(List.of(plan));

        // Single-arg generate() — matches new OllamaRestClient signature
        when(ollamaRestClient.generate(anyString()))
                .thenReturn("Recommended Plan: Premium 20GB (ID: 2) — $45.00/month\n\nI recommend upgrading to Premium 20GB.");

        CopilotInteraction stubInteraction = CopilotInteraction.builder()
                .interactionId("test-id")
                .customer(customer)
                .identifiedIntent("PLAN_UPGRADE")
                .llmSummary("Recommended Plan: Premium 20GB")
                .createdAt(LocalDateTime.now())
                .build();
        when(copilotInteractionService.logInteraction(anyString(), eq(customer),
                anyString(), anyString())).thenReturn(stubInteraction);

        AdvisorRequest request = new AdvisorRequest("CUST001", "What plan should I upgrade to?");

        // --- Act ---
        AdvisorResponse response = advisorService.advise(request);

        // --- Assert ---
        assertThat(response.getCustomerId()).isEqualTo("CUST001");
        assertThat(response.getCustomerName()).isEqualTo("Alice");
        assertThat(response.getCurrentPlanName()).isEqualTo("Basic 5GB");
        assertThat(response.getIdentifiedIntent()).isEqualTo("PLAN_UPGRADE");
        assertThat(response.getRecommendation()).contains("Premium 20GB");
        assertThat(response.getProRatedCost()).isEqualByComparingTo("9.67");
        assertThat(response.getCurrency()).isEqualTo("USD");
        assertThat(response.getEligiblePromotions()).hasSize(1);
        assertThat(response.getInteractionId()).isNotBlank();
    }
}
