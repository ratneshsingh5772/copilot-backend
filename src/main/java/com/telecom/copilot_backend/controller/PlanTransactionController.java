package com.telecom.copilot_backend.controller;

import com.telecom.copilot_backend.dto.ApiResponse;
import com.telecom.copilot_backend.dto.PlanChangeRequest;
import com.telecom.copilot_backend.dto.PlanTransactionDto;
import com.telecom.copilot_backend.service.IPlanTransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Tag(name = "Plan Transactions", description = "Plan change execution and financial ledger")
public class PlanTransactionController {

    private final IPlanTransactionService planTransactionService;

    @PostMapping("/execute")
    @Operation(
            summary = "Execute a plan change",
            description = "Calculates the pro-rated cost, optionally applies a promo discount, "
                    + "updates the customer's plan, and records the transaction in the ledger.")
    public ResponseEntity<ApiResponse<PlanTransactionDto>> executePlanChange(
            @Valid @RequestBody PlanChangeRequest request) {
        PlanTransactionDto result = planTransactionService.executePlanChange(request);
        return ResponseEntity.ok(ApiResponse.ok("Plan change executed successfully", result));
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Get all plan transactions for a customer")
    public ResponseEntity<ApiResponse<List<PlanTransactionDto>>> getTransactions(
            @PathVariable String customerId) {
        return ResponseEntity.ok(ApiResponse.ok(
                planTransactionService.getTransactionsForCustomer(customerId)));
    }
}

