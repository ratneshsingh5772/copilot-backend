package com.telecom.copilot_backend.repository;

import com.telecom.copilot_backend.entity.Customer;
import com.telecom.copilot_backend.entity.CustomerUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerUsageRepository extends JpaRepository<CustomerUsage, Integer> {

    /**
     * Returns the most recent usage record for a customer (current billing period).
     */
    Optional<CustomerUsage> findTopByCustomerOrderByLastUpdatedDesc(Customer customer);
}

