package com.example.EmployeeLeaveManagementSystem.Security;

import com.example.EmployeeLeaveManagementSystem.Util.SecurityErrorWriter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** Called when an unauthenticated request hits a protected endpoint -> 401. */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        SecurityErrorWriter.write(objectMapper, response, HttpStatus.UNAUTHORIZED,
                "Authentication required. Please provide a valid JWT token", request.getRequestURI());
    }
}
