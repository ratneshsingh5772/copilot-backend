package com.telecom.copilot_backend.repository;

import com.telecom.copilot_backend.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, String>, JpaSpecificationExecutor<Customer> {

    boolean existsByCustomerId(String customerId);

    Optional<Customer> findByEmail(String email);

    boolean existsByEmail(String email);
}


