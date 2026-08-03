package com.example.EmployeeManagementSystem.service.impl;

import com.example.EmployeeManagementSystem.dto.request.*;
import com.example.EmployeeManagementSystem.dto.response.PageResponse;
import com.example.EmployeeManagementSystem.dto.response.UserResponse;
import com.example.EmployeeManagementSystem.exception.*;
import com.example.EmployeeManagementSystem.model.*;
import com.example.EmployeeManagementSystem.repository.DepartmentRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


 // Pure Mockito unit tests for UserServiceImpl - no Spring context, no database.

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private DepartmentRepository departmentRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private TaskRepository taskRepository;
    @Mock private AuditLogService auditLogService;

    private UserServiceImpl userService;

    private User admin;
    private User manager;       // heads `engineering`
    private User employeeSameDept;   // in `engineering`
    private User employeeOtherDept;  // in `sales`
    private Department engineering;
    private Department sales;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userRepository, departmentRepository,
                passwordEncoder, taskRepository, auditLogService);

        engineering = new Department();
        engineering.setId(10L);
        engineering.setName("Engineering");

        sales = new Department();
        sales.setId(20L);
        sales.setName("Sales");

        admin = new User();
        admin.setId(1L);
        admin.setUsername("admin");
        admin.setEmail("admin@example.com");
        admin.setRole(Role.ADMIN);

        manager = new User();
        manager.setId(2L);
        manager.setUsername("msmith");
        manager.setEmail("msmith@example.com");
        manager.setRole(Role.MANAGER);
        manager.setDepartment(engineering);
        engineering.setManager(manager);

        employeeSameDept = new User();
        employeeSameDept.setId(3L);
        employeeSameDept.setUsername("jdoe");
        employeeSameDept.setEmail("jdoe@example.com");
        employeeSameDept.setRole(Role.EMPLOYEE);
        employeeSameDept.setDepartment(engineering);

        employeeOtherDept = new User();
        employeeOtherDept.setId(4L);
        employeeOtherDept.setUsername("bwayne");
        employeeOtherDept.setEmail("bwayne@example.com");
        employeeOtherDept.setRole(Role.EMPLOYEE);
        employeeOtherDept.setDepartment(sales);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /** Marks `user` as the currently authenticated caller for getCurrentUser(). */
    private void authenticateAs(User user) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user.getUsername(), null));
        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));
    }

    // ---------------------------------------------------------------
    // createUser
    // ---------------------------------------------------------------
    @Nested
    class CreateUser {


        @Test
        void succeeds_whenCalledByAdmin() throws IllegalAccessException {
            authenticateAs(admin);
            CreateUserRequest request = new CreateUserRequest(
                    "newuser", "rawpassword", "new@example.com", "New", "User", Role.EMPLOYEE, null);

            when(userRepository.existsByUsername("newuser")).thenReturn(false);
            when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
            when(passwordEncoder.encode("rawpassword")).thenReturn("hashed");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(99L);
                return u;
            });

            UserResponse response = userService.createUser(request);

            assertThat(response.getUsername()).isEqualTo("newuser");
            assertThat(response.getRole()).isEqualTo(Role.EMPLOYEE);
            verify(userRepository).save(argThat(u -> u.getPassword().equals("hashed")));
            verify(auditLogService).record(eq(admin), anyString(), eq(99L), eq(AuditTargetType.USER), anyString());
        }

        @Test
        void throwsDuplicated_whenUsernameAlreadyExists() throws IllegalAccessException {
            authenticateAs(admin);
            CreateUserRequest request = new CreateUserRequest(
                    "existing", "pw", "x@example.com", "X", "Y", Role.EMPLOYEE, null);
            when(userRepository.existsByUsername("existing")).thenReturn(true);

            assertThatThrownBy(() -> userService.createUser(request))
                    .isInstanceOf(DuplicatedException.class)
                    .hasMessageContaining("Username");

            verify(userRepository, never()).save(any());
        }

        @Test
        void throwsDuplicated_whenEmailAlreadyExists() throws IllegalAccessException {
            authenticateAs(admin);
            CreateUserRequest request = new CreateUserRequest(
                    "brandnew", "pw", "taken@example.com", "X", "Y", Role.EMPLOYEE, null);
            when(userRepository.existsByUsername("brandnew")).thenReturn(false);
            when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

            assertThatThrownBy(() -> userService.createUser(request))
                    .isInstanceOf(DuplicatedException.class)
                    .hasMessageContaining("Email");

            verify(userRepository, never()).save(any());
        }
    }

    // ---------------------------------------------------------------
    // getUserById - fully testable, manual role/ownership check
    // ---------------------------------------------------------------
    @Nested
    class GetUserById {

        @Test
        void adminCanViewAnyUser() {
            authenticateAs(admin);
            when(userRepository.findById(4L)).thenReturn(Optional.of(employeeOtherDept));

            UserResponse response = userService.getUserById(4L);

            assertThat(response.getUsername()).isEqualTo("bwayne");
        }

        @Test
        void managerCanViewUserInOwnDepartment() {
            authenticateAs(manager);
            when(userRepository.findById(3L)).thenReturn(Optional.of(employeeSameDept));

            UserResponse response = userService.getUserById(3L);

            assertThat(response.getUsername()).isEqualTo("jdoe");
        }

        @Test
        void managerCannotViewUserOutsideOwnDepartment() {
            authenticateAs(manager);
            when(userRepository.findById(4L)).thenReturn(Optional.of(employeeOtherDept));

            assertThatThrownBy(() -> userService.getUserById(4L))
                    .isInstanceOf(UnauthorizedAccessException.class);
        }

        @Test
        void employeeCanViewSelf() {
            authenticateAs(employeeSameDept);
            when(userRepository.findById(3L)).thenReturn(Optional.of(employeeSameDept));

            UserResponse response = userService.getUserById(3L);

            assertThat(response.getUsername()).isEqualTo("jdoe");
        }

        @Test
        void employeeCannotViewSomeoneElse() {
            authenticateAs(employeeSameDept);
            when(userRepository.findById(4L)).thenReturn(Optional.of(employeeOtherDept));

            assertThatThrownBy(() -> userService.getUserById(4L))
                    .isInstanceOf(UnauthorizedAccessException.class);
        }

        @Test
        void throwsNotFound_whenUserDoesNotExist() {
            authenticateAs(admin);
            when(userRepository.findById(404L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUserById(404L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ---------------------------------------------------------------
    // getUserByUsername - same rules as getUserById, spot-checked
    // ---------------------------------------------------------------
    @Nested
    class GetUserByUsername {

        @Test
        void adminCanViewAnyUser() {
            when(userRepository.findByUsername("bwayne")).thenReturn(Optional.of(employeeOtherDept));
            authenticateAs(admin);

            UserResponse response = userService.getUserByUsername("bwayne");

            assertThat(response.getUsername()).isEqualTo("bwayne");
        }

        @Test
        void managerCannotViewUserOutsideOwnDepartment() {
            when(userRepository.findByUsername("bwayne")).thenReturn(Optional.of(employeeOtherDept));
            authenticateAs(manager);

            assertThatThrownBy(() -> userService.getUserByUsername("bwayne"))
                    .isInstanceOf(UnauthorizedAccessException.class);
        }

        @Test
        void throwsNotFound_whenUsernameDoesNotExist() {
            when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUserByUsername("ghost"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ---------------------------------------------------------------
    // getAllUsers(Pageable) - returns PageResponse<UserResponse>.
    // ---------------------------------------------------------------
    @Nested
    class GetAllUsers {

        private final Pageable pageable = PageRequest.of(0, 20);

        @Test
        void adminSeesEveryone() {
            authenticateAs(admin);
            Page<User> page = new PageImpl<>(
                    List.of(admin, manager, employeeSameDept, employeeOtherDept), pageable, 4);
            when(userRepository.findAll(pageable)).thenReturn(page);

            PageResponse<UserResponse> result = userService.getAllUsers(pageable);

            assertThat(result.getContent()).hasSize(4);
        }

        @Test
        void managerSeesOnlyOwnDepartment() {
            authenticateAs(manager);
            Page<User> page = new PageImpl<>(List.of(manager, employeeSameDept), pageable, 2);
            when(userRepository.findByDepartment(engineering, pageable)).thenReturn(page);

            PageResponse<UserResponse> result = userService.getAllUsers(pageable);

            assertThat(result.getContent())
                    .extracting(UserResponse::getUsername)
                    .containsExactlyInAnyOrder("msmith", "jdoe");
        }

        @Test
        void managerWithNoDepartmentSeesEmptyPage() {
            User managerNoDept = new User();
            managerNoDept.setId(5L);
            managerNoDept.setUsername("floater");
            managerNoDept.setRole(Role.MANAGER);
            managerNoDept.setDepartment(null);
            authenticateAs(managerNoDept);

            PageResponse<UserResponse> result = userService.getAllUsers(pageable);

            assertThat(result.getContent()).isEmpty();
            verify(userRepository, never()).findByDepartment(any(), any());
        }

        @Test
        void employeeSeesOnlySelf() {
            authenticateAs(employeeSameDept);

            PageResponse<UserResponse> result = userService.getAllUsers(pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getUsername()).isEqualTo("jdoe");
        }
    }

    // ---------------------------------------------------------------
    // updateUser - @PreAuthorize ADMIN/MANAGER + manual dept check
    // ---------------------------------------------------------------
    @Nested
    class UpdateUser {

        private UpdateUserRequest request() {
            return new UpdateUserRequest("Jane", "Doe", "jane@example.com");
        }

        @Test
        void adminCanUpdateAnyUser() {
            authenticateAs(admin);
            when(userRepository.findById(4L)).thenReturn(Optional.of(employeeOtherDept));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            UserResponse response = userService.updateUser(4L, request());

            assertThat(response.getFirstName()).isEqualTo("Jane");
            verify(auditLogService).record(eq(admin), anyString(), eq(4L), eq(AuditTargetType.USER), anyString());
        }

        @Test
        void managerCanUpdateEmployeeInOwnDepartment() {
            authenticateAs(manager);
            when(userRepository.findById(3L)).thenReturn(Optional.of(employeeSameDept));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            UserResponse response = userService.updateUser(3L, request());

            assertThat(response.getEmail()).isEqualTo("jane@example.com");
        }

        @Test
        void managerCannotUpdateEmployeeOutsideOwnDepartment() {
            authenticateAs(manager);
            when(userRepository.findById(4L)).thenReturn(Optional.of(employeeOtherDept));

            assertThatThrownBy(() -> userService.updateUser(4L, request()))
                    .isInstanceOf(UnauthorizedAccessException.class);

            verify(userRepository, never()).save(any());
        }

        @Test
        void rejectsUpdate_whenNewEmailAlreadyTakenByAnotherUser() {
            authenticateAs(admin);
            when(userRepository.findById(4L)).thenReturn(Optional.of(employeeOtherDept));
            when(userRepository.existsByEmail("jane@example.com")).thenReturn(true);

            assertThatThrownBy(() -> userService.updateUser(4L, request()))
                    .isInstanceOf(DuplicatedException.class)
                    .hasMessageContaining("Email");

            verify(userRepository, never()).save(any());
        }

        @Test
        void allowsUpdate_whenEmailUnchanged() {
            authenticateAs(admin);
            // request() email matches what's already on the user - the
            // existsByEmail check should be skipped entirely since the
            // code only checks it when the email is actually changing
            employeeOtherDept.setEmail("jane@example.com");
            when(userRepository.findById(4L)).thenReturn(Optional.of(employeeOtherDept));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            userService.updateUser(4L, request());

            verify(userRepository, never()).existsByEmail(anyString());
        }

        // Note: an EMPLOYEE calling this at all is blocked by @PreAuthorize
        // before the method body runs - not reproducible in a unit test.
    }

    // ---------------------------------------------------------------
    // deleteUser - cascade-deletes the user's tasks (both assigned-to
    // and assigned-by), and blocks entirely if the user currently heads
    // a department (admin must reassign/remove them as manager first).
    // ---------------------------------------------------------------
    @Nested
    class DeleteUser {

        @Test
        void deletesUserAndCascadesTheirTasks() {
            authenticateAs(admin);
            when(userRepository.findById(3L)).thenReturn(Optional.of(employeeSameDept));

            Task assignedToThem = new Task();
            assignedToThem.setId(100L);
            Task assignedByThem = new Task();
            assignedByThem.setId(101L);
            List<Task> assignedToList = List.of(assignedToThem);
            List<Task> assignedByList = List.of(assignedByThem);

            when(taskRepository.findByAssignedTo(employeeSameDept)).thenReturn(assignedToList);
            when(taskRepository.findByAssignedBy(employeeSameDept)).thenReturn(assignedByList);

            userService.deleteUser(3L);

            verify(taskRepository).deleteAll(assignedToList);
            verify(taskRepository).deleteAll(assignedByList);
            verify(userRepository).delete(employeeSameDept);
            // audit message should mention how many tasks went with them (2 here)
            verify(auditLogService).record(eq(admin), anyString(), eq(3L), eq(AuditTargetType.USER),
                    argThat(details -> details.contains("2")));
        }

        @Test
        void blocksDeletion_whenUserCurrentlyHeadsADepartment() {
            // `manager` is set up in @BeforeEach as engineering's head
            authenticateAs(admin);
            when(userRepository.findById(2L)).thenReturn(Optional.of(manager));

            assertThatThrownBy(() -> userService.deleteUser(2L))
                    .isInstanceOf(InvalidTaskStateException.class)
                    .hasMessageContaining("heading department");

            verify(userRepository, never()).delete(any());
            verify(taskRepository, never()).deleteAll(any());
        }

        @Test
        void throwsNotFound_whenUserDoesNotExist() {
            authenticateAs(admin);
            when(userRepository.findById(404L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.deleteUser(404L))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(userRepository, never()).delete(any());
        }
    }

    // ---------------------------------------------------------------
    // assignDepartment - only guarded by @PreAuthorize
    // ---------------------------------------------------------------
    @Nested
    class AssignDepartment {

        @Test
        void succeeds_whenCalledByAdmin() {
            authenticateAs(admin);
            when(userRepository.findById(4L)).thenReturn(Optional.of(employeeOtherDept));
            when(departmentRepository.findById(10L)).thenReturn(Optional.of(engineering));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            AssignDepartmentRequest request = new AssignDepartmentRequest();
            request.setDepartmentId(10L);

            UserResponse response = userService.assignDepartment(4L, request);

            assertThat(response.getDepartmentName()).isEqualTo("Engineering");
        }

        @Test
        void throwsNotFound_whenDepartmentDoesNotExist() {
            authenticateAs(admin);
            when(userRepository.findById(4L)).thenReturn(Optional.of(employeeOtherDept));
            when(departmentRepository.findById(999L)).thenReturn(Optional.empty());

            AssignDepartmentRequest request = new AssignDepartmentRequest();
            request.setDepartmentId(999L);

            assertThatThrownBy(() -> userService.assignDepartment(4L, request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ---------------------------------------------------------------
    // updatePassword - fully testable, manual self-or-admin check
    // ---------------------------------------------------------------
    @Nested
    class UpdatePassword {

        @Test
        void selfCanChangeOwnPassword_whenOldPasswordMatches() {
            authenticateAs(employeeSameDept);
            employeeSameDept.setPassword("hashed-old");
            when(userRepository.findById(3L)).thenReturn(Optional.of(employeeSameDept));
            when(passwordEncoder.matches("oldpw", "hashed-old")).thenReturn(true);
            when(passwordEncoder.encode("newpw")).thenReturn("hashed-new");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            userService.updatePassword(3L, new PasswordChangeRequest("oldpw", "newpw"));

            verify(userRepository).save(argThat(u -> u.getPassword().equals("hashed-new")));
            // self-service changes are not audit logged, per current design
            verify(auditLogService, never()).record(any(), anyString(), any(), any(), anyString());
        }

        @Test
        void selfChangeFails_whenOldPasswordWrong() {
            authenticateAs(employeeSameDept);
            employeeSameDept.setPassword("hashed-old");
            when(userRepository.findById(3L)).thenReturn(Optional.of(employeeSameDept));
            when(passwordEncoder.matches("wrongpw", "hashed-old")).thenReturn(false);

            assertThatThrownBy(() -> userService.updatePassword(3L, new PasswordChangeRequest("wrongpw", "newpw")))
                    .isInstanceOf(UnMatchedPasswordsException.class);

            verify(userRepository, never()).save(any());
        }

        @Test
        void adminCanResetSomeoneElsesPassword_withoutOldPasswordCheck() {
            authenticateAs(admin);
            employeeSameDept.setPassword("hashed-old");
            when(userRepository.findById(3L)).thenReturn(Optional.of(employeeSameDept));
            when(passwordEncoder.encode("newpw")).thenReturn("hashed-new");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            userService.updatePassword(3L, new PasswordChangeRequest("irrelevant", "newpw"));

            verify(passwordEncoder, never()).matches(anyString(), anyString());
            verify(auditLogService).record(eq(admin), anyString(), eq(3L), eq(AuditTargetType.USER), anyString());
        }

        @Test
        void nonSelfNonAdminIsRejected() {
            authenticateAs(employeeOtherDept);
            when(userRepository.findById(3L)).thenReturn(Optional.of(employeeSameDept));

            assertThatThrownBy(() -> userService.updatePassword(3L, new PasswordChangeRequest("x", "y")))
                    .isInstanceOf(UnauthorizedAccessException.class);

            verify(userRepository, never()).save(any());
        }
    }

    // ---------------------------------------------------------------
    // changeRole - already covered in detail in an earlier pass;
    // ---------------------------------------------------------------
    @Nested
    class ChangeRole {

        @Test
        void adminCanChangeRole_happyPathDemotion() {
            authenticateAs(admin);
            manager.setDepartment(engineering);
            when(userRepository.findById(2L)).thenReturn(Optional.of(manager));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            UserResponse response = userService.changeRole(2L, new ChangeRoleRequest(Role.EMPLOYEE));

            assertThat(response.getRole()).isEqualTo(Role.EMPLOYEE);
            assertThat(engineering.getManager()).isNull();
        }
    }
}