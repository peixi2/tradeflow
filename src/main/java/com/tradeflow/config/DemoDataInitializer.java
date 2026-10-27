package com.tradeflow.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradeflow.model.CustomerOrder;
import com.tradeflow.model.OutboxEvent;
import com.tradeflow.model.PaymentEvent;
import com.tradeflow.model.Product;
import com.tradeflow.model.UserAccount;
import com.tradeflow.repository.CustomerOrderRepository;
import com.tradeflow.repository.OutboxEventRepository;
import com.tradeflow.repository.PaymentEventRepository;
import com.tradeflow.repository.ProductRepository;
import com.tradeflow.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class DemoDataInitializer implements CommandLineRunner {
    private final UserAccountRepository userRepository;
    private final ProductRepository productRepository;
    private final CustomerOrderRepository orderRepository;
    private final PaymentEventRepository paymentRepository;
    private final OutboxEventRepository outboxRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    @Value("${app.demo.seed-enabled:true}")
    private boolean enabled;
    @Value("${app.demo.username:admin}")
    private String username;
    @Value("${app.demo.password:admin123}")
    private String password;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (!enabled) return;
        userRepository.findByUsername(username).orElseGet(() -> userRepository.save(UserAccount.builder()
                .username(username).passwordHash(passwordEncoder.encode(password)).displayName("林然").role("ADMIN").build()));

        List<ProductSeed> products = List.of(
                new ProductSeed("TF-PHONE-01", "Nova X 智能手机", "3299.00", 180, 46),
                new ProductSeed("TF-TABLET-02", "Slate Pro 平板", "2599.00", 120, 31),
                new ProductSeed("TF-WATCH-03", "Pulse 智能手表", "899.00", 260, 58),
                new ProductSeed("TF-BUDS-04", "AirBuds 降噪耳机", "599.00", 340, 72),
                new ProductSeed("TF-CAM-05", "HomeEye 云台相机", "399.00", 220, 39),
                new ProductSeed("TF-HUB-06", "LinkHub 智能网关", "269.00", 300, 44)
        );
        for (ProductSeed seed : products) {
            productRepository.findBySku(seed.sku()).orElseGet(() -> productRepository.save(Product.builder()
                    .sku(seed.sku()).name(seed.name()).price(new BigDecimal(seed.price()))
                    .stock(seed.stock()).reserved(seed.reserved()).build()));
        }

        String[] states = {"CREATED", "PAID", "RESERVED", "SHIPPED", "COMPLETED", "CANCELLED"};
        String[] customers = {"星河科技", "远航零售", "云杉商贸", "北辰数码", "澄海供应链", "晨光生活馆"};
        String[] owners = {"陈序", "周岚", "林然", "顾思远"};
        for (int index = 1; index <= 28; index++) {
            String orderNo = "TF202610" + String.format("%04d", index);
            int current = index;
            orderRepository.findByOrderNo(orderNo).orElseGet(() -> {
                ProductSeed product = products.get((current - 1) % products.size());
                int quantity = current % 4 + 1;
                String state = states[(current - 1) % states.length];
                return orderRepository.save(CustomerOrder.builder()
                        .orderNo(orderNo).customerName(customers[(current - 1) % customers.length])
                        .productSku(product.sku()).quantity(quantity)
                        .totalAmount(new BigDecimal(product.price()).multiply(BigDecimal.valueOf(quantity)))
                        .status(state).paymentStatus("CREATED".equals(state) ? "UNPAID" : "PAID")
                        .owner(owners[(current - 1) % owners.length]).build());
            });
        }

        if (paymentRepository.count() == 0) {
            for (int index = 2; index <= 20; index++) {
                CustomerOrder order = orderRepository.findByOrderNo("TF202610" + String.format("%04d", index)).orElseThrow();
                if (!"UNPAID".equals(order.getPaymentStatus())) {
                    paymentRepository.save(PaymentEvent.builder().eventId("pay_demo_" + index)
                            .orderNo(order.getOrderNo()).amount(order.getTotalAmount())
                            .processedAt(LocalDateTime.now().minusHours(30L - index)).build());
                }
            }
        }

        if (outboxRepository.count() == 0) {
            for (int index = 1; index <= 14; index++) {
                String orderNo = "TF202610" + String.format("%04d", index + 3);
                outboxRepository.save(OutboxEvent.builder().aggregateType("ORDER").aggregateId(orderNo)
                        .eventType(index % 2 == 0 ? "PaymentConfirmed" : "OrderStatusChanged")
                        .payload(objectMapper.writeValueAsString(Map.of("orderNo", orderNo, "seed", true)))
                        .status(index < 12 ? "PUBLISHED" : "PENDING").build());
            }
        }
    }

    private record ProductSeed(String sku, String name, String price, int stock, int reserved) {}
}

