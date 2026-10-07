package com.example.EmployeeLeaveManagementSystem.Integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LeaveFlowIntegrationTest extends AbstractIntegrationTest {

    private final LocalDate start = LocalDate.now().plusDays(7);

    private ResultActions apply(String token, LocalDate from, LocalDate to) throws Exception {
        Long casualId = leaveTypeRepository.findByNameIgnoreCase("CASUAL").orElseThrow().getId();
        return mockMvc.perform(bearer(post("/api/leaves"), token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("leaveTypeId", casualId, "startDate", from.toString(),
                        "endDate", to.toString(), "reason", "Family function"))));
    }

    private long applyAndGetId(String token, LocalDate from, LocalDate to) throws Exception {
        MvcResult result = apply(token, from, to).andExpect(status().isCreated()).andReturn();
        return read(result).at("/data/id").asLong();
    }

    private void assertCasualBalance(String token, int used, int remaining) throws Exception {
        mockMvc.perform(bearer(get("/api/leave-balances/my"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.leaveTypeName=='CASUAL')].usedDays").value(used))
                .andExpect(jsonPath("$.data[?(@.leaveTypeName=='CASUAL')].remainingDays").value(remaining));
    }

    @Test
    void applyThenApprove_deductsBalanceOnlyAfterApproval() throws Exception {
        String employeeToken = registerAndLogin("EMP400", "emp400@gmail.com");
        String adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);

        MvcResult applied = apply(employeeToken, start, start.plusDays(4))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.numberOfDays").value(5))
                .andReturn();
        long leaveId = read(applied).at("/data/id").asLong();
        assertCasualBalance(employeeToken, 0, 12);            // nothing deducted while PENDING

        mockMvc.perform(bearer(put("/api/leaves/" + leaveId + "/approve"), adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.approvedByName").value("System Admin"));
        assertCasualBalance(employeeToken, 5, 7);

        // a second approval must not double-deduct
        mockMvc.perform(bearer(put("/api/leaves/" + leaveId + "/approve"), adminToken))
                .andExpect(status().isConflict());
        assertCasualBalance(employeeToken, 5, 7);
    }

    @Test
    void reject_storesReason_andKeepsBalance() throws Exception {
        String employeeToken = registerAndLogin("EMP401", "emp401@gmail.com");
        String adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);
        long leaveId = applyAndGetId(employeeToken, start, start.plusDays(2));

        mockMvc.perform(bearer(put("/api/leaves/" + leaveId + "/reject"), adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("rejectionReason", "Project deadline is approaching."))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"))
                .andExpect(jsonPath("$.data.rejectionReason").value("Project deadline is approaching."));
        assertCasualBalance(employeeToken, 0, 12);
    }

    @Test
    void cancelPendingLeave_changesNoBalance() throws Exception {
        String employeeToken = registerAndLogin("EMP402", "emp402@gmail.com");
        long leaveId = applyAndGetId(employeeToken, start, start.plusDays(1));

        mockMvc.perform(bearer(put("/api/leaves/" + leaveId + "/cancel"), employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
        assertCasualBalance(employeeToken, 0, 12);
    }

    @Test
    void overlappingLeave_isRejectedWith409() throws Exception {
        String employeeToken = registerAndLogin("EMP403", "emp403@gmail.com");
        applyAndGetId(employeeToken, start, start.plusDays(3));

        apply(employeeToken, start.plusDays(2), start.plusDays(5)).andExpect(status().isConflict());
    }

    @Test
    void insufficientBalance_isRejectedWith400() throws Exception {
        String employeeToken = registerAndLogin("EMP404", "emp404@gmail.com");

        apply(employeeToken, start, start.plusDays(12))       // 13 days, only 12 CASUAL allocated
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("insufficient")));
    }

    @Test
    void pastStartDate_returnsValidationError() throws Exception {
        String employeeToken = registerAndLogin("EMP405", "emp405@gmail.com");

        apply(employeeToken, LocalDate.now().minusDays(1), start)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.startDate").value("Start date cannot be in the past"));
    }

    @Test
    void employee_cannotSeeAnotherEmployeesLeave() throws Exception {
        String ownerToken = registerAndLogin("EMP406", "emp406@gmail.com");
        String otherToken = registerAndLogin("EMP407", "emp407@gmail.com");
        long leaveId = applyAndGetId(ownerToken, start, start.plusDays(1));

        mockMvc.perform(bearer(get("/api/leaves/my/" + leaveId), otherToken)).andExpect(status().isNotFound());
        mockMvc.perform(bearer(put("/api/leaves/" + leaveId + "/cancel"), otherToken)).andExpect(status().isNotFound());
        mockMvc.perform(bearer(get("/api/leaves/my/" + leaveId), ownerToken)).andExpect(status().isOk());
    }
}
