package com.example.EmployeeManagementSystem.controller;

import com.example.EmployeeManagementSystem.dto.request.CreateTaskRequest;
import com.example.EmployeeManagementSystem.dto.request.UpdateTaskRequest;
import com.example.EmployeeManagementSystem.model.*;
import com.example.EmployeeManagementSystem.repository.DepartmentRepository;
import com.example.EmployeeManagementSystem.repository.TaskRepository;
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

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;



@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TaskControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private DepartmentRepository departmentRepository;
    @Autowired private TaskRepository taskRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private Department engineering;
    private Department sales;
    private User admin;
    private User manager;
    private User employeeSameDept;
    private User employeeOtherDept;

    @BeforeEach
    void setUp() {
        engineering = departmentRepository.save(newDept("Engineering"));
        sales = departmentRepository.save(newDept("Sales"));

        admin = persistUser("task_admin", Role.ADMIN, null);
        manager = persistUser("task_manager", Role.MANAGER, engineering);
        engineering.setManager(manager);
        departmentRepository.save(engineering);

        employeeSameDept = persistUser("task_employee_same", Role.EMPLOYEE, engineering);
        employeeOtherDept = persistUser("task_employee_other", Role.EMPLOYEE, sales);
    }

    private Department newDept(String name) {
        Department d = new Department();
        d.setName(name);
        return d;
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

    private Task persistTask(User assignedTo, User assignedBy, Status status) {
        Task task = new Task();
        task.setTitle("Sample task");
        task.setDescription("Sample description");
        task.setAssignedTo(assignedTo);
        task.setAssignedBy(assignedBy);
        task.setStatus(status);
        task.setStartDate(LocalDate.now());
        task.setEndDate(LocalDate.now().plusDays(7));
        return taskRepository.save(task);
    }

    private CreateTaskRequest createRequest(Long assignedToId, LocalDate start, LocalDate end) {
        CreateTaskRequest r = new CreateTaskRequest();
        r.setTitle("New task");
        r.setDescription("Do the thing");
        r.setAssignedToId(assignedToId);
        r.setStartDate(start);
        r.setEndDate(end);
        return r;
    }

    // ---------------------------------------------------------------
    // POST /api/tasks
    // ---------------------------------------------------------------
    @Nested
    @Transactional
    class CreateTask {

        @Test
        @WithMockUser(username = "task_admin", roles = "ADMIN")
        void adminCanAssignToAManager() throws Exception {
            CreateTaskRequest request = createRequest(manager.getId(), LocalDate.now(), LocalDate.now().plusDays(5));

            mockMvc.perform(post("/api/tasks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.assignedToUsername").value("task_manager"))
                    .andExpect(jsonPath("$.status").value("ASSIGNED"));
        }

        @Test
        @WithMockUser(username = "task_admin", roles = "ADMIN")
        void adminCannotAssignToAnEmployee() throws Exception {
            CreateTaskRequest request = createRequest(employeeSameDept.getId(), LocalDate.now(), LocalDate.now().plusDays(5));

            mockMvc.perform(post("/api/tasks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "task_manager", roles = "MANAGER")
        void managerCanAssignToEmployeeInOwnDepartment() throws Exception {
            CreateTaskRequest request = createRequest(employeeSameDept.getId(), LocalDate.now(), LocalDate.now().plusDays(5));

            mockMvc.perform(post("/api/tasks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        @Test
        @WithMockUser(username = "task_manager", roles = "MANAGER")
        void managerCannotAssignToEmployeeInAnotherDepartment() throws Exception {
            CreateTaskRequest request = createRequest(employeeOtherDept.getId(), LocalDate.now(), LocalDate.now().plusDays(5));

            mockMvc.perform(post("/api/tasks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "task_employee_same", roles = "EMPLOYEE")
        void employeeCannotCreateTasksAtAll() throws Exception {
            CreateTaskRequest request = createRequest(employeeOtherDept.getId(), LocalDate.now(), LocalDate.now().plusDays(5));

            mockMvc.perform(post("/api/tasks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "task_admin", roles = "ADMIN")
        void rejectsEndDateBeforeStartDate() throws Exception {
            CreateTaskRequest request = createRequest(manager.getId(), LocalDate.now(), LocalDate.now().minusDays(1));

            mockMvc.perform(post("/api/tasks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }

        @Test
        void unauthenticatedRequestIsRejected() throws Exception {
            CreateTaskRequest request = createRequest(manager.getId(), LocalDate.now(), LocalDate.now().plusDays(5));

            mockMvc.perform(post("/api/tasks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().is4xxClientError());
        }
    }

    // ---------------------------------------------------------------
    // GET /api/tasks/{id}
    // ---------------------------------------------------------------
    @Nested
    @Transactional
    class GetTaskById {

        @Test
        @WithMockUser(username = "task_admin", roles = "ADMIN")
        void adminCanViewAnyTask() throws Exception {
            Task task = persistTask(employeeOtherDept, manager, Status.ASSIGNED);

            mockMvc.perform(get("/api/tasks/" + task.getId()))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(username = "task_employee_same", roles = "EMPLOYEE")
        void employeeCanViewOwnTask() throws Exception {
            Task task = persistTask(employeeSameDept, manager, Status.ASSIGNED);

            mockMvc.perform(get("/api/tasks/" + task.getId()))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(username = "task_employee_same", roles = "EMPLOYEE")
        void employeeCannotViewSomeoneElsesTask() throws Exception {
            Task task = persistTask(employeeOtherDept, manager, Status.ASSIGNED);

            mockMvc.perform(get("/api/tasks/" + task.getId()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "task_admin", roles = "ADMIN")
        void throwsNotFound_whenTaskDoesNotExist() throws Exception {
            mockMvc.perform(get("/api/tasks/999999"))
                    .andExpect(status().isNotFound());
        }
    }

    // ---------------------------------------------------------------
    // The 4-state workflow, driven end to end over real HTTP:
    // ASSIGNED -> STARTED -> UNDER_REVIEW -> DONE (or back to ASSIGNED via reject)
    // ---------------------------------------------------------------
    @Nested
    @Transactional
    class Workflow {

        @Test
        @WithMockUser(username = "task_employee_same", roles = "EMPLOYEE")
        void assigneeCanStartAnAssignedTask() throws Exception {
            Task task = persistTask(employeeSameDept, manager, Status.ASSIGNED);

            mockMvc.perform(patch("/api/tasks/" + task.getId() + "/start"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("STARTED"));
        }

        @Test
        @WithMockUser(username = "task_manager", roles = "MANAGER")
        void nonAssigneeCannotStartTheTask() throws Exception {
            // task_manager is the assigner here, not the assignee
            Task task = persistTask(employeeSameDept, manager, Status.ASSIGNED);

            mockMvc.perform(patch("/api/tasks/" + task.getId() + "/start"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "task_employee_same", roles = "EMPLOYEE")
        void cannotStartATaskThatIsNotInAssignedState() throws Exception {
            Task task = persistTask(employeeSameDept, manager, Status.STARTED);

            mockMvc.perform(patch("/api/tasks/" + task.getId() + "/start"))
                    .andExpect(status().isConflict());
        }

        @Test
        @WithMockUser(username = "task_employee_same", roles = "EMPLOYEE")
        void assigneeCanSubmitAStartedTaskForReview() throws Exception {
            Task task = persistTask(employeeSameDept, manager, Status.STARTED);

            mockMvc.perform(patch("/api/tasks/" + task.getId() + "/submit-for-review"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("UNDER_REVIEW"));
        }

        @Test
        @WithMockUser(username = "task_manager", roles = "MANAGER")
        void assignerCanApproveAnUnderReviewTask() throws Exception {
            Task task = persistTask(employeeSameDept, manager, Status.UNDER_REVIEW);

            mockMvc.perform(patch("/api/tasks/" + task.getId() + "/approve"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("DONE"));
        }

        @Test
        @WithMockUser(username = "task_employee_same", roles = "EMPLOYEE")
        void assigneeCannotApproveTheirOwnTask() throws Exception {
            Task task = persistTask(employeeSameDept, manager, Status.UNDER_REVIEW);

            mockMvc.perform(patch("/api/tasks/" + task.getId() + "/approve"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "task_manager", roles = "MANAGER")
        void assignerCanRejectAnUnderReviewTaskBackToAssigned() throws Exception {
            Task task = persistTask(employeeSameDept, manager, Status.UNDER_REVIEW);

            mockMvc.perform(patch("/api/tasks/" + task.getId() + "/reject"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("ASSIGNED"));
        }

        @Test
        @WithMockUser(username = "task_admin", roles = "ADMIN")
        void adminCanApproveEvenWithoutBeingTheAssigner() throws Exception {
            Task task = persistTask(employeeSameDept, manager, Status.UNDER_REVIEW);

            mockMvc.perform(patch("/api/tasks/" + task.getId() + "/approve"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("DONE"));
        }
    }

    // ---------------------------------------------------------------
    // PUT /api/tasks/{id}
    // ---------------------------------------------------------------
    @Nested
    @Transactional
    class UpdateTask {

        private UpdateTaskRequest validRequest() {
            UpdateTaskRequest r = new UpdateTaskRequest();
            r.setTitle("Updated title");
            r.setDescription("Updated description");
            r.setStartDate(LocalDate.now());
            r.setEndDate(LocalDate.now().plusDays(3));
            return r;
        }

        @Test
        @WithMockUser(username = "task_manager", roles = "MANAGER")
        void assignerCanUpdateTheirOwnTask() throws Exception {
            Task task = persistTask(employeeSameDept, manager, Status.ASSIGNED);

            mockMvc.perform(put("/api/tasks/" + task.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Updated title"));
        }

        @Test
        @WithMockUser(username = "task_employee_same", roles = "EMPLOYEE")
        void nonAssignerNonAdminCannotUpdate() throws Exception {
            Task task = persistTask(employeeSameDept, manager, Status.ASSIGNED);

            mockMvc.perform(put("/api/tasks/" + task.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest())))
                    .andExpect(status().isForbidden());
        }
    }

    // ---------------------------------------------------------------
    // DELETE /api/tasks/{id}
    // ---------------------------------------------------------------
    @Nested
    @Transactional
    class DeleteTask {

        @Test
        @WithMockUser(username = "task_manager", roles = "MANAGER")
        void assignerCanDeleteTheirOwnTask() throws Exception {
            Task task = persistTask(employeeSameDept, manager, Status.ASSIGNED);

            mockMvc.perform(delete("/api/tasks/" + task.getId()))
                    .andExpect(status().isNoContent());
        }

        @Test
        @WithMockUser(username = "task_employee_same", roles = "EMPLOYEE")
        void nonAssignerNonAdminCannotDelete() throws Exception {
            Task task = persistTask(employeeSameDept, manager, Status.ASSIGNED);

            mockMvc.perform(delete("/api/tasks/" + task.getId()))
                    .andExpect(status().isForbidden());
        }
    }
}