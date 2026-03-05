package com.telecom.copilot_backend.repository;

import com.telecom.copilot_backend.entity.PlanTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlanTransactionRepository extends JpaRepository<PlanTransaction, String> {

    List<PlanTransaction> findByCustomer_CustomerIdOrderByExecutedAtDesc(String customerId);
}

