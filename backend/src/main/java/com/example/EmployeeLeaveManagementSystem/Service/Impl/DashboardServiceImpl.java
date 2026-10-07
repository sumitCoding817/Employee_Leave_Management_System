package com.example.EmployeeLeaveManagementSystem.Service.Impl;

import com.example.EmployeeLeaveManagementSystem.DTO.AdminDashboardResponse;
import com.example.EmployeeLeaveManagementSystem.DTO.EmployeeDashboardResponse;
import com.example.EmployeeLeaveManagementSystem.DTO.LeaveBalanceResponse;
import com.example.EmployeeLeaveManagementSystem.DTO.LeaveTypeStatistic;
import com.example.EmployeeLeaveManagementSystem.Entity.Employee;
import com.example.EmployeeLeaveManagementSystem.Enums.LeaveStatus;
import com.example.EmployeeLeaveManagementSystem.Repository.EmployeeRepository;
import com.example.EmployeeLeaveManagementSystem.Repository.LeaveRequestRepository;
import com.example.EmployeeLeaveManagementSystem.Repository.LeaveTypeRepository;
import com.example.EmployeeLeaveManagementSystem.Service.DashboardService;
import com.example.EmployeeLeaveManagementSystem.Service.LeaveBalanceService;
import com.example.EmployeeLeaveManagementSystem.Util.CurrentUserUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final EmployeeRepository employeeRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveBalanceService leaveBalanceService;
    private final CurrentUserUtil currentUserUtil;

    /** Not read-only: viewing balances lazily provisions the current year's balances. */
    @Override
    @Transactional
    public EmployeeDashboardResponse getEmployeeDashboard() {
        Employee employee = currentUserUtil.getCurrentEmployee();
        List<LeaveBalanceResponse> balances = leaveBalanceService.getMyBalances(null);

        int allocated = balances.stream().mapToInt(LeaveBalanceResponse::totalAllocated).sum();
        int used = balances.stream().mapToInt(LeaveBalanceResponse::usedDays).sum();
        int remaining = balances.stream().mapToInt(LeaveBalanceResponse::remainingDays).sum();

        return new EmployeeDashboardResponse(
                leaveTypeRepository.countByActiveTrue(),
                allocated,
                used,
                remaining,
                leaveRequestRepository.countByEmployeeIdAndStatus(employee.getId(), LeaveStatus.PENDING),
                leaveRequestRepository.countByEmployeeIdAndStatus(employee.getId(), LeaveStatus.APPROVED),
                leaveRequestRepository.countByEmployeeIdAndStatus(employee.getId(), LeaveStatus.REJECTED));
    }

    @Override
    @Transactional(readOnly = true)
    public AdminDashboardResponse getAdminDashboard() {
        long active = employeeRepository.countByActive(true);
        long inactive = employeeRepository.countByActive(false);

        Map<String, Long> departmentWise = new LinkedHashMap<>();
        employeeRepository.countEmployeesByDepartment()
                .forEach(row -> departmentWise.put((String) row[0], ((Number) row[1]).longValue()));

        // value layout: [pending, approved, rejected, cancelled, approvedDays]
        Map<String, long[]> statistics = new TreeMap<>();
        for (Object[] row : leaveRequestRepository.leaveStatistics()) {
            String leaveType = (String) row[0];
            LeaveStatus status = (LeaveStatus) row[1];
            long count = ((Number) row[2]).longValue();
            long days = ((Number) row[3]).longValue();
            long[] values = statistics.computeIfAbsent(leaveType, key -> new long[5]);
            switch (status) {
                case PENDING -> values[0] = count;
                case APPROVED -> {
                    values[1] = count;
                    values[4] = days;
                }
                case REJECTED -> values[2] = count;
                case CANCELLED -> values[3] = count;
            }
        }
        List<LeaveTypeStatistic> leaveStatistics = statistics.entrySet().stream()
                .map(entry -> new LeaveTypeStatistic(entry.getKey(), entry.getValue()[0], entry.getValue()[1],
                        entry.getValue()[2], entry.getValue()[3], entry.getValue()[4]))
                .toList();

        return new AdminDashboardResponse(
                active + inactive,
                active,
                inactive,
                leaveRequestRepository.countByStatus(LeaveStatus.PENDING),
                leaveRequestRepository.countByStatus(LeaveStatus.APPROVED),
                leaveRequestRepository.countByStatus(LeaveStatus.REJECTED),
                departmentWise,
                leaveStatistics);
    }
}
