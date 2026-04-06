package com.telecom.copilot_backend.service;

import com.telecom.copilot_backend.dto.CustomerDto;
import com.telecom.copilot_backend.dto.CustomerSearchCriteria;
import com.telecom.copilot_backend.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service interface for customer management operations.
 * Defines the contract for customer-related business logic.
 */
public interface ICustomerService {

    /**
     * Retrieve all customers.
     * @return list of all customers
     */
    List<CustomerDto> getAllCustomers();

    /**
     * Retrieve customers with pagination and filtering.
     * @param criteria search and filter criteria
     * @param pageable pagination information (page, size, sort)
     * @return paginated list of customers matching criteria
     */
    Page<CustomerDto> searchCustomers(CustomerSearchCriteria criteria, Pageable pageable);

    /**
     * Retrieve a customer by their ID.
     * @param customerId the customer identifier
     * @return customer DTO
     * @throws com.telecom.copilot_backend.exception.ResourceNotFoundException if customer not found
     */
    CustomerDto getCustomerById(String customerId);

    /**
     * Create a new customer.
     * @param dto the customer data
     * @return created customer DTO
     * @throws IllegalArgumentException if customer ID or email already exists
     */
    CustomerDto createCustomer(CustomerDto dto);

    /**
     * Update an existing customer.
     * @param customerId the customer identifier
     * @param dto the updated customer data
     * @return updated customer DTO
     * @throws com.telecom.copilot_backend.exception.ResourceNotFoundException if customer not found
     */
    CustomerDto updateCustomer(String customerId, CustomerDto dto);

    /**
     * Delete a customer.
     * @param customerId the customer identifier
     * @throws com.telecom.copilot_backend.exception.ResourceNotFoundException if customer not found
     */
    void deleteCustomer(String customerId);

    /**
     * Find customer entity by ID (for internal use).
     * @param customerId the customer identifier
     * @return customer entity
     * @throws com.telecom.copilot_backend.exception.ResourceNotFoundException if customer not found
     */
    Customer findEntityByCustomerId(String customerId);

    /**
     * Convert Customer entity to CustomerDto.
     * @param customer the customer entity
     * @return customer DTO
     */
    CustomerDto toDto(Customer customer);
}

