package com.example.EmployeeLeaveManagementSystem.Service;

import com.example.EmployeeLeaveManagementSystem.DTO.LoginRequest;
import com.example.EmployeeLeaveManagementSystem.DTO.LoginResponse;
import com.example.EmployeeLeaveManagementSystem.Entity.Employee;
import com.example.EmployeeLeaveManagementSystem.Enums.Role;
import com.example.EmployeeLeaveManagementSystem.Repository.EmployeeRepository;
import com.example.EmployeeLeaveManagementSystem.Security.JwtService;
import com.example.EmployeeLeaveManagementSystem.Security.TokenBlacklistService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private AuthenticationManager authenticationManager;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private JwtService jwtService;
    @Mock private TokenBlacklistService tokenBlacklistService;
    @Mock private EmployeeService employeeService;

    @InjectMocks private AuthService authService;

    @Test
    void login_withValidCredentials_returnsTokenAndUserDetails() {
        Employee employee = Employee.builder().id(1L).firstName("John").lastName("Doe")
                .email("john@gmail.com").role(Role.EMPLOYEE).build();
        when(authenticationManager.authenticate(any())).thenReturn(mock(Authentication.class));
        when(employeeRepository.findByEmail("john@gmail.com")).thenReturn(Optional.of(employee));
        when(jwtService.generateToken(employee)).thenReturn("jwt-token");

        LoginResponse response = authService.login(new LoginRequest("John@Gmail.com", "Password@123"));

        assertEquals("jwt-token", response.token());
        assertEquals("Bearer", response.type());
        assertEquals(1L, response.userId());
        assertEquals("John Doe", response.name());
        assertEquals(Role.EMPLOYEE, response.role());
    }

    @Test
    void login_withInvalidCredentials_throwsAndIssuesNoToken() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class,
                () -> authService.login(new LoginRequest("john@gmail.com", "wrong")));
        verifyNoInteractions(jwtService);
    }
}
