package com.tradeflow.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradeflow.model.CustomerOrder;
import com.tradeflow.model.OutboxEvent;
import com.tradeflow.model.PaymentEvent;
import com.tradeflow.model.Product;
import com.tradeflow.repository.CustomerOrderRepository;
import com.tradeflow.repository.OutboxEventRepository;
import com.tradeflow.repository.PaymentEventRepository;
import com.tradeflow.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class OrderService {
    private static final Map<String, Set<String>> TRANSITIONS = Map.of(
            "CREATED", Set.of("PAID", "CANCELLED"),
            "PAID", Set.of("RESERVED", "CANCELLED"),
            "RESERVED", Set.of("SHIPPED", "CANCELLED"),
            "SHIPPED", Set.of("COMPLETED"),
            "COMPLETED", Set.of(),
            "CANCELLED", Set.of()
    );

    private final CustomerOrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final PaymentEventRepository paymentEventRepository;
    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<CustomerOrder> list() {
        return orderRepository.recent();
    }

    @Transactional(readOnly = true)
    public CustomerOrder get(String orderNo) {
        return orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new IllegalArgumentException("订单不存在"));
    }

    @Transactional
    public Map<String, Object> paymentCallback(String eventId, String orderNo, BigDecimal amount) {
        if (paymentEventRepository.existsByEventId(eventId)) {
            return Map.of("duplicate", true, "order", get(orderNo));
        }
        CustomerOrder order = orderRepository.findByOrderNoForUpdate(orderNo)
                .orElseThrow(() -> new IllegalArgumentException("订单不存在"));
        if (amount.compareTo(order.getTotalAmount()) != 0) {
            throw new IllegalArgumentException("支付金额与订单金额不一致");
        }
        paymentEventRepository.saveAndFlush(PaymentEvent.builder()
                .eventId(eventId).orderNo(orderNo).amount(amount).processedAt(LocalDateTime.now()).build());
        if (!"PAID".equals(order.getPaymentStatus())) {
            order.setPaymentStatus("PAID");
            order.setStatus("PAID");
            writeOutbox(orderNo, "PaymentConfirmed", Map.of("eventId", eventId, "amount", amount));
        }
        return Map.of("duplicate", false, "order", order);
    }

    @Transactional
    public CustomerOrder reserve(String orderNo) {
        CustomerOrder order = orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new IllegalArgumentException("订单不存在"));
        if (!"PAID".equals(order.getStatus())) {
            throw new IllegalArgumentException("仅已支付订单可以预占库存");
        }
        Product product = productRepository.findBySku(order.getProductSku())
                .orElseThrow(() -> new IllegalArgumentException("商品不存在"));
        if (product.available() < order.getQuantity()) {
            throw new IllegalArgumentException("可用库存不足");
        }
        product.setReserved(product.getReserved() + order.getQuantity());
        order.setStatus("RESERVED");
        writeOutbox(orderNo, "InventoryReserved", Map.of("sku", product.getSku(), "quantity", order.getQuantity()));
        return order;
    }

    @Transactional
    public CustomerOrder transition(String orderNo, String target) {
        CustomerOrder order = orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new IllegalArgumentException("订单不存在"));
        String normalized = target.toUpperCase();
        if (!TRANSITIONS.getOrDefault(order.getStatus(), Set.of()).contains(normalized)) {
            throw new IllegalArgumentException("不允许从 " + order.getStatus() + " 流转到 " + normalized);
        }
        order.setStatus(normalized);
        writeOutbox(orderNo, "OrderStatusChanged", Map.of("status", normalized));
        return order;
    }

    private void writeOutbox(String orderNo, String type, Map<String, ?> payload) {
        try {
            outboxRepository.save(OutboxEvent.builder()
                    .aggregateType("ORDER").aggregateId(orderNo).eventType(type)
                    .payload(objectMapper.writeValueAsString(payload)).status("PENDING").build());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("事件序列化失败", exception);
        }
    }
}

