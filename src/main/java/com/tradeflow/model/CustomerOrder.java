package com.tradeflow.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "tf_order", indexes = {
        @Index(name = "idx_order_status_created", columnList = "status,created_at"),
        @Index(name = "idx_order_customer", columnList = "customer_name")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerOrder extends BaseEntity {
    @Column(nullable = false, unique = true, length = 36)
    private String orderNo;

    @Column(nullable = false, length = 80)
    private String customerName;

    @Column(nullable = false, length = 40)
    private String productSku;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false, length = 24)
    private String status;

    @Column(nullable = false, length = 24)
    private String paymentStatus;

    @Column(length = 80)
    private String owner;

    @Version
    private Long version;
}

