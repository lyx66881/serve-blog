package com.blog.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.blog.common.ApiResponse;
import com.blog.dto.admin.AdminStatsResponse;
import com.blog.service.AdminStatsService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "数据看板", description = "管理员：全站数据统计（需 admin 角色）")
@RestController
@RequestMapping("/api/v1/admin/stats")
public class AdminStatsController {

    private final AdminStatsService adminStatsService;

    public AdminStatsController(AdminStatsService adminStatsService) {
        this.adminStatsService = adminStatsService;
    }

    @Operation(summary = "看板统计", description = "管理员接口；总量指标 + 近 14 天发文 / 新增用户趋势")
    @GetMapping
    public ApiResponse<AdminStatsResponse> stats() {
        return ApiResponse.ok(adminStatsService.stats());
    }
}
