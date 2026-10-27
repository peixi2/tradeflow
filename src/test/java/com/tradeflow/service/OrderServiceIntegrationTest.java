package com.tradeflow.service;

import com.tradeflow.model.CustomerOrder;
import com.tradeflow.repository.CustomerOrderRepository;
import com.tradeflow.repository.PaymentEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class OrderServiceIntegrationTest {
    @Autowired OrderService orderService;
    @Autowired CustomerOrderRepository orderRepository;
    @Autowired PaymentEventRepository paymentRepository;

    @Test
    @Transactional
    void repeatedPaymentCallbackIsIdempotent() {
        CustomerOrder order = orderRepository.findByOrderNo("TF2026100001").orElseThrow();
        String eventId = "test-" + UUID.randomUUID();
        var first = orderService.paymentCallback(eventId, order.getOrderNo(), order.getTotalAmount());
        var second = orderService.paymentCallback(eventId, order.getOrderNo(), order.getTotalAmount());
        assertThat(first.get("duplicate")).isEqualTo(false);
        assertThat(second.get("duplicate")).isEqualTo(true);
        assertThat(paymentRepository.existsByEventId(eventId)).isTrue();
    }

    @Test
    @Transactional
    void invalidTransitionIsRejected() {
        CustomerOrder order = orderRepository.findByOrderNo("TF2026100005").orElseThrow();
        assertThat(order.getStatus()).isEqualTo("COMPLETED");
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> orderService.transition(order.getOrderNo(), "PAID"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

