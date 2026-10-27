package com.tradeflow.repository;

import com.tradeflow.model.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {
    List<OutboxEvent> findTop12ByOrderByCreatedAtDesc();
    long countByStatus(String status);
}

