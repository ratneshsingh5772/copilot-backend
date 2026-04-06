package com.telecom.copilot_backend.controller;

import com.telecom.copilot_backend.dto.ApiResponse;
import com.telecom.copilot_backend.dto.AuthResponse;
import com.telecom.copilot_backend.dto.CustomerDto;
import com.telecom.copilot_backend.dto.LoginRequest;
import com.telecom.copilot_backend.dto.RegisterRequest;
import com.telecom.copilot_backend.security.CustomerUserDetails;
import com.telecom.copilot_backend.service.IAuthService;
import com.telecom.copilot_backend.service.ICustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Customer login, registration and profile")
public class AuthController {

    private final IAuthService authService;
    private final ICustomerService customerService;

    @PostMapping("/login")
    @Operation(summary = "Customer login — returns JWT token")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok("Login successful", response));
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new customer — returns JWT token")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Registration successful", response));
    }

    @GetMapping("/me")
    @Operation(
        summary = "Get current authenticated customer profile",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<CustomerDto>> me(
            @AuthenticationPrincipal CustomerUserDetails principal) {
        CustomerDto dto = customerService.getCustomerById(principal.getCustomerId());
        return ResponseEntity.ok(ApiResponse.ok(dto));
    }
}


