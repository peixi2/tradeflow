package com.tradeflow.controller;

import com.tradeflow.common.ApiResponse;
import com.tradeflow.model.CustomerOrder;
import com.tradeflow.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @GetMapping
    ApiResponse<List<CustomerOrder>> list() {
        return ApiResponse.ok(orderService.list());
    }

    @GetMapping("/{orderNo}")
    ApiResponse<CustomerOrder> get(@PathVariable String orderNo) {
        return ApiResponse.ok(orderService.get(orderNo));
    }

    @PostMapping("/payment-callback")
    ApiResponse<Map<String, Object>> payment(@Valid @RequestBody PaymentRequest request) {
        return ApiResponse.ok(orderService.paymentCallback(request.eventId(), request.orderNo(), request.amount()));
    }

    @PostMapping("/{orderNo}/reserve")
    ApiResponse<CustomerOrder> reserve(@PathVariable String orderNo) {
        return ApiResponse.ok("库存预占成功", orderService.reserve(orderNo));
    }

    @PostMapping("/{orderNo}/transition")
    ApiResponse<CustomerOrder> transition(@PathVariable String orderNo, @Valid @RequestBody TransitionRequest request) {
        return ApiResponse.ok("状态流转成功", orderService.transition(orderNo, request.target()));
    }

    public record PaymentRequest(@NotBlank String eventId, @NotBlank String orderNo,
                                 @NotNull @DecimalMin("0.01") BigDecimal amount) {}
    public record TransitionRequest(@NotBlank String target) {}
}

