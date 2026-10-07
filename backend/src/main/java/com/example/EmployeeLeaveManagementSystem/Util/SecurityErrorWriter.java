package com.example.EmployeeLeaveManagementSystem.Util;

import com.example.EmployeeLeaveManagementSystem.DTO.ApiErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Writes the standard JSON error body from servlet-level handlers (entry point / access denied). */
public final class SecurityErrorWriter {

    private SecurityErrorWriter() {
    }

    public static void write(ObjectMapper objectMapper, HttpServletResponse response,
                             HttpStatus status, String message, String path) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), ApiErrorResponse.of(status, message, path, null));
    }
}
