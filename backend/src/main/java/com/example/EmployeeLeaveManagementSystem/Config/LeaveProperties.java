package com.example.EmployeeLeaveManagementSystem.Config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.leave")
public record LeaveProperties(boolean excludeWeekends) {
}
