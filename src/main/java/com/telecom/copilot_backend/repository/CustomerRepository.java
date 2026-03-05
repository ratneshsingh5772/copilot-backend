package com.telecom.copilot_backend.repository;

import com.telecom.copilot_backend.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, String> {

    boolean existsByCustomerId(String customerId);
}
