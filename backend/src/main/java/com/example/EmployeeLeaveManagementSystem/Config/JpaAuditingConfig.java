package com.example.EmployeeLeaveManagementSystem.Config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Turns on Spring Data auditing. Entities annotated with @EntityListeners(AuditingEntityListener.class)
 * get @CreatedDate / @LastModifiedDate filled automatically on insert / update.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
