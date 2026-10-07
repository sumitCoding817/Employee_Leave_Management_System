package com.example.EmployeeLeaveManagementSystem.Integration;

import com.example.EmployeeLeaveManagementSystem.Repository.DepartmentRepository;
import com.example.EmployeeLeaveManagementSystem.Repository.LeaveTypeRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Boots the full application on H2; every test is rolled back, the seeded admin remains available. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
abstract class AbstractIntegrationTest {

    protected static final String PASSWORD = "Password@123";
    protected static final String ADMIN_EMAIL = "admin@gmail.com";
    protected static final String ADMIN_PASSWORD = "Admin@123";

    @Autowired protected MockMvc mockMvc;
    @Autowired protected ObjectMapper objectMapper;
    @Autowired protected DepartmentRepository departmentRepository;
    @Autowired protected LeaveTypeRepository leaveTypeRepository;

    protected Map<String, Object> registerPayload(String code, String email) {
        Long departmentId = departmentRepository.findByNameIgnoreCase("IT").orElseThrow().getId();
        return Map.of(
                "employeeCode", code,
                "firstName", "John",
                "lastName", "Doe",
                "email", email,
                "password", PASSWORD,
                "phone", "9876543210",
                "departmentId", departmentId,
                "designation", "Developer",
                "joiningDate", "2024-01-15");
    }

    protected String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    protected JsonNode read(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    protected ResultActions register(String code, String email) throws Exception {
        return mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(registerPayload(code, email))));
    }

    protected String login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        return read(result).get("token").asText();
    }

    protected String registerAndLogin(String code, String email) throws Exception {
        register(code, email).andExpect(status().isCreated());
        return login(email, PASSWORD);
    }

    protected MockHttpServletRequestBuilder bearer(MockHttpServletRequestBuilder builder, String token) {
        return builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }
}
