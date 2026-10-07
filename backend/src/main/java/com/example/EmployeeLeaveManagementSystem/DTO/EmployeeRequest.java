package com.example.EmployeeLeaveManagementSystem.DTO;

import com.example.EmployeeLeaveManagementSystem.Enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Partial-update request: every field is optional, and only non-null fields are applied.
 * Which fields a caller may change depends on their role (see EmployeeServiceImpl).
 */
public record EmployeeRequest(
        @Size(max = 20, message = "Employee code must not exceed 20 characters")
        @Pattern(regexp = "^[A-Za-z0-9-]*$", message = "Employee code may contain only letters, digits and hyphens")
        String employeeCode,

        @Size(max = 50, message = "First name must not exceed 50 characters")
        String firstName,

        @Size(max = 50, message = "Last name must not exceed 50 characters")
        String lastName,

        @Email(message = "Please enter a valid email")
        @Size(max = 100, message = "Email must not exceed 100 characters")
        String email,

        @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Phone number must contain 10 to 15 digits")
        String phone,

        Long departmentId,

        @Size(max = 100, message = "Designation must not exceed 100 characters")
        String designation,

        @PastOrPresent(message = "Joining date cannot be in the future")
        LocalDate joiningDate,

        Role role) {
}
