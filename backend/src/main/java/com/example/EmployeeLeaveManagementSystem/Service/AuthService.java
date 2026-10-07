package com.example.EmployeeLeaveManagementSystem.Service;

import com.example.EmployeeLeaveManagementSystem.DTO.EmployeeResponse;
import com.example.EmployeeLeaveManagementSystem.DTO.LoginRequest;
import com.example.EmployeeLeaveManagementSystem.DTO.LoginResponse;
import com.example.EmployeeLeaveManagementSystem.DTO.RegisterRequest;
import com.example.EmployeeLeaveManagementSystem.Entity.Employee;
import com.example.EmployeeLeaveManagementSystem.Exception.BusinessException;
import com.example.EmployeeLeaveManagementSystem.Exception.EmployeeNotFoundException;
import com.example.EmployeeLeaveManagementSystem.Repository.EmployeeRepository;
import com.example.EmployeeLeaveManagementSystem.Security.JwtService;
import com.example.EmployeeLeaveManagementSystem.Security.TokenBlacklistService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthenticationManager authenticationManager;
    private final EmployeeRepository employeeRepository;
    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;
    private final EmployeeService employeeService;

    public EmployeeResponse register(RegisterRequest request) {
        return employeeService.register(request);
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, request.password()));
        } catch (AuthenticationException ex) {
            log.warn("Failed login attempt for email={}", email);
            throw ex;
        }

        Employee employee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new EmployeeNotFoundException(email));
        String token = jwtService.generateToken(employee);
        log.info("Login successful: userId={}, email={}", employee.getId(), employee.getEmail());

        return new LoginResponse(token, "Bearer", employee.getId(),
                employee.getFirstName() + " " + employee.getLastName(),
                employee.getEmail(), employee.getRole());
    }

    public void logout(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            throw new BusinessException("Authorization header with a Bearer token is required");
        }
        String token = authorizationHeader.substring(BEARER_PREFIX.length());
        try {
            Claims claims = jwtService.parseClaims(token);
            tokenBlacklistService.blacklist(token, claims.getExpiration());
            log.info("Logout: email={}", claims.getSubject());
        } catch (JwtException | IllegalArgumentException ex) {
            throw new BusinessException("Invalid or expired token");
        }
    }
}
