package com.example.EmployeeManagementSystem.service.impl;

import com.example.EmployeeManagementSystem.dto.request.AssignManagerRequest;
import com.example.EmployeeManagementSystem.dto.request.CreateDepartmentRequest;
import com.example.EmployeeManagementSystem.dto.request.UpdateDepartmentRequest;
import com.example.EmployeeManagementSystem.dto.response.DepartmentResponse;
import com.example.EmployeeManagementSystem.dto.response.PageResponse;
import com.example.EmployeeManagementSystem.exception.DuplicatedException;
import com.example.EmployeeManagementSystem.exception.InvalidTaskStateException;
import com.example.EmployeeManagementSystem.exception.ResourceNotFoundException;
import com.example.EmployeeManagementSystem.model.AuditTargetType;
import com.example.EmployeeManagementSystem.model.Department;
import com.example.EmployeeManagementSystem.model.Role;
import com.example.EmployeeManagementSystem.model.User;
import com.example.EmployeeManagementSystem.repository.DepartmentRepository;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceImplTest {

    @Mock private DepartmentRepository departmentRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuditLogService auditLogService;

    private DepartmentServiceImpl departmentService;

    private User admin;
    private Department engineering;
    private final Pageable pageable = PageRequest.of(0, 20);

    @BeforeEach
    void setUp() {
        departmentService = new DepartmentServiceImpl(departmentRepository, userRepository, auditLogService);

        admin = new User();
        admin.setId(1L);
        admin.setUsername("admin");
        admin.setRole(Role.ADMIN);

        engineering = new Department();
        engineering.setId(10L);
        engineering.setName("Engineering");
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

    // ---------------------------------------------------------------
    // createDepartment
    // ---------------------------------------------------------------
    @Nested
    class CreateDepartment {

        @Test
        void succeeds_whenNameIsUnique() {
            authenticateAs(admin);
            CreateDepartmentRequest request = new CreateDepartmentRequest();
            request.setName("Marketing");

            when(departmentRepository.existsByName("Marketing")).thenReturn(false);
            when(departmentRepository.save(any(Department.class))).thenAnswer(inv -> {
                Department d = inv.getArgument(0);
                d.setId(20L);
                return d;
            });

            DepartmentResponse response = departmentService.createDepartment(request);

            assertThat(response.getName()).isEqualTo("Marketing");
            verify(auditLogService).record(eq(admin), anyString(), eq(20L), eq(AuditTargetType.DEPARTMENT), anyString());
        }

        @Test
        void throwsDuplicated_whenNameAlreadyExists() {
            authenticateAs(admin);
            CreateDepartmentRequest request = new CreateDepartmentRequest();
            request.setName("Engineering");
            when(departmentRepository.existsByName("Engineering")).thenReturn(true);

            assertThatThrownBy(() -> departmentService.createDepartment(request))
                    .isInstanceOf(DuplicatedException.class);

            verify(departmentRepository, never()).save(any());
        }
    }

    // ---------------------------------------------------------------
    // getDepartmentById
    // ---------------------------------------------------------------
    @Nested
    class GetDepartmentById {

        @Test
        void returnsDepartment_whenFound() {
            when(departmentRepository.findById(10L)).thenReturn(Optional.of(engineering));

            DepartmentResponse response = departmentService.getDepartmentById(10L);

            assertThat(response.getName()).isEqualTo("Engineering");
        }

        @Test
        void throwsNotFound_whenMissing() {
            when(departmentRepository.findById(404L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> departmentService.getDepartmentById(404L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ---------------------------------------------------------------
    // getAllDepartments(Pageable)
    // ---------------------------------------------------------------
    @Nested
    class GetAllDepartments {

        @Test
        void returnsAllDepartmentsPaginated() {
            Department sales = new Department();
            sales.setId(11L);
            sales.setName("Sales");
            Page<Department> page = new PageImpl<>(List.of(engineering, sales), pageable, 2);
            when(departmentRepository.findAll(pageable)).thenReturn(page);

            PageResponse<DepartmentResponse> result = departmentService.getAllDepartments(pageable);

            assertThat(result.getContent())
                    .extracting(DepartmentResponse::getName)
                    .containsExactlyInAnyOrder("Engineering", "Sales");
        }
    }

    // ---------------------------------------------------------------
    // updateDepartment
    // ---------------------------------------------------------------
    @Nested
    class UpdateDepartment {

        @Test
        void renamesSuccessfully_whenNewNameIsUnique() {
            authenticateAs(admin);
            when(departmentRepository.findById(10L)).thenReturn(Optional.of(engineering));
            when(departmentRepository.existsByName("Platform Engineering")).thenReturn(false);
            when(departmentRepository.save(any(Department.class))).thenAnswer(inv -> inv.getArgument(0));

            UpdateDepartmentRequest request = new UpdateDepartmentRequest();
            request.setName("Platform Engineering");

            DepartmentResponse response = departmentService.updateDepartment(10L, request);

            assertThat(response.getName()).isEqualTo("Platform Engineering");
        }

        @Test
        void allowsUpdate_whenNameUnchanged() {
            authenticateAs(admin);
            when(departmentRepository.findById(10L)).thenReturn(Optional.of(engineering));
            when(departmentRepository.save(any(Department.class))).thenAnswer(inv -> inv.getArgument(0));

            UpdateDepartmentRequest request = new UpdateDepartmentRequest();
            request.setName("Engineering"); // same as current

            departmentService.updateDepartment(10L, request);

            // since the name isn't actually changing, the uniqueness check should be skipped
            verify(departmentRepository, never()).existsByName(anyString());
        }

        @Test
        void throwsDuplicated_whenRenamingToAnExistingName() {
            authenticateAs(admin);
            when(departmentRepository.findById(10L)).thenReturn(Optional.of(engineering));
            when(departmentRepository.existsByName("Sales")).thenReturn(true);

            UpdateDepartmentRequest request = new UpdateDepartmentRequest();
            request.setName("Sales");

            assertThatThrownBy(() -> departmentService.updateDepartment(10L, request))
                    .isInstanceOf(DuplicatedException.class);

            verify(departmentRepository, never()).save(any());
        }
    }

    // ---------------------------------------------------------------
    // deleteDepartment
    // ---------------------------------------------------------------
    @Nested
    class DeleteDepartment {

        @Test
        void deletesSuccessfully_whenNoEmployeesAssigned() {
            authenticateAs(admin);
            when(departmentRepository.findById(10L)).thenReturn(Optional.of(engineering));
            when(userRepository.findByDepartment(engineering)).thenReturn(List.of());

            departmentService.deleteDepartment(10L);

            verify(departmentRepository).delete(engineering);
            verify(auditLogService).record(eq(admin), anyString(), eq(10L), eq(AuditTargetType.DEPARTMENT), anyString());
        }

        @Test
        void blocksDeletion_whenEmployeesStillAssigned() {
            authenticateAs(admin);
            when(departmentRepository.findById(10L)).thenReturn(Optional.of(engineering));
            User someEmployee = new User();
            someEmployee.setId(3L);
            when(userRepository.findByDepartment(engineering)).thenReturn(List.of(someEmployee));

            assertThatThrownBy(() -> departmentService.deleteDepartment(10L))
                    .isInstanceOf(InvalidTaskStateException.class)
                    .hasMessageContaining("employee");

            verify(departmentRepository, never()).delete(any());
        }
    }

    // ---------------------------------------------------------------
    // assignManager
    // ---------------------------------------------------------------
    @Nested
    class AssignManager {

        @Test
        void succeeds_whenDepartmentHasNoManagerAndUserIsAManager() {
            authenticateAs(admin);
            User candidateManager = new User();
            candidateManager.setId(5L);
            candidateManager.setUsername("newmgr");
            candidateManager.setRole(Role.MANAGER);

            when(departmentRepository.findById(10L)).thenReturn(Optional.of(engineering));
            when(userRepository.findById(5L)).thenReturn(Optional.of(candidateManager));
            when(departmentRepository.save(any(Department.class))).thenAnswer(inv -> inv.getArgument(0));

            AssignManagerRequest request = new AssignManagerRequest();
            request.setUserId(5L);

            DepartmentResponse response = departmentService.assignManager(10L, request);

            assertThat(response.getManagerUsername()).isEqualTo("newmgr");
        }

        @Test
        void throwsInvalidState_whenDepartmentAlreadyHasAManager() {
            authenticateAs(admin);
            User existingManager = new User();
            existingManager.setId(2L);
            engineering.setManager(existingManager);
            when(departmentRepository.findById(10L)).thenReturn(Optional.of(engineering));

            AssignManagerRequest request = new AssignManagerRequest();
            request.setUserId(5L);

            assertThatThrownBy(() -> departmentService.assignManager(10L, request))
                    .isInstanceOf(InvalidTaskStateException.class)
                    .hasMessageContaining("already has a manager");
        }

        @Test
        void throwsInvalidState_whenCandidateDoesNotHoldManagerRole() {
            authenticateAs(admin);
            User candidateEmployee = new User();
            candidateEmployee.setId(5L);
            candidateEmployee.setRole(Role.EMPLOYEE);

            when(departmentRepository.findById(10L)).thenReturn(Optional.of(engineering));
            when(userRepository.findById(5L)).thenReturn(Optional.of(candidateEmployee));

            AssignManagerRequest request = new AssignManagerRequest();
            request.setUserId(5L);

            assertThatThrownBy(() -> departmentService.assignManager(10L, request))
                    .isInstanceOf(InvalidTaskStateException.class)
                    .hasMessageContaining("MANAGER role");
        }

        @Test
        void throwsNotFound_whenCandidateUserDoesNotExist() {
            authenticateAs(admin);
            when(departmentRepository.findById(10L)).thenReturn(Optional.of(engineering));
            when(userRepository.findById(404L)).thenReturn(Optional.empty());

            AssignManagerRequest request = new AssignManagerRequest();
            request.setUserId(404L);

            assertThatThrownBy(() -> departmentService.assignManager(10L, request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ---------------------------------------------------------------
    // removeManager
    // ---------------------------------------------------------------
    @Nested
    class RemoveManager {

        @Test
        void removesExistingManager() {
            authenticateAs(admin);
            User currentManager = new User();
            currentManager.setId(2L);
            currentManager.setUsername("msmith");
            engineering.setManager(currentManager);

            when(departmentRepository.findById(10L)).thenReturn(Optional.of(engineering));
            when(departmentRepository.save(any(Department.class))).thenAnswer(inv -> inv.getArgument(0));

            DepartmentResponse response = departmentService.removeManager(10L);

            assertThat(response.getManagerId()).isNull();
            verify(auditLogService).record(eq(admin), anyString(), eq(10L), eq(AuditTargetType.DEPARTMENT),
                    argThat(details -> details.contains("msmith")));
        }

        @Test
        void isANoOp_whenDepartmentAlreadyHasNoManager() {
            authenticateAs(admin);
            engineering.setManager(null);
            when(departmentRepository.findById(10L)).thenReturn(Optional.of(engineering));
            when(departmentRepository.save(any(Department.class))).thenAnswer(inv -> inv.getArgument(0));

            DepartmentResponse response = departmentService.removeManager(10L);

            assertThat(response.getManagerId()).isNull();
        }
    }
}