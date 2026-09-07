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
import com.example.EmployeeManagementSystem.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final TaskRepository taskRepository;
    private final AuditLogService auditLogService;


    private UserResponse toUserResponse(User user)
    {
        return UserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .username(user.getUsername())
                .role(user.getRole())
                .departmentName(user.getDepartment() != null
                        ? user.getDepartment().getName()
                        : null)
                .createdAt(user.getCreatedAt())
                .build();
    }
    private boolean isManagerOfUsersDepartment(User manager, User target) {
        Department dept = target.getDepartment();
        if (dept == null || dept.getManager() == null) {
            return false;
        }
        return dept.getManager().getId().equals(manager.getId());
    }

    private User getCurrentUser() {
        String username = Objects.requireNonNull(SecurityContextHolder.getContext()
                        .getAuthentication())
                .getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Authenticated user not found"));
    }



    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public UserResponse createUser(CreateUserRequest request) throws IllegalAccessException {
        User currentUser = getCurrentUser();
        if(userRepository.existsByUsername(request.getUsername()))
        {
           throw new DuplicatedException("Username already exists");
        }
        if(userRepository.existsByEmail(request.getEmail()))
        {
            throw new DuplicatedException("Email already exists");
        }

        User newUser = new User();
        newUser.setUsername(request.getUsername());
        newUser.setEmail(request.getEmail());
        newUser.setPassword(passwordEncoder.encode(request.getPassword()));
        newUser.setRole(request.getRole());
        newUser.setFirstName(request.getFirstName());
        newUser.setLastName(request.getLastName());

        if (request.getDepartmentId() != null) {
            Department department = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Department not found with id: " + request.getDepartmentId()));
            newUser.setDepartment(department);
        }

        User savedUser = userRepository.save(newUser);

        auditLogService.record(currentUser, "CREATE_USER", savedUser.getId(), AuditTargetType.USER,
                "Created user '" + savedUser.getUsername() + "' with role " + savedUser.getRole());

        return toUserResponse(savedUser);


    }

    @Override
    public UserResponse getUserById(Long id) {
        User currentUser = getCurrentUser();
        Role role = currentUser.getRole();
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        if (role == Role.ADMIN) {
            return toUserResponse(user);
        }

        if (role == Role.MANAGER) {
            if (isManagerOfUsersDepartment(currentUser, user)) {
                return toUserResponse(user);
            }
            throw new UnauthorizedAccessException("You don't have permission to perform this operation");
        }

        // EMPLOYEE (or any other role) can only view their own profile
        if (currentUser.getId().equals(user.getId())) {
            return toUserResponse(user);
        }
        throw new UnauthorizedAccessException("You don't have permission to perform this operation");
    }

    @Override
    public UserResponse getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
        User currentUser = getCurrentUser();
        Role role = currentUser.getRole();

        if (role == Role.ADMIN) {
            return toUserResponse(user);
        }

        if (role == Role.MANAGER) {
            if (isManagerOfUsersDepartment(currentUser, user)) {
                return toUserResponse(user);
            }
            throw new UnauthorizedAccessException("You don't have permission to perform this operation");
        }

        if (currentUser.getId().equals(user.getId())) {
            return toUserResponse(user);
        }
        throw new UnauthorizedAccessException("You don't have permission to perform this operation");

    }

    @Override
    public PageResponse<UserResponse> getAllUsers(Pageable pageable) {
        User currentUser = getCurrentUser();
        Role role = currentUser.getRole();

        if (role == Role.ADMIN) {
            Page<UserResponse> page = userRepository.findAll(pageable).map(this::toUserResponse);
            return PageResponse.from(page);
        }

        if (role == Role.MANAGER) {
            Department dept = currentUser.getDepartment();
            if (dept == null) {
                return PageResponse.from(Page.empty(pageable));
            }
            Page<UserResponse> page = userRepository.findByDepartment(dept, pageable).map(this::toUserResponse);
            return PageResponse.from(page);
        }

        // EMPLOYEE: only themselves - a manually built single-item "page"
        Page<UserResponse> page = new PageImpl<>(List.of(toUserResponse(currentUser)), pageable, 1);
        return PageResponse.from(page);
    }

    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Override
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        User currentUser = getCurrentUser();
        Role role = currentUser.getRole();
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        boolean allowed = role == Role.ADMIN
                || (role == Role.MANAGER && isManagerOfUsersDepartment(currentUser, user));

        if (!allowed) {
            throw new UnauthorizedAccessException("You don't have permission to perform this operation");
        }

        if (!user.getEmail().equals(request.getEmail())
                && userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicatedException("Email already exists");
        }

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        User updatedUser = userRepository.save(user);

        auditLogService.record(currentUser, "UPDATE_USER", updatedUser.getId(), AuditTargetType.USER,
                "Updated profile for user '" + updatedUser.getUsername() + "'");

        return toUserResponse(updatedUser);
    }


    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public void deleteUser(Long id) {
        User currentUser = getCurrentUser();
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        // block if the user role is manager and he is leading a department , Admin need to change his role first .
        if (user.getDepartment() != null && user.getDepartment().getManager() != null
                && user.getDepartment().getManager().getId().equals(user.getId())) {
            throw new InvalidTaskStateException(
                    "Cannot delete user: they are currently heading department '"
                            + user.getDepartment().getName() + "'. Remove them as manager first.");
        }

        //delete all tasks related to this user.
        List<Task> tasksAssignedToUser = taskRepository.findByAssignedTo(user);
        List<Task> tasksAssignedByUser = taskRepository.findByAssignedBy(user);
        int deletedTaskCount = tasksAssignedToUser.size() + tasksAssignedByUser.size();

        taskRepository.deleteAll(tasksAssignedToUser);
        taskRepository.deleteAll(tasksAssignedByUser);

        //delete the user
        userRepository.delete(user);

        //log it , and show a count of his total tasks that was deleted with him.
        auditLogService.record(currentUser, "DELETE_USER", id, AuditTargetType.USER,
                "Deleted user '" + user.getUsername() + "' along with " + deletedTaskCount + " associated task(s)");

    }

    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public UserResponse changeRole(Long id, ChangeRoleRequest request) {
        User currentUser = getCurrentUser();


        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        Role oldRole = user.getRole();
        Role newRole = request.getRole();

        // Promotion rule: EMPLOYEE -> MANAGER only if the employee has zero active tasks
        if (oldRole == Role.EMPLOYEE && newRole == Role.MANAGER) {
            boolean hasActiveTasks = taskRepository.existsByAssignedToAndStatusNot(user, Status.DONE);
            if (hasActiveTasks) {
                throw new InvalidTaskStateException(
                        "Cannot promote user: employee still has active (non-DONE) tasks");
            }

            Department department = user.getDepartment();
            if (department == null) {
                throw new InvalidTaskStateException("Cannot promote user: user has no department assigned");
            }
            // Single Manager Constraint: department must not already have a manager
            if (department.getManager() != null) {
                throw new InvalidTaskStateException(
                        "Cannot promote user: department '" + department.getName()
                                + "' already has a manager. Remove/replace the current manager first.");
            }

            department.setManager(user);
            departmentRepository.save(department);
        }

        // Demotion / reassignment away from MANAGER: release department headship
        if (oldRole == Role.MANAGER && newRole != Role.MANAGER) {
            Department department = user.getDepartment();
            if (department != null && department.getManager() != null
                    && department.getManager().getId().equals(user.getId())) {
                department.setManager(null);
                departmentRepository.save(department);
            }
        }

        user.setRole(newRole);
        User updatedUser = userRepository.save(user);

        auditLogService.record(currentUser, "CHANGE_ROLE", updatedUser.getId(), AuditTargetType.USER,
                "Changed role of user '" + updatedUser.getUsername() + "' from " + oldRole + " to " + newRole);

        return toUserResponse(updatedUser);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public UserResponse assignDepartment(Long id, AssignDepartmentRequest request) {

        User currentUser = getCurrentUser();
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Department not found with id: " + request.getDepartmentId()));

        user.setDepartment(department);
        User updatedUser = userRepository.save(user);

        auditLogService.record(currentUser, "ASSIGN_DEPARTMENT", updatedUser.getId(), AuditTargetType.USER,
                "Assigned user '" + updatedUser.getUsername() + "' to department '" + department.getName() + "'");

        return toUserResponse(updatedUser);
    }

    @Override
    public UserResponse updatePassword(Long id, PasswordChangeRequest request) {
        User currentUser = getCurrentUser();
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        boolean isSelf = currentUser.getId().equals(user.getId());
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;

        if (!isSelf && !isAdmin) {
            throw new UnauthorizedAccessException("You don't have permission to perform this operation");
        }

        // Self-service password changes must prove knowledge of the old password.
        // Admin-initiated resets on other users skip this check.
        if (isSelf && !passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new UnMatchedPasswordsException("Old passwords don't match");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        User updatedUser = userRepository.save(user);

        if (!isSelf) {
            auditLogService.record(currentUser, "RESET_PASSWORD", updatedUser.getId(), AuditTargetType.USER,
                    "Admin reset password for user '" + updatedUser.getUsername() + "'");
        }

        return toUserResponse(updatedUser);



    }
}
