package com.telecom.copilot_backend.service;

import com.telecom.copilot_backend.dto.CustomerUsageDto;
import com.telecom.copilot_backend.entity.Customer;
import com.telecom.copilot_backend.entity.CustomerUsage;
import com.telecom.copilot_backend.exception.ResourceNotFoundException;
import com.telecom.copilot_backend.repository.CustomerUsageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerUsageService {

    private final CustomerUsageRepository customerUsageRepository;
    private final CustomerService customerService;

    /**
     * Returns the most recent usage record for a customer (current billing period).
     */
    public CustomerUsageDto getCurrentUsage(String customerId) {
        Customer customer = customerService.findEntityByCustomerId(customerId);
        return customerUsageRepository
                .findTopByCustomerOrderByLastUpdatedDesc(customer)
                .map(this::toDto)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "CustomerUsage", "customerId", customerId));
    }

    public List<CustomerUsageDto> getAllUsageForCustomer(String customerId) {
        // Retrieve all usage records by fetching all and filtering (simple approach)
        Customer customer = customerService.findEntityByCustomerId(customerId);
        return customerUsageRepository.findAll().stream()
                .filter(u -> u.getCustomer().getCustomerId().equals(customerId))
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public CustomerUsageDto upsertUsage(CustomerUsageDto dto) {
        Customer customer = customerService.findEntityByCustomerId(dto.getCustomerId());
        CustomerUsage usage = CustomerUsage.builder()
                .customer(customer)
                .billingPeriodStart(dto.getBillingPeriodStart())
                .dataUsedGb(dto.getDataUsedGb())
                .roamingUsedMb(dto.getRoamingUsedMb())
                .lastUpdated(LocalDateTime.now())
                .build();
        return toDto(customerUsageRepository.save(usage));
    }

    // -------------------------------------------------------------------------
    // Package-internal helper — used by AdvisorService for AI context
    // -------------------------------------------------------------------------

    public CustomerUsage findCurrentUsageEntity(Customer customer) {
        return customerUsageRepository
                .findTopByCustomerOrderByLastUpdatedDesc(customer)
                .orElse(null);
    }

    // -------------------------------------------------------------------------
    // Mapping
    // -------------------------------------------------------------------------

    public CustomerUsageDto toDto(CustomerUsage u) {
        return CustomerUsageDto.builder()
                .usageId(u.getUsageId())
                .customerId(u.getCustomer().getCustomerId())
                .billingPeriodStart(u.getBillingPeriodStart())
                .dataUsedGb(u.getDataUsedGb())
                .roamingUsedMb(u.getRoamingUsedMb())
                .lastUpdated(u.getLastUpdated())
                .build();
    }
}

