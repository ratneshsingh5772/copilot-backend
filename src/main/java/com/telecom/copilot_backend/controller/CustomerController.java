package com.telecom.copilot_backend.controller;

import com.telecom.copilot_backend.dto.ApiResponse;
import com.telecom.copilot_backend.dto.CustomerDto;
import com.telecom.copilot_backend.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
@Tag(name = "Customers", description = "Customer profile management")
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping
    @Operation(summary = "List all customers")
    public ResponseEntity<ApiResponse<List<CustomerDto>>> getAllCustomers() {
        return ResponseEntity.ok(ApiResponse.ok(customerService.getAllCustomers()));
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
    @Operation(summary = "Update an existing customer")
    public ResponseEntity<ApiResponse<CustomerDto>> updateCustomer(
            @PathVariable String customerId,
            @Valid @RequestBody CustomerDto dto) {
        return ResponseEntity.ok(ApiResponse.ok("Customer updated successfully",
                customerService.updateCustomer(customerId, dto)));
    }

    @DeleteMapping("/{customerId}")
    @Operation(summary = "Delete a customer")
    public ResponseEntity<ApiResponse<Void>> deleteCustomer(@PathVariable String customerId) {
        customerService.deleteCustomer(customerId);
        return ResponseEntity.ok(ApiResponse.ok("Customer deleted successfully", null));
    }
}
