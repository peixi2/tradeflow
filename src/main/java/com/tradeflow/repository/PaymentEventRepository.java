package com.tradeflow.repository;

import com.tradeflow.model.PaymentEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentEventRepository extends JpaRepository<PaymentEvent, Long> {
    boolean existsByEventId(String eventId);
}

