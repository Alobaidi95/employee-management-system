package com.example.EmployeeManagementSystem.controller;

import com.example.EmployeeManagementSystem.model.*;
import com.example.EmployeeManagementSystem.repository.AuditLogRepository;
import com.example.EmployeeManagementSystem.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuditLogControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private User admin;
    private User employee;

    @BeforeEach
    void setUp() {
        admin = persistUser("audit_admin", Role.ADMIN);
        employee = persistUser("audit_employee", Role.EMPLOYEE);

        persistLog(admin, "CREATE_USER", employee.getId(), AuditTargetType.USER, "Created user 'audit_employee'");
        persistLog(admin, "CHANGE_ROLE", employee.getId(), AuditTargetType.USER, "Changed role of 'audit_employee'");
    }

    private User persistUser(String username, Role role) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setPassword(passwordEncoder.encode("password123"));
        user.setRole(role);
        user.setFirstName("Test");
        user.setLastName("User");
        return userRepository.save(user);
    }

    private void persistLog(User performedBy, String action, Long targetId, AuditTargetType targetType, String details) {
        AuditLog log = new AuditLog();
        log.setPerformedBy(performedBy);
        log.setAction(action);
        log.setTargetId(targetId);
        log.setTargetType(targetType);
        log.setDetails(details);
        log.setTimestamp(LocalDateTime.now());
        auditLogRepository.save(log);
    }

    // ---------------------------------------------------------------
    // GET /api/audit-logs
    // ---------------------------------------------------------------
    @Nested
    @Transactional
    class GetAllLogs {

        @Test
        @WithMockUser(username = "audit_admin", roles = "ADMIN")
        void adminCanViewAllLogs() throws Exception {
            mockMvc.perform(get("/api/audit-logs"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());
        }

        @Test
        @WithMockUser(username = "audit_employee", roles = "EMPLOYEE")
        void employeeCannotViewLogs() throws Exception {
            mockMvc.perform(get("/api/audit-logs"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void unauthenticatedRequestIsRejected() throws Exception {
            mockMvc.perform(get("/api/audit-logs"))
                    .andExpect(status().is4xxClientError());
        }
    }

    // ---------------------------------------------------------------
    // GET /api/audit-logs/by-user/{userId}
    // ---------------------------------------------------------------
    @Nested
    @Transactional
    class GetLogsByUser {

        @Test
        @WithMockUser(username = "audit_admin", roles = "ADMIN")
        void adminCanViewLogsForASpecificUser() throws Exception {
            mockMvc.perform(get("/api/audit-logs/by-user/" + admin.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());
        }

        @Test
        @WithMockUser(username = "audit_admin", roles = "ADMIN")
        void throwsNotFound_whenUserDoesNotExist() throws Exception {
            mockMvc.perform(get("/api/audit-logs/by-user/999999"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser(username = "audit_employee", roles = "EMPLOYEE")
        void employeeCannotViewLogsByUser() throws Exception {
            mockMvc.perform(get("/api/audit-logs/by-user/" + admin.getId()))
                    .andExpect(status().isForbidden());
        }
    }

    // ---------------------------------------------------------------
    // GET /api/audit-logs/by-target/{targetId}?targetType=USER
    // ---------------------------------------------------------------
    @Nested
    @Transactional
    class GetLogsForTarget {

        @Test
        @WithMockUser(username = "audit_admin", roles = "ADMIN")
        void adminCanViewLogsForASpecificTarget() throws Exception {
            mockMvc.perform(get("/api/audit-logs/by-target/" + employee.getId())
                            .param("targetType", "USER"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());
        }

        @Test
        @WithMockUser(username = "audit_employee", roles = "EMPLOYEE")
        void employeeCannotViewLogsByTarget() throws Exception {
            mockMvc.perform(get("/api/audit-logs/by-target/" + employee.getId())
                            .param("targetType", "USER"))
                    .andExpect(status().isForbidden());
        }
    }
}