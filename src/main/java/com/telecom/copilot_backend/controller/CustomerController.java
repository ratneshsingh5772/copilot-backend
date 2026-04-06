package com.telecom.copilot_backend.controller;

import com.telecom.copilot_backend.dto.ApiResponse;
import com.telecom.copilot_backend.dto.CustomerDto;
import com.telecom.copilot_backend.dto.CustomerSearchCriteria;
import com.telecom.copilot_backend.service.ICustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
@Tag(name = "Customers", description = "Customer profile management")
public class CustomerController {

    private final ICustomerService customerService;

    @GetMapping
    @Operation(summary = "List all customers")
    public ResponseEntity<ApiResponse<List<CustomerDto>>> getAllCustomers() {
        return ResponseEntity.ok(ApiResponse.ok(customerService.getAllCustomers()));
    }

    @GetMapping("/search")
    @Operation(
        summary = "Search customers with pagination and filtering",
        security = @SecurityRequirement(name = "bearerAuth"),
        description = "Search for customers with optional filtering and pagination. " +
                "Query parameters: name, email, phoneNumber, currentPlanId, minTenureMonths, maxTenureMonths, " +
                "page (0-indexed), size (default 10), sort (e.g., 'name,asc' or 'tenureMonths,desc')"
    )
    public ResponseEntity<ApiResponse<Page<CustomerDto>>> searchCustomers(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phoneNumber,
            @RequestParam(required = false) Integer currentPlanId,
            @RequestParam(required = false) Integer minTenureMonths,
            @RequestParam(required = false) Integer maxTenureMonths,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name,asc") String[] sort) {

        // Build sort order
        Sort.Order[] orders = new Sort.Order[sort.length];
        for (int i = 0; i < sort.length; i++) {
            String[] parts = sort[i].split(",");
            String field = parts[0].trim();
            String direction = parts.length > 1 ? parts[1].trim() : "asc";
            orders[i] = "desc".equalsIgnoreCase(direction)
                ? Sort.Order.desc(field)
                : Sort.Order.asc(field);
        }

        // Build pagination
        Pageable pageable = PageRequest.of(page, size, Sort.by(orders));

        // Build search criteria
        CustomerSearchCriteria criteria = CustomerSearchCriteria.builder()
                .name(name)
                .email(email)
                .phoneNumber(phoneNumber)
                .currentPlanId(currentPlanId)
                .minTenureMonths(minTenureMonths)
                .maxTenureMonths(maxTenureMonths)
                .build();

        Page<CustomerDto> result = customerService.searchCustomers(criteria, pageable);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/{customerId}")
    @Operation(summary = "Get customer by ID")
    public ResponseEntity<ApiResponse<CustomerDto>> getCustomer(@PathVariable String customerId) {
        return ResponseEntity.ok(ApiResponse.ok(customerService.getCustomerById(customerId)));
    }

    @PostMapping
    @Operation(summary = "Create a new customer")
    public ResponseEntity<ApiResponse<CustomerDto>> createCustomer(@Valid @RequestBody CustomerDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Customer created successfully", customerService.createCustomer(dto)));
    }

    @PutMapping("/{customerId}")
    @Operation(
        summary = "Update an existing customer",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<CustomerDto>> updateCustomer(
            @PathVariable String customerId,
            @Valid @RequestBody CustomerDto dto) {
        return ResponseEntity.ok(ApiResponse.ok("Customer updated successfully",
                customerService.updateCustomer(customerId, dto)));
    }

    @DeleteMapping("/{customerId}")
    @Operation(
        summary = "Delete a customer",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<Void>> deleteCustomer(@PathVariable String customerId) {
        customerService.deleteCustomer(customerId);
        return ResponseEntity.ok(ApiResponse.ok("Customer deleted successfully", null));
    }
}



