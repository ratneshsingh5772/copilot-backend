package com.telecom.copilot_backend.repository;

import com.telecom.copilot_backend.entity.CopilotInteraction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CopilotInteractionRepository extends JpaRepository<CopilotInteraction, String> {

    List<CopilotInteraction> findByCustomer_CustomerIdOrderByCreatedAtDesc(String customerId);
}

