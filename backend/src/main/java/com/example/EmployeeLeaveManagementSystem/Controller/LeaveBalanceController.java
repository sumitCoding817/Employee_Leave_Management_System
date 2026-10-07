package com.example.EmployeeLeaveManagementSystem.Controller;

import com.example.EmployeeLeaveManagementSystem.DTO.ApiResponse;
import com.example.EmployeeLeaveManagementSystem.DTO.LeaveBalanceRequest;
import com.example.EmployeeLeaveManagementSystem.DTO.LeaveBalanceResponse;
import com.example.EmployeeLeaveManagementSystem.Service.LeaveBalanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/leave-balances")
@RequiredArgsConstructor
@Tag(name = "Leave Balances")
public class LeaveBalanceController {

    private final LeaveBalanceService leaveBalanceService;

    @GetMapping("/my")
    @Operation(summary = "My leave balances (optional ?year=, defaults to current year)")
    public ApiResponse<List<LeaveBalanceResponse>> getMyBalances(@RequestParam(required = false) Integer year) {
        return ApiResponse.ok("Leave balances fetched successfully", leaveBalanceService.getMyBalances(year));
    }

    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    @Operation(summary = "Leave balances of an employee - ADMIN/HR")
    public ApiResponse<List<LeaveBalanceResponse>> getByEmployee(@PathVariable Long employeeId,
                                                                 @RequestParam(required = false) Integer year) {
        return ApiResponse.ok("Leave balances fetched successfully",
                leaveBalanceService.getBalancesByEmployee(employeeId, year));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    @Operation(summary = "Create a leave balance - ADMIN/HR")
    public ApiResponse<LeaveBalanceResponse> create(@Valid @RequestBody LeaveBalanceRequest request) {
        return ApiResponse.ok("Leave balance created successfully", leaveBalanceService.createBalance(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    @Operation(summary = "Update a leave balance - ADMIN/HR")
    public ApiResponse<LeaveBalanceResponse> update(@PathVariable Long id,
                                                    @Valid @RequestBody LeaveBalanceRequest request) {
        return ApiResponse.ok("Leave balance updated successfully", leaveBalanceService.updateBalance(id, request));
    }
}
