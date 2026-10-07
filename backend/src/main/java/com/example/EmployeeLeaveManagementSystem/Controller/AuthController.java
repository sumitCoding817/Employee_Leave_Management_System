package com.example.EmployeeLeaveManagementSystem.Controller;

import com.example.EmployeeLeaveManagementSystem.DTO.ApiResponse;
import com.example.EmployeeLeaveManagementSystem.DTO.EmployeeResponse;
import com.example.EmployeeLeaveManagementSystem.DTO.LoginRequest;
import com.example.EmployeeLeaveManagementSystem.DTO.LoginResponse;
import com.example.EmployeeLeaveManagementSystem.DTO.RegisterRequest;
import com.example.EmployeeLeaveManagementSystem.Service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a new employee (role is always EMPLOYEE)")
    public ApiResponse<EmployeeResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok("Employee registered successfully", authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Login and receive a JWT")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout - invalidates the supplied JWT")
    public ApiResponse<Void> logout(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
                                    String authorization) {
        authService.logout(authorization);
        return ApiResponse.ok("Logged out successfully", null);
    }
}
