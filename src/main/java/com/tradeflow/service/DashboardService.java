package com.tradeflow.service;

import com.tradeflow.model.CustomerOrder;
import com.tradeflow.model.OutboxEvent;
import com.tradeflow.model.Product;
import com.tradeflow.repository.CustomerOrderRepository;
import com.tradeflow.repository.OutboxEventRepository;
import com.tradeflow.repository.PaymentEventRepository;
import com.tradeflow.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {
    private final CustomerOrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final PaymentEventRepository paymentEventRepository;
    private final OutboxEventRepository outboxRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> overview() {
        List<CustomerOrder> orders = orderRepository.recent();
        BigDecimal revenue = orders.stream()
                .filter(order -> "PAID".equals(order.getPaymentStatus()))
                .map(CustomerOrder::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, Long> status = new LinkedHashMap<>();
        for (String value : List.of("CREATED", "PAID", "RESERVED", "SHIPPED", "COMPLETED", "CANCELLED")) {
            status.put(value, orderRepository.countByStatus(value));
        }
        return Map.of(
                "orders", orders.size(),
                "paidRevenue", revenue,
                "paymentEvents", paymentEventRepository.count(),
                "pendingOutbox", outboxRepository.countByStatus("PENDING"),
                "status", status,
                "products", productRepository.findAll().stream().map(this::productView).toList(),
                "events", outboxRepository.findTop12ByOrderByCreatedAtDesc().stream().map(this::eventView).toList()
        );
    }

    private Map<String, Object> productView(Product product) {
        return Map.of("sku", product.getSku(), "name", product.getName(), "stock", product.getStock(),
                "reserved", product.getReserved(), "available", product.available(), "version", product.getVersion());
    }

    private Map<String, Object> eventView(OutboxEvent event) {
        return Map.of("id", event.getId(), "aggregateId", event.getAggregateId(), "eventType", event.getEventType(),
                "status", event.getStatus(), "createdAt", event.getCreatedAt());
    }
}

