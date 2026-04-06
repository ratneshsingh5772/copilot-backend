package com.telecom.copilot_backend.service;

import com.telecom.copilot_backend.dto.AuthResponse;
import com.telecom.copilot_backend.dto.LoginRequest;
import com.telecom.copilot_backend.dto.RegisterRequest;

/**
 * Service interface for customer authentication operations.
 * Defines the contract for login, registration, and JWT token generation.
 */
public interface IAuthService {

    /**
     * Authenticate a customer with email and password.
     * Validates credentials and generates a JWT token upon successful authentication.
     *
     * @param request login credentials (email and password)
     * @return authentication response containing JWT token and customer info
     * @throws org.springframework.security.core.AuthenticationException if credentials are invalid
     */
    AuthResponse login(LoginRequest request);

    /**
     * Register a new customer account.
     * Creates a new customer profile and generates a JWT token for immediate login.
     * Validates that customer ID and email are unique.
     *
     * @param request registration details (customerId, name, email, password, phoneNumber)
     * @return authentication response containing JWT token and customer info
     * @throws IllegalArgumentException if customer ID or email already exists
     */
    AuthResponse register(RegisterRequest request);
}

