package com.example.EmployeeManagementSystem.controller;

import com.example.EmployeeManagementSystem.dto.request.AssignManagerRequest;
import com.example.EmployeeManagementSystem.dto.request.CreateDepartmentRequest;
import com.example.EmployeeManagementSystem.dto.request.UpdateDepartmentRequest;
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
class DepartmentControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private DepartmentRepository departmentRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private Department engineering;
    private User admin;
    private User manager;
    private User employee;

    @BeforeEach
    void setUp() {
        engineering = new Department();
        engineering.setName("Engineering");
        engineering = departmentRepository.save(engineering);

        admin = persistUser("dept_admin", Role.ADMIN, null);
        manager = persistUser("dept_manager", Role.MANAGER, null); // not yet assigned to head anything
        employee = persistUser("dept_employee", Role.EMPLOYEE, engineering);
    }

    private User persistUser(String username, Role role, Department department) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setPassword(passwordEncoder.encode("password123"));
        user.setRole(role);
        user.setFirstName("Test");
        user.setLastName("User");
        user.setDepartment(department);
        return userRepository.save(user);
    }

    // ---------------------------------------------------------------
    // POST /api/departments - @PreAuthorize("hasRole('ADMIN')")
    // ---------------------------------------------------------------
    @Nested
    @Transactional
    class CreateDepartment {

        @Test
        @WithMockUser(username = "dept_admin", roles = "ADMIN")
        void adminCanCreateADepartment() throws Exception {
            CreateDepartmentRequest request = new CreateDepartmentRequest();
            request.setName("Marketing");

            mockMvc.perform(post("/api/departments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("Marketing"));
        }

        @Test
        @WithMockUser(username = "dept_admin", roles = "ADMIN")
        void rejectsDuplicateName() throws Exception {
            CreateDepartmentRequest request = new CreateDepartmentRequest();
            request.setName("Engineering"); // already exists from setUp()

            mockMvc.perform(post("/api/departments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(username = "dept_manager", roles = "MANAGER")
        void managerCannotCreateADepartment() throws Exception {
            CreateDepartmentRequest request = new CreateDepartmentRequest();
            request.setName("Marketing");

            mockMvc.perform(post("/api/departments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "dept_employee", roles = "EMPLOYEE")
        void employeeCannotCreateADepartment() throws Exception {
            CreateDepartmentRequest request = new CreateDepartmentRequest();
            request.setName("Marketing");

            mockMvc.perform(post("/api/departments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        void unauthenticatedRequestIsRejected() throws Exception {
            CreateDepartmentRequest request = new CreateDepartmentRequest();
            request.setName("Marketing");

            mockMvc.perform(post("/api/departments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().is4xxClientError());
        }
    }

    // ---------------------------------------------------------------
    // GET /api/departments/{id} and GET /api/departments - no
    // @PreAuthorize, open to any authenticated role
    // ---------------------------------------------------------------
    @Nested
    @Transactional
    class ReadDepartments {

        @Test
        @WithMockUser(username = "dept_employee", roles = "EMPLOYEE")
        void anyAuthenticatedRoleCanViewADepartmentById() throws Exception {
            mockMvc.perform(get("/api/departments/" + engineering.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Engineering"));
        }

        @Test
        @WithMockUser(username = "dept_employee", roles = "EMPLOYEE")
        void anyAuthenticatedRoleCanListDepartments() throws Exception {
            mockMvc.perform(get("/api/departments"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(username = "dept_admin", roles = "ADMIN")
        void throwsNotFound_whenDepartmentDoesNotExist() throws Exception {
            mockMvc.perform(get("/api/departments/999999"))
                    .andExpect(status().isNotFound());
        }

        @Test
        void unauthenticatedRequestIsRejected() throws Exception {
            mockMvc.perform(get("/api/departments/" + engineering.getId()))
                    .andExpect(status().is4xxClientError());
        }
    }

    // ---------------------------------------------------------------
    // PUT /api/departments/{id} - @PreAuthorize("hasRole('ADMIN')")
    // ---------------------------------------------------------------
    @Nested
    @Transactional
    class UpdateDepartment {

        @Test
        @WithMockUser(username = "dept_admin", roles = "ADMIN")
        void adminCanRenameADepartment() throws Exception {
            UpdateDepartmentRequest request = new UpdateDepartmentRequest();
            request.setName("Platform Engineering");

            mockMvc.perform(put("/api/departments/" + engineering.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Platform Engineering"));
        }

        @Test
        @WithMockUser(username = "dept_manager", roles = "MANAGER")
        void managerCannotRenameADepartment() throws Exception {
            UpdateDepartmentRequest request = new UpdateDepartmentRequest();
            request.setName("Platform Engineering");

            mockMvc.perform(put("/api/departments/" + engineering.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    // ---------------------------------------------------------------
    // DELETE /api/departments/{id} - @PreAuthorize("hasRole('ADMIN')")
    // blocked if the department still has employees assigned
    // ---------------------------------------------------------------
    @Nested
    @Transactional
    class DeleteDepartment {

        @Test
        @WithMockUser(username = "dept_admin", roles = "ADMIN")
        void blocksDeletion_whenEmployeesStillAssigned() throws Exception {
            // engineering has `employee` assigned to it via setUp()
            mockMvc.perform(delete("/api/departments/" + engineering.getId()))
                    .andExpect(status().isConflict());
        }

        @Test
        @WithMockUser(username = "dept_admin", roles = "ADMIN")
        void deletesSuccessfully_whenEmpty() throws Exception {
            Department empty = new Department();
            empty.setName("Empty Department");
            empty = departmentRepository.save(empty);

            mockMvc.perform(delete("/api/departments/" + empty.getId()))
                    .andExpect(status().isNoContent());
        }

        @Test
        @WithMockUser(username = "dept_manager", roles = "MANAGER")
        void managerCannotDeleteADepartment() throws Exception {
            mockMvc.perform(delete("/api/departments/" + engineering.getId()))
                    .andExpect(status().isForbidden());
        }
    }

    // ---------------------------------------------------------------
    // PATCH /api/departments/{id}/manager - @PreAuthorize("hasRole('ADMIN')")
    // single-manager constraint enforced
    // ---------------------------------------------------------------
    @Nested
    @Transactional
    class AssignManager {

        @Test
        @WithMockUser(username = "dept_admin", roles = "ADMIN")
        void adminCanAssignAManager_whenDepartmentHasNone() throws Exception {
            AssignManagerRequest request = new AssignManagerRequest();
            request.setUserId(manager.getId());

            mockMvc.perform(patch("/api/departments/" + engineering.getId() + "/manager")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.managerUsername").value("dept_manager"));
        }

        @Test
        @WithMockUser(username = "dept_admin", roles = "ADMIN")
        void blocksAssignment_whenDepartmentAlreadyHasAManager() throws Exception {
            engineering.setManager(manager);
            departmentRepository.save(engineering);

            User anotherManager = persistUser("dept_manager2", Role.MANAGER, null);
            AssignManagerRequest request = new AssignManagerRequest();
            request.setUserId(anotherManager.getId());

            mockMvc.perform(patch("/api/departments/" + engineering.getId() + "/manager")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }

        @Test
        @WithMockUser(username = "dept_admin", roles = "ADMIN")
        void blocksAssignment_whenCandidateDoesNotHoldManagerRole() throws Exception {
            AssignManagerRequest request = new AssignManagerRequest();
            request.setUserId(employee.getId()); // EMPLOYEE, not MANAGER

            mockMvc.perform(patch("/api/departments/" + engineering.getId() + "/manager")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }

        @Test
        @WithMockUser(username = "dept_manager", roles = "MANAGER")
        void managerCannotAssignADepartmentManager() throws Exception {
            AssignManagerRequest request = new AssignManagerRequest();
            request.setUserId(manager.getId());

            mockMvc.perform(patch("/api/departments/" + engineering.getId() + "/manager")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    // ---------------------------------------------------------------
    // DELETE /api/departments/{id}/manager - @PreAuthorize("hasRole('ADMIN')")
    // ---------------------------------------------------------------
    @Nested
    @Transactional
    class RemoveManager {

        @Test
        @WithMockUser(username = "dept_admin", roles = "ADMIN")
        void adminCanRemoveTheCurrentManager() throws Exception {
            engineering.setManager(manager);
            departmentRepository.save(engineering);

            mockMvc.perform(delete("/api/departments/" + engineering.getId() + "/manager"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.managerId").value(org.hamcrest.Matchers.nullValue()));
        }

        @Test
        @WithMockUser(username = "dept_manager", roles = "MANAGER")
        void managerCannotRemoveADepartmentManager() throws Exception {
            engineering.setManager(manager);
            departmentRepository.save(engineering);

            mockMvc.perform(delete("/api/departments/" + engineering.getId() + "/manager"))
                    .andExpect(status().isForbidden());
        }
    }
}