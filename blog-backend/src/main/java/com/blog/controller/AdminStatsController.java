package com.blog.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.blog.common.ApiResponse;
import com.blog.dto.admin.AdminStatsResponse;
import com.blog.service.AdminStatsService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 管理端数据看板接口（SecurityConfig 层已限定 ROLE_ADMIN）。
 */
@Tag(name = "管理端-数据看板", description = "平台统计（仅管理员）")
@RestController
@RequestMapping("/api/v1/admin/stats")
public class AdminStatsController {

    private final AdminStatsService adminStatsService;

    public AdminStatsController(AdminStatsService adminStatsService) {
        this.adminStatsService = adminStatsService;
    }

    @Operation(summary = "看板统计", description = "总量统计 + 近 14 天发布/注册趋势")
    @GetMapping
    public ApiResponse<AdminStatsResponse> stats() {
        return ApiResponse.ok(adminStatsService.stats());
    }
}
