package com.example.EmployeeLeaveManagementSystem.Service;

import com.example.EmployeeLeaveManagementSystem.DTO.AdminDashboardResponse;
import com.example.EmployeeLeaveManagementSystem.DTO.EmployeeDashboardResponse;

public interface DashboardService {

    EmployeeDashboardResponse getEmployeeDashboard();

    AdminDashboardResponse getAdminDashboard();
}
