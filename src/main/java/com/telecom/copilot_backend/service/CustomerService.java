package com.telecom.copilot_backend.service;

import com.telecom.copilot_backend.dto.CustomerDto;
import com.telecom.copilot_backend.entity.Customer;
import com.telecom.copilot_backend.entity.PlanCatalog;
import com.telecom.copilot_backend.exception.ResourceNotFoundException;
import com.telecom.copilot_backend.repository.CustomerRepository;
import com.telecom.copilot_backend.repository.PlanCatalogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final PlanCatalogRepository planCatalogRepository;

    public List<CustomerDto> getAllCustomers() {
        return customerRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public CustomerDto getCustomerById(String customerId) {
        return toDto(findEntityByCustomerId(customerId));
    }

    @Transactional
    public CustomerDto createCustomer(CustomerDto dto) {
        if (customerRepository.existsByCustomerId(dto.getCustomerId())) {
            throw new IllegalArgumentException("Customer with ID '" + dto.getCustomerId() + "' already exists");
        }
        Customer saved = customerRepository.save(toEntity(dto));
        return toDto(saved);
    }

    @Transactional
    public CustomerDto updateCustomer(String customerId, CustomerDto dto) {
        Customer existing = findEntityByCustomerId(customerId);
        existing.setName(dto.getName());
        existing.setPhoneNumber(dto.getPhoneNumber());
        existing.setTenureMonths(dto.getTenureMonths());
        existing.setBillingCycleDate(dto.getBillingCycleDate());
        if (dto.getCurrentPlanId() != null) {
            PlanCatalog plan = planCatalogRepository.findById(dto.getCurrentPlanId())
                    .orElseThrow(() -> new ResourceNotFoundException("Plan", "planId",
                            String.valueOf(dto.getCurrentPlanId())));
            existing.setCurrentPlan(plan);
        }
        return toDto(customerRepository.save(existing));
    }

    @Transactional
    public void deleteCustomer(String customerId) {
        customerRepository.delete(findEntityByCustomerId(customerId));
    }

    // -------------------------------------------------------------------------
    // Package-internal helpers used by other services
    // -------------------------------------------------------------------------

    public Customer findEntityByCustomerId(String customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "customerId", customerId));
    }

    // -------------------------------------------------------------------------
    // Mapping
    // -------------------------------------------------------------------------

    public CustomerDto toDto(Customer c) {
        return CustomerDto.builder()
                .customerId(c.getCustomerId())
                .name(c.getName())
                .phoneNumber(c.getPhoneNumber())
                .currentPlanId(c.getCurrentPlan() != null ? c.getCurrentPlan().getPlanId() : null)
                .currentPlanName(c.getCurrentPlan() != null ? c.getCurrentPlan().getPlanName() : null)
                .tenureMonths(c.getTenureMonths())
                .billingCycleDate(c.getBillingCycleDate())
                .build();
    }

    private Customer toEntity(CustomerDto dto) {
        PlanCatalog plan = null;
        if (dto.getCurrentPlanId() != null) {
            plan = planCatalogRepository.findById(dto.getCurrentPlanId())
                    .orElseThrow(() -> new ResourceNotFoundException("Plan", "planId",
                            String.valueOf(dto.getCurrentPlanId())));
        }
        return Customer.builder()
                .customerId(dto.getCustomerId())
                .name(dto.getName())
                .phoneNumber(dto.getPhoneNumber())
                .currentPlan(plan)
                .tenureMonths(dto.getTenureMonths())
                .billingCycleDate(dto.getBillingCycleDate())
                .build();
    }
}
