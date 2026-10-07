package com.example.EmployeeLeaveManagementSystem.Controller;

import com.example.EmployeeLeaveManagementSystem.DTO.ApiResponse;
import com.example.EmployeeLeaveManagementSystem.DTO.LeaveTypeRequest;
import com.example.EmployeeLeaveManagementSystem.DTO.LeaveTypeResponse;
import com.example.EmployeeLeaveManagementSystem.Service.LeaveTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/leave-types")
@RequiredArgsConstructor
@Tag(name = "Leave Types")
public class LeaveTypeController {

    private final LeaveTypeService leaveTypeService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    @Operation(summary = "Create leave type - ADMIN/HR")
    public ApiResponse<LeaveTypeResponse> create(@Valid @RequestBody LeaveTypeRequest request) {
        return ApiResponse.ok("Leave type created successfully", leaveTypeService.createLeaveType(request));
    }

    @GetMapping
    @Operation(summary = "List leave types (any authenticated user)")
    public ApiResponse<List<LeaveTypeResponse>> getAll() {
        return ApiResponse.ok("Leave types fetched successfully", leaveTypeService.getAllLeaveTypes());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get leave type by id")
    public ApiResponse<LeaveTypeResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok("Leave type fetched successfully", leaveTypeService.getLeaveTypeById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    @Operation(summary = "Update leave type - ADMIN/HR")
    public ApiResponse<LeaveTypeResponse> update(@PathVariable Long id,
                                                 @Valid @RequestBody LeaveTypeRequest request) {
        return ApiResponse.ok("Leave type updated successfully", leaveTypeService.updateLeaveType(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    @Operation(summary = "Delete (deactivate) leave type - ADMIN/HR")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        leaveTypeService.deleteLeaveType(id);
        return ApiResponse.ok("Leave type deleted successfully", null);
    }
}
