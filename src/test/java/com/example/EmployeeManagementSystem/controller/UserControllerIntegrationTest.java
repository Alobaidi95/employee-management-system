package com.example.EmployeeManagementSystem.controller;

import com.example.EmployeeManagementSystem.dto.request.*;
import com.example.EmployeeManagementSystem.model.Department;
import com.example.EmployeeManagementSystem.model.Role;
import com.example.EmployeeManagementSystem.model.User;
import com.example.EmployeeManagementSystem.repository.DepartmentRepository;
import com.example.EmployeeManagementSystem.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private DepartmentRepository departmentRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private Department engineering;
    private User targetEmployee;

    @BeforeEach
    void setUp() {
        engineering = new Department();
        engineering.setName("Engineering");
        engineering = departmentRepository.save(engineering);

        User admin = persistUser("admin", "admin@example.com", Role.ADMIN, null);

        User manager = persistUser("jack", "jack@example.com", Role.MANAGER, engineering);
        engineering.setManager(manager);
        departmentRepository.save(engineering);

        persistUser("john", "john@example.com", Role.EMPLOYEE, engineering);

        targetEmployee = persistUser("target", "target@example.com", Role.EMPLOYEE, null);
    }

    private User persistUser(String username, String email, Role role, Department department) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode("password123"));
        user.setRole(role);
        user.setFirstName("Test");
        user.setLastName("User");
        user.setDepartment(department);
        return userRepository.save(user);
    }

    // ---------------------------------------------------------------
    // POST /api/users - @PreAuthorize("hasRole('ADMIN')")
    // ---------------------------------------------------------------
    @Nested
    class CreateUser {

        private CreateUserRequest validRequest() {
            return new CreateUserRequest("newuser", "test123", "newuser@test.com",
                    "New", "User", Role.EMPLOYEE, null);
        }

        @Test
        @WithMockUser(username = "admin", roles = "ADMIN")
        void adminCanCreateAUser() throws Exception {
            mockMvc.perform(post("/api/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.username").value("newuser"));
        }

        @Test
        @WithMockUser(username = "jack", roles = "MANAGER")
        void managerCannotCreateAUser() throws Exception {
            mockMvc.perform(post("/api/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest())))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "john", roles = "EMPLOYEE")
        void employeeCannotCreateAUser() throws Exception {
            mockMvc.perform(post("/api/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest())))
                    .andExpect(status().isForbidden());
        }

        @Test
        void unauthenticatedRequestIsRejected() throws Exception {
            mockMvc.perform(post("/api/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest())))
                    .andExpect(status().is4xxClientError());
        }
    }

    // ---------------------------------------------------------------
    // DELETE /api/users/{id} - @PreAuthorize("hasRole('ADMIN')")
    // ---------------------------------------------------------------
    @Nested
    class DeleteUser {

        @Test
        @WithMockUser(username = "admin", roles = "ADMIN")
        void adminCanDeleteAUser() throws Exception {
            mockMvc.perform(delete("/api/users/" + targetEmployee.getId()))
                    .andExpect(status().isNoContent());
        }

        @Test
        @WithMockUser(username = "jack", roles = "MANAGER")
        void managerCannotDeleteAUser() throws Exception {
            mockMvc.perform(delete("/api/users/" + targetEmployee.getId()))
                    .andExpect(status().isForbidden());
        }
    }

    // ---------------------------------------------------------------
    // PATCH /api/users/{id}/role - @PreAuthorize("hasRole('ADMIN')")
    // ---------------------------------------------------------------
    @Nested
    class ChangeRole {

        @Test
        @WithMockUser(username = "admin", roles = "ADMIN")
        void adminCanChangeARole() throws Exception {
            ChangeRoleRequest request = new ChangeRoleRequest(Role.MANAGER);
            // give the target a department with no existing manager so the
            // promotion rule doesn't block this on an unrelated constraint
            targetEmployee.setDepartment(engineering);
            userRepository.save(targetEmployee);
            engineering.setManager(null);
            departmentRepository.save(engineering);

            mockMvc.perform(patch("/api/users/" + targetEmployee.getId() + "/role")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.role").value("MANAGER"));
        }

        @Test
        @WithMockUser(username = "john", roles = "EMPLOYEE")
        void employeeCannotChangeARole() throws Exception {
            ChangeRoleRequest request = new ChangeRoleRequest(Role.MANAGER);

            mockMvc.perform(patch("/api/users/" + targetEmployee.getId() + "/role")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    // ---------------------------------------------------------------
    // PATCH /api/users/{id}/department - @PreAuthorize("hasRole('ADMIN')")
    // ---------------------------------------------------------------
    @Nested
    class AssignDepartment {

        @Test
        @WithMockUser(username = "admin", roles = "ADMIN")
        void adminCanAssignADepartment() throws Exception {
            AssignDepartmentRequest request = new AssignDepartmentRequest();
            request.setDepartmentId(engineering.getId());

            mockMvc.perform(patch("/api/users/" + targetEmployee.getId() + "/department")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.departmentName").value("Engineering"));
        }

        @Test
        @WithMockUser(username = "jack", roles = "MANAGER")
        void managerCannotAssignADepartment() throws Exception {
            AssignDepartmentRequest request = new AssignDepartmentRequest();
            request.setDepartmentId(engineering.getId());

            mockMvc.perform(patch("/api/users/" + targetEmployee.getId() + "/department")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    // ---------------------------------------------------------------
    // PUT /api/users/{id} - @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    // plus a manual same-department check for managers
    // ---------------------------------------------------------------
    @Nested
    class UpdateUser {

        @Test
        @WithMockUser(username = "jack", roles = "MANAGER")
        void managerCanUpdateAnEmployeeInTheirOwnDepartment() throws Exception {
            User employeeInDept = userRepository.findByUsername("john").orElseThrow();
            UpdateUserRequest request = new UpdateUserRequest("Jane", "Doe", "jane.doe@example.com");

            mockMvc.perform(put("/api/users/" + employeeInDept.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.firstName").value("Jane"));
        }

        @Test
        @WithMockUser(username = "john", roles = "EMPLOYEE")
        void employeeCannotUpdateAnyoneAtAll() throws Exception {
            // blocked by @PreAuthorize before the method body's department
            // check ever runs
            UpdateUserRequest request = new UpdateUserRequest("Jane", "Doe", "jane.doe@example.com");

            mockMvc.perform(put("/api/users/" + targetEmployee.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "jack", roles = "MANAGER")
        void managerCannotUpdateAnEmployeeOutsideTheirDepartment() throws Exception {
            // targetEmployee has no department at all, so isManagerOfUsersDepartment() is false
            UpdateUserRequest request = new UpdateUserRequest("Jane", "Doe", "jane.doe@example.com");

            mockMvc.perform(put("/api/users/" + targetEmployee.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }
}