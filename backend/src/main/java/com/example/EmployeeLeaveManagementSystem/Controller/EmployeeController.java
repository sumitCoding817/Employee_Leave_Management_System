package com.example.EmployeeLeaveManagementSystem.Controller;

import com.example.EmployeeLeaveManagementSystem.DTO.ApiResponse;
import com.example.EmployeeLeaveManagementSystem.DTO.EmployeeRequest;
import com.example.EmployeeLeaveManagementSystem.DTO.EmployeeResponse;
import com.example.EmployeeLeaveManagementSystem.DTO.PageResponse;
import com.example.EmployeeLeaveManagementSystem.Service.EmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
@Tag(name = "Employees")
public class EmployeeController {

    private final EmployeeService employeeService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    @Operation(summary = "List employees (paged, optional keyword search) - ADMIN/HR")
    public ApiResponse<PageResponse<EmployeeResponse>> getAll(
            @RequestParam(required = false) String keyword,
            @ParameterObject @PageableDefault(size = 10) Pageable pageable) {
        return ApiResponse.ok("Employees fetched successfully", employeeService.getAllEmployees(keyword, pageable));
    }

    @GetMapping("/me")
    @Operation(summary = "Get my profile")
    public ApiResponse<EmployeeResponse> getMe() {
        return ApiResponse.ok("Profile fetched successfully", employeeService.getCurrentEmployee());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get employee by id (ADMIN/HR any, EMPLOYEE only self)")
    public ApiResponse<EmployeeResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok("Employee fetched successfully", employeeService.getEmployeeById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update employee (field access depends on role)")
    public ApiResponse<EmployeeResponse> update(@PathVariable Long id, @Valid @RequestBody EmployeeRequest request) {
        return ApiResponse.ok("Employee updated successfully", employeeService.updateEmployee(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete (deactivate) employee - ADMIN only")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        employeeService.deleteEmployee(id);
        return ApiResponse.ok("Employee deleted successfully", null);
    }
}
