package com.example.EmployeeLeaveManagementSystem.Config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(LeaveProperties.class)
public class AppConfig {
}
