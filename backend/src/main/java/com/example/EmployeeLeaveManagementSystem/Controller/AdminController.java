package com.example.EmployeeLeaveManagementSystem.Controller;

import com.example.EmployeeLeaveManagementSystem.DTO.ApiResponse;
import com.example.EmployeeLeaveManagementSystem.DTO.EmployeeRequest;
import com.example.EmployeeLeaveManagementSystem.DTO.EmployeeResponse;
import com.example.EmployeeLeaveManagementSystem.DTO.LeaveResponse;
import com.example.EmployeeLeaveManagementSystem.DTO.PageResponse;
import com.example.EmployeeLeaveManagementSystem.Enums.LeaveStatus;
import com.example.EmployeeLeaveManagementSystem.Service.EmployeeService;
import com.example.EmployeeLeaveManagementSystem.Service.LeaveService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAnyRole('ADMIN','HR')")
@RequiredArgsConstructor
@Tag(name = "Admin / HR")
public class AdminController {

    private final EmployeeService employeeService;
    private final LeaveService leaveService;

    @GetMapping("/employees")
    @Operation(summary = "List / search employees (?keyword=john&page=0&size=10&sort=firstName,asc)")
    public ApiResponse<PageResponse<EmployeeResponse>> getEmployees(
            @RequestParam(required = false) String keyword,
            @ParameterObject @PageableDefault(size = 10) Pageable pageable) {
        return ApiResponse.ok("Employees fetched successfully", employeeService.getAllEmployees(keyword, pageable));
    }

    @GetMapping("/employees/{id}")
    @Operation(summary = "Get an employee by id")
    public ApiResponse<EmployeeResponse> getEmployee(@PathVariable Long id) {
        return ApiResponse.ok("Employee fetched successfully", employeeService.getEmployeeById(id));
    }

    @PutMapping("/employees/{id}")
    @Operation(summary = "Update an employee (only ADMIN may change employee code, email or role)")
    public ApiResponse<EmployeeResponse> updateEmployee(@PathVariable Long id,
                                                        @Valid @RequestBody EmployeeRequest request) {
        return ApiResponse.ok("Employee updated successfully", employeeService.updateEmployee(id, request));
    }

    @PutMapping("/employees/{id}/activate")
    @Operation(summary = "Activate an employee")
    public ApiResponse<EmployeeResponse> activateEmployee(@PathVariable Long id) {
        return ApiResponse.ok("Employee activated successfully", employeeService.activateEmployee(id));
    }

    @PutMapping("/employees/{id}/deactivate")
    @Operation(summary = "Deactivate an employee")
    public ApiResponse<EmployeeResponse> deactivateEmployee(@PathVariable Long id) {
        return ApiResponse.ok("Employee deactivated successfully", employeeService.deactivateEmployee(id));
    }

    @GetMapping("/leaves")
    @Operation(summary = "Filter leaves by status, employeeId, department and/or date window")
    public ApiResponse<PageResponse<LeaveResponse>> getLeaves(
            @RequestParam(required = false) LeaveStatus status,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @ParameterObject @PageableDefault(size = 10) Pageable pageable) {
        return ApiResponse.ok("Leave requests fetched successfully",
                leaveService.getAllLeaves(status, employeeId, department, startDate, endDate, pageable));
    }

    @GetMapping("/leaves/status/{status}")
    @Operation(summary = "Leaves by status")
    public ApiResponse<PageResponse<LeaveResponse>> getLeavesByStatus(
            @PathVariable LeaveStatus status,
            @ParameterObject @PageableDefault(size = 10) Pageable pageable) {
        return ApiResponse.ok("Leave requests fetched successfully",
                leaveService.getAllLeaves(status, null, null, null, null, pageable));
    }

    @GetMapping("/leaves/employee/{employeeId}")
    @Operation(summary = "Leaves of one employee")
    public ApiResponse<PageResponse<LeaveResponse>> getLeavesByEmployee(
            @PathVariable Long employeeId,
            @ParameterObject @PageableDefault(size = 10) Pageable pageable) {
        return ApiResponse.ok("Leave requests fetched successfully",
                leaveService.getAllLeaves(null, employeeId, null, null, null, pageable));
    }
}
