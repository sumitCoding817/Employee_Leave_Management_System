package com.example.EmployeeLeaveManagementSystem.Controller;

import com.example.EmployeeLeaveManagementSystem.DTO.AdminDashboardResponse;
import com.example.EmployeeLeaveManagementSystem.DTO.ApiResponse;
import com.example.EmployeeLeaveManagementSystem.DTO.EmployeeDashboardResponse;
import com.example.EmployeeLeaveManagementSystem.Service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/employee")
    @Operation(summary = "My dashboard")
    public ApiResponse<EmployeeDashboardResponse> employeeDashboard() {
        return ApiResponse.ok("Dashboard fetched successfully", dashboardService.getEmployeeDashboard());
    }

    @GetMapping("/admin")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    @Operation(summary = "Admin / HR dashboard")
    public ApiResponse<AdminDashboardResponse> adminDashboard() {
        return ApiResponse.ok("Dashboard fetched successfully", dashboardService.getAdminDashboard());
    }
}
