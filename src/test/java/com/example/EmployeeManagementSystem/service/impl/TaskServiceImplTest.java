package com.example.EmployeeManagementSystem.service.impl;

import com.example.EmployeeManagementSystem.dto.request.CreateTaskRequest;
import com.example.EmployeeManagementSystem.dto.request.UpdateTaskRequest;
import com.example.EmployeeManagementSystem.dto.response.PageResponse;
import com.example.EmployeeManagementSystem.dto.response.TaskResponse;
import com.example.EmployeeManagementSystem.exception.InvalidTaskStateException;
import com.example.EmployeeManagementSystem.exception.ResourceNotFoundException;
import com.example.EmployeeManagementSystem.exception.UnauthorizedAccessException;
import com.example.EmployeeManagementSystem.model.*;
import com.example.EmployeeManagementSystem.repository.TaskRepository;
import com.example.EmployeeManagementSystem.repository.UserRepository;
import com.example.EmployeeManagementSystem.service.AuditLogService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class TaskServiceImplTest {

    @Mock private TaskRepository taskRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuditLogService auditLogService;

    private TaskServiceImpl taskService;

    private User admin;
    private User manager;
    private User employeeSameDept;
    private User employeeOtherDept;
    private Department engineering;
    private Department sales;
    private final Pageable pageable = PageRequest.of(0, 20);

    @BeforeEach
    void setUp() {
        taskService = new TaskServiceImpl(taskRepository, userRepository, auditLogService);

        engineering = new Department();
        engineering.setId(10L);
        engineering.setName("Engineering");

        sales = new Department();
        sales.setId(20L);
        sales.setName("Sales");

        admin = new User();
        admin.setId(1L);
        admin.setUsername("admin");
        admin.setRole(Role.ADMIN);

        manager = new User();
        manager.setId(2L);
        manager.setUsername("msmith");
        manager.setRole(Role.MANAGER);
        manager.setDepartment(engineering);
        engineering.setManager(manager);

        employeeSameDept = new User();
        employeeSameDept.setId(3L);
        employeeSameDept.setUsername("jdoe");
        employeeSameDept.setRole(Role.EMPLOYEE);
        employeeSameDept.setDepartment(engineering);

        employeeOtherDept = new User();
        employeeOtherDept.setId(4L);
        employeeOtherDept.setUsername("bwayne");
        employeeOtherDept.setRole(Role.EMPLOYEE);
        employeeOtherDept.setDepartment(sales);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(User user) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user.getUsername(), null));
        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));
    }

    private Task taskWith(Long id, User assignedTo, User assignedBy, Status status) {
        Task task = new Task();
        task.setId(id);
        task.setTitle("Some task");
        task.setDescription("Description");
        task.setAssignedTo(assignedTo);
        task.setAssignedBy(assignedBy);
        task.setStatus(status);
        task.setStartDate(LocalDate.now());
        task.setEndDate(LocalDate.now().plusDays(7));
        return task;
    }

    // ---------------------------------------------------------------
    // createTask
    // ---------------------------------------------------------------
    @Nested
    class CreateTask {

        private CreateTaskRequest requestFor(Long assignedToId) {
            CreateTaskRequest r = new CreateTaskRequest();
            r.setTitle("New task");
            r.setDescription("Do the thing");
            r.setAssignedToId(assignedToId);
            r.setStartDate(LocalDate.now());
            r.setEndDate(LocalDate.now().plusDays(5));
            return r;
        }

        @Test
        void adminCanAssignToAManager() {
            authenticateAs(admin);
            when(userRepository.findById(2L)).thenReturn(Optional.of(manager));
            when(taskRepository.save(any(Task.class))).thenAnswer(inv -> {
                Task t = inv.getArgument(0);
                t.setId(100L);
                return t;
            });

            TaskResponse response = taskService.createTask(requestFor(2L));

            assertThat(response.getAssignedToUsername()).isEqualTo("msmith");
            assertThat(response.getStatus()).isEqualTo(Status.ASSIGNED);
        }

        @Test
        void adminCannotAssignToAnEmployee() {
            authenticateAs(admin);
            when(userRepository.findById(3L)).thenReturn(Optional.of(employeeSameDept));

            assertThatThrownBy(() -> taskService.createTask(requestFor(3L)))
                    .isInstanceOf(UnauthorizedAccessException.class);

            verify(taskRepository, never()).save(any());
        }

        @Test
        void managerCanAssignToEmployeeInOwnDepartment() {
            authenticateAs(manager);
            when(userRepository.findById(3L)).thenReturn(Optional.of(employeeSameDept));
            when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

            TaskResponse response = taskService.createTask(requestFor(3L));

            assertThat(response.getAssignedToUsername()).isEqualTo("jdoe");
        }

        @Test
        void managerCannotAssignToEmployeeInAnotherDepartment() {
            authenticateAs(manager);
            when(userRepository.findById(4L)).thenReturn(Optional.of(employeeOtherDept));

            assertThatThrownBy(() -> taskService.createTask(requestFor(4L)))
                    .isInstanceOf(UnauthorizedAccessException.class);
        }

        @Test
        void managerCannotAssignToAnotherManager() {
            authenticateAs(manager);
            User otherManager = new User();
            otherManager.setId(6L);
            otherManager.setRole(Role.MANAGER);
            otherManager.setDepartment(engineering);
            when(userRepository.findById(6L)).thenReturn(Optional.of(otherManager));

            assertThatThrownBy(() -> taskService.createTask(requestFor(6L)))
                    .isInstanceOf(UnauthorizedAccessException.class);
        }

        @Test
        void employeeCannotCreateTasksAtAll() {
            authenticateAs(employeeSameDept);
            when(userRepository.findById(4L)).thenReturn(Optional.of(employeeOtherDept));

            assertThatThrownBy(() -> taskService.createTask(requestFor(4L)))
                    .isInstanceOf(UnauthorizedAccessException.class);
        }

        @Test
        void rejectsEndDateBeforeStartDate() {
            authenticateAs(admin);
            when(userRepository.findById(2L)).thenReturn(Optional.of(manager));
            CreateTaskRequest request = requestFor(2L);
            request.setStartDate(LocalDate.now());
            request.setEndDate(LocalDate.now().minusDays(1));

            assertThatThrownBy(() -> taskService.createTask(request))
                    .isInstanceOf(InvalidTaskStateException.class);

            verify(taskRepository, never()).save(any());
        }
    }

    // ---------------------------------------------------------------
    // getTaskById - canView() visibility rules
    // ---------------------------------------------------------------
    @Nested
    class GetTaskById {

        @Test
        void adminCanViewAnyTask() {
            authenticateAs(admin);
            Task task = taskWith(100L, employeeOtherDept, manager, Status.ASSIGNED);
            when(taskRepository.findById(100L)).thenReturn(Optional.of(task));

            TaskResponse response = taskService.getTaskById(100L);

            assertThat(response.getId()).isEqualTo(100L);
        }

        @Test
        void managerCanViewTaskTheyAssigned_evenOutsideTheirDepartment() {
            authenticateAs(manager);
            Task task = taskWith(100L, employeeOtherDept, manager, Status.ASSIGNED);
            when(taskRepository.findById(100L)).thenReturn(Optional.of(task));

            TaskResponse response = taskService.getTaskById(100L);

            assertThat(response.getId()).isEqualTo(100L);
        }

        @Test
        void managerCanViewTaskInOwnDepartment_evenIfNotTheAssigner() {
            authenticateAs(manager);
            User anotherManager = new User();
            anotherManager.setId(7L);
            Task task = taskWith(100L, employeeSameDept, anotherManager, Status.ASSIGNED);
            when(taskRepository.findById(100L)).thenReturn(Optional.of(task));

            TaskResponse response = taskService.getTaskById(100L);

            assertThat(response.getId()).isEqualTo(100L);
        }

        @Test
        void managerCannotViewTaskOutsideDepartmentTheyDidNotAssign() {
            authenticateAs(manager);
            User anotherManager = new User();
            anotherManager.setId(7L);
            Task task = taskWith(100L, employeeOtherDept, anotherManager, Status.ASSIGNED);
            when(taskRepository.findById(100L)).thenReturn(Optional.of(task));

            assertThatThrownBy(() -> taskService.getTaskById(100L))
                    .isInstanceOf(UnauthorizedAccessException.class);
        }

        @Test
        void employeeCanViewOwnTask() {
            authenticateAs(employeeSameDept);
            Task task = taskWith(100L, employeeSameDept, manager, Status.ASSIGNED);
            when(taskRepository.findById(100L)).thenReturn(Optional.of(task));

            TaskResponse response = taskService.getTaskById(100L);

            assertThat(response.getId()).isEqualTo(100L);
        }

        @Test
        void employeeCannotViewSomeoneElsesTask() {
            authenticateAs(employeeSameDept);
            Task task = taskWith(100L, employeeOtherDept, manager, Status.ASSIGNED);
            when(taskRepository.findById(100L)).thenReturn(Optional.of(task));

            assertThatThrownBy(() -> taskService.getTaskById(100L))
                    .isInstanceOf(UnauthorizedAccessException.class);
        }

        @Test
        void throwsNotFound_whenTaskDoesNotExist() {
            authenticateAs(admin);
            when(taskRepository.findById(404L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> taskService.getTaskById(404L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ---------------------------------------------------------------
    // getAllTasks(Pageable)
    // ---------------------------------------------------------------
    @Nested
    class GetAllTasks {

        @Test
        void adminSeesAllTasks() {
            authenticateAs(admin);
            Task t1 = taskWith(100L, employeeSameDept, manager, Status.ASSIGNED);
            Page<Task> page = new PageImpl<>(List.of(t1), pageable, 1);
            when(taskRepository.findAll(pageable)).thenReturn(page);

            PageResponse<TaskResponse> result = taskService.getAllTasks(pageable);

            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        void managerWithDepartmentUsesVisibleToManagerQuery() {
            authenticateAs(manager);
            Task t1 = taskWith(100L, employeeSameDept, manager, Status.ASSIGNED);
            Page<Task> page = new PageImpl<>(List.of(t1), pageable, 1);
            when(taskRepository.findVisibleToManager(manager, engineering, pageable)).thenReturn(page);

            PageResponse<TaskResponse> result = taskService.getAllTasks(pageable);

            assertThat(result.getContent()).hasSize(1);
            verify(taskRepository, never()).findByAssignedBy(any(), any());
        }

        @Test
        void managerWithoutDepartmentFallsBackToAssignedByOnly() {
            User managerNoDept = new User();
            managerNoDept.setId(8L);
            managerNoDept.setUsername("floater");
            managerNoDept.setRole(Role.MANAGER);
            managerNoDept.setDepartment(null);
            authenticateAs(managerNoDept);

            Task t1 = taskWith(100L, employeeSameDept, managerNoDept, Status.ASSIGNED);
            Page<Task> page = new PageImpl<>(List.of(t1), pageable, 1);
            when(taskRepository.findByAssignedBy(managerNoDept, pageable)).thenReturn(page);

            PageResponse<TaskResponse> result = taskService.getAllTasks(pageable);

            assertThat(result.getContent()).hasSize(1);
            verify(taskRepository, never()).findVisibleToManager(any(), any(), any());
        }

        @Test
        void employeeSeesOnlyTasksAssignedToThem() {
            authenticateAs(employeeSameDept);
            Task t1 = taskWith(100L, employeeSameDept, manager, Status.ASSIGNED);
            Page<Task> page = new PageImpl<>(List.of(t1), pageable, 1);
            when(taskRepository.findByAssignedTo(employeeSameDept, pageable)).thenReturn(page);

            PageResponse<TaskResponse> result = taskService.getAllTasks(pageable);

            assertThat(result.getContent()).hasSize(1);
        }
    }

    // ---------------------------------------------------------------
    // updateTask
    // ---------------------------------------------------------------
    @Nested
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
        void assignerCanUpdateTheirOwnTask() {
            authenticateAs(manager);
            Task task = taskWith(100L, employeeSameDept, manager, Status.ASSIGNED);
            when(taskRepository.findById(100L)).thenReturn(Optional.of(task));
            when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

            TaskResponse response = taskService.updateTask(100L, validRequest());

            assertThat(response.getTitle()).isEqualTo("Updated title");
        }

        @Test
        void adminCanUpdateAnyTask() {
            authenticateAs(admin);
            Task task = taskWith(100L, employeeSameDept, manager, Status.ASSIGNED);
            when(taskRepository.findById(100L)).thenReturn(Optional.of(task));
            when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

            TaskResponse response = taskService.updateTask(100L, validRequest());

            assertThat(response.getTitle()).isEqualTo("Updated title");
        }

        @Test
        void nonAssignerNonAdminIsRejected() {
            authenticateAs(employeeSameDept);
            Task task = taskWith(100L, employeeSameDept, manager, Status.ASSIGNED);
            when(taskRepository.findById(100L)).thenReturn(Optional.of(task));

            assertThatThrownBy(() -> taskService.updateTask(100L, validRequest()))
                    .isInstanceOf(UnauthorizedAccessException.class);

            verify(taskRepository, never()).save(any());
        }

        @Test
        void rejectsEndDateBeforeStartDate() {
            authenticateAs(manager);
            Task task = taskWith(100L, employeeSameDept, manager, Status.ASSIGNED);
            when(taskRepository.findById(100L)).thenReturn(Optional.of(task));

            UpdateTaskRequest request = validRequest();
            request.setStartDate(LocalDate.now());
            request.setEndDate(LocalDate.now().minusDays(1));

            assertThatThrownBy(() -> taskService.updateTask(100L, request))
                    .isInstanceOf(InvalidTaskStateException.class);
        }
    }

    // ---------------------------------------------------------------
    // startTask (ASSIGNED -> STARTED, assignee only)
    // ---------------------------------------------------------------
    @Nested
    class StartTask {

        @Test
        void assigneeCanStartAnAssignedTask() {
            authenticateAs(employeeSameDept);
            Task task = taskWith(100L, employeeSameDept, manager, Status.ASSIGNED);
            when(taskRepository.findById(100L)).thenReturn(Optional.of(task));
            when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

            TaskResponse response = taskService.startTask(100L);

            assertThat(response.getStatus()).isEqualTo(Status.STARTED);
        }

        @Test
        void nonAssigneeCannotStartTheTask() {
            authenticateAs(manager); // manager is the assigner, not the assignee
            Task task = taskWith(100L, employeeSameDept, manager, Status.ASSIGNED);
            when(taskRepository.findById(100L)).thenReturn(Optional.of(task));

            assertThatThrownBy(() -> taskService.startTask(100L))
                    .isInstanceOf(UnauthorizedAccessException.class);
        }

        @Test
        void rejectsStart_whenTaskIsNotInAssignedState() {
            authenticateAs(employeeSameDept);
            Task task = taskWith(100L, employeeSameDept, manager, Status.STARTED);
            when(taskRepository.findById(100L)).thenReturn(Optional.of(task));

            assertThatThrownBy(() -> taskService.startTask(100L))
                    .isInstanceOf(InvalidTaskStateException.class);

            verify(taskRepository, never()).save(any());
        }
    }

    // ---------------------------------------------------------------
    // submitForReview (STARTED -> UNDER_REVIEW, assignee only)
    // ---------------------------------------------------------------
    @Nested
    class SubmitForReview {

        @Test
        void assigneeCanSubmitAStartedTask() {
            authenticateAs(employeeSameDept);
            Task task = taskWith(100L, employeeSameDept, manager, Status.STARTED);
            when(taskRepository.findById(100L)).thenReturn(Optional.of(task));
            when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

            TaskResponse response = taskService.submitForReview(100L);

            assertThat(response.getStatus()).isEqualTo(Status.UNDER_REVIEW);
        }

        @Test
        void rejectsSubmit_whenTaskIsNotStarted() {
            authenticateAs(employeeSameDept);
            Task task = taskWith(100L, employeeSameDept, manager, Status.ASSIGNED);
            when(taskRepository.findById(100L)).thenReturn(Optional.of(task));

            assertThatThrownBy(() -> taskService.submitForReview(100L))
                    .isInstanceOf(InvalidTaskStateException.class);
        }
    }

    // ---------------------------------------------------------------
    // approveTask (UNDER_REVIEW -> DONE, assigner/admin only)
    // ---------------------------------------------------------------
    @Nested
    class ApproveTask {

        @Test
        void assignerCanApprove() {
            authenticateAs(manager);
            Task task = taskWith(100L, employeeSameDept, manager, Status.UNDER_REVIEW);
            when(taskRepository.findById(100L)).thenReturn(Optional.of(task));
            when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

            TaskResponse response = taskService.approveTask(100L);

            assertThat(response.getStatus()).isEqualTo(Status.DONE);
        }

        @Test
        void adminCanApprove_evenIfNotTheAssigner() {
            authenticateAs(admin);
            Task task = taskWith(100L, employeeSameDept, manager, Status.UNDER_REVIEW);
            when(taskRepository.findById(100L)).thenReturn(Optional.of(task));
            when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

            TaskResponse response = taskService.approveTask(100L);

            assertThat(response.getStatus()).isEqualTo(Status.DONE);
        }

        @Test
        void assigneeCannotApproveTheirOwnTask() {
            authenticateAs(employeeSameDept);
            Task task = taskWith(100L, employeeSameDept, manager, Status.UNDER_REVIEW);
            when(taskRepository.findById(100L)).thenReturn(Optional.of(task));

            assertThatThrownBy(() -> taskService.approveTask(100L))
                    .isInstanceOf(UnauthorizedAccessException.class);
        }

        @Test
        void rejectsApprove_whenTaskIsNotUnderReview() {
            authenticateAs(manager);
            Task task = taskWith(100L, employeeSameDept, manager, Status.STARTED);
            when(taskRepository.findById(100L)).thenReturn(Optional.of(task));

            assertThatThrownBy(() -> taskService.approveTask(100L))
                    .isInstanceOf(InvalidTaskStateException.class);
        }
    }

    // ---------------------------------------------------------------
    // rejectTask (UNDER_REVIEW -> ASSIGNED, assigner/admin only)
    // ---------------------------------------------------------------
    @Nested
    class RejectTask {

        @Test
        void assignerCanRejectBackToAssigned() {
            authenticateAs(manager);
            Task task = taskWith(100L, employeeSameDept, manager, Status.UNDER_REVIEW);
            when(taskRepository.findById(100L)).thenReturn(Optional.of(task));
            when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

            TaskResponse response = taskService.rejectTask(100L);

            assertThat(response.getStatus()).isEqualTo(Status.ASSIGNED);
        }

        @Test
        void nonAssignerNonAdminCannotReject() {
            authenticateAs(employeeSameDept);
            Task task = taskWith(100L, employeeSameDept, manager, Status.UNDER_REVIEW);
            when(taskRepository.findById(100L)).thenReturn(Optional.of(task));

            assertThatThrownBy(() -> taskService.rejectTask(100L))
                    .isInstanceOf(UnauthorizedAccessException.class);
        }
    }

    // ---------------------------------------------------------------
    // deleteTask
    // ---------------------------------------------------------------
    @Nested
    class DeleteTask {

        @Test
        void assignerCanDeleteTheirOwnTask() {
            authenticateAs(manager);
            Task task = taskWith(100L, employeeSameDept, manager, Status.ASSIGNED);
            when(taskRepository.findById(100L)).thenReturn(Optional.of(task));

            taskService.deleteTask(100L);

            verify(taskRepository).delete(task);
            verify(auditLogService).record(eq(manager), anyString(), eq(100L), eq(AuditTargetType.TASK), anyString());
        }

        @Test
        void adminCanDeleteAnyTask() {
            authenticateAs(admin);
            Task task = taskWith(100L, employeeSameDept, manager, Status.ASSIGNED);
            when(taskRepository.findById(100L)).thenReturn(Optional.of(task));

            taskService.deleteTask(100L);

            verify(taskRepository).delete(task);
        }

        @Test
        void nonAssignerNonAdminCannotDelete() {
            authenticateAs(employeeSameDept);
            Task task = taskWith(100L, employeeSameDept, manager, Status.ASSIGNED);
            when(taskRepository.findById(100L)).thenReturn(Optional.of(task));

            assertThatThrownBy(() -> taskService.deleteTask(100L))
                    .isInstanceOf(UnauthorizedAccessException.class);

            verify(taskRepository, never()).delete(any());
        }
    }
}