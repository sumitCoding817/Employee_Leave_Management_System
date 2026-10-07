package com.example.EmployeeLeaveManagementSystem.Service;

import com.example.EmployeeLeaveManagementSystem.DTO.EmployeeRequest;
import com.example.EmployeeLeaveManagementSystem.DTO.EmployeeResponse;
import com.example.EmployeeLeaveManagementSystem.DTO.PageResponse;
import com.example.EmployeeLeaveManagementSystem.DTO.RegisterRequest;
import org.springframework.data.domain.Pageable;

public interface EmployeeService {

    EmployeeResponse register(RegisterRequest request);

    PageResponse<EmployeeResponse> getAllEmployees(String keyword, Pageable pageable);

    EmployeeResponse getEmployeeById(Long id);

    EmployeeResponse getCurrentEmployee();

    EmployeeResponse updateEmployee(Long id, EmployeeRequest request);

    void deleteEmployee(Long id);

    EmployeeResponse activateEmployee(Long id);

    EmployeeResponse deactivateEmployee(Long id);
}
