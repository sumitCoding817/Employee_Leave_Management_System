package com.example.EmployeeLeaveManagementSystem.Integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RoleAuthorizationIntegrationTest extends AbstractIntegrationTest {

    @Test
    void request_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/employees/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void employee_cannotAccessAdminEndpoints() throws Exception {
        String token = registerAndLogin("EMP300", "emp300@gmail.com");

        mockMvc.perform(bearer(get("/api/admin/employees"), token)).andExpect(status().isForbidden());
        mockMvc.perform(bearer(get("/api/dashboard/admin"), token)).andExpect(status().isForbidden());
        mockMvc.perform(bearer(get("/api/employees"), token)).andExpect(status().isForbidden());
    }

    @Test
    void employee_cannotApproveOrRejectLeaves() throws Exception {
        String token = registerAndLogin("EMP301", "emp301@gmail.com");

        mockMvc.perform(bearer(put("/api/leaves/1/approve"), token)).andExpect(status().isForbidden());
        mockMvc.perform(bearer(put("/api/leaves/1/reject"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rejectionReason\":\"No\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void employee_cannotViewAnotherEmployeesProfile() throws Exception {
        String firstToken = registerAndLogin("EMP302", "emp302@gmail.com");
        MvcResult second = register("EMP303", "emp303@gmail.com").andExpect(status().isCreated()).andReturn();
        long secondId = read(second).at("/data/id").asLong();

        mockMvc.perform(bearer(get("/api/employees/" + secondId), firstToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(bearer(get("/api/employees/me"), firstToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("emp302@gmail.com"));
    }

    @Test
    void admin_canListEmployeesAndSeesDashboard() throws Exception {
        String adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);

        mockMvc.perform(bearer(get("/api/admin/employees").param("keyword", "admin"), adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].password").doesNotExist())
                .andExpect(jsonPath("$.data.page").value(0));
        mockMvc.perform(bearer(get("/api/dashboard/admin"), adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalEmployees").isNumber());
    }

    @Test
    void logout_invalidatesTheToken() throws Exception {
        String token = registerAndLogin("EMP304", "emp304@gmail.com");
        mockMvc.perform(bearer(get("/api/employees/me"), token)).andExpect(status().isOk());

        mockMvc.perform(bearer(post("/api/auth/logout"), token)).andExpect(status().isOk());

        mockMvc.perform(bearer(get("/api/employees/me"), token)).andExpect(status().isUnauthorized());
    }
}
