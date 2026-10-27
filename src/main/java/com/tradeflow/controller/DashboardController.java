package com.tradeflow.controller;

import com.tradeflow.common.ApiResponse;
import com.tradeflow.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    private final DashboardService dashboardService;

    @GetMapping
    ApiResponse<Map<String, Object>> overview() {
        return ApiResponse.ok(dashboardService.overview());
    }
}

