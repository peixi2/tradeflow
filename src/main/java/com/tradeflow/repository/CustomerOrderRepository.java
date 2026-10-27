package com.tradeflow.repository;

import com.tradeflow.model.CustomerOrder;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {
    Optional<CustomerOrder> findByOrderNo(String orderNo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from CustomerOrder o where o.orderNo = :orderNo")
    Optional<CustomerOrder> findByOrderNoForUpdate(@Param("orderNo") String orderNo);

    List<CustomerOrder> findTop50ByOrderByCreatedAtDesc();

    long countByStatus(String status);

    default List<CustomerOrder> recent() {
        return findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream().limit(50).toList();
    }
}

