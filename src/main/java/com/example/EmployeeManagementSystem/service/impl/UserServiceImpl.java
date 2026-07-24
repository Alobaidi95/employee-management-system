package com.example.EmployeeManagementSystem.service.impl;

import com.example.EmployeeManagementSystem.dto.request.*;
import com.example.EmployeeManagementSystem.dto.response.UserResponse;
import com.example.EmployeeManagementSystem.exception.DuplicatedException;
import com.example.EmployeeManagementSystem.exception.ResourceNotFoundException;
import com.example.EmployeeManagementSystem.exception.UnMatchedPasswordsException;
import com.example.EmployeeManagementSystem.exception.UnauthorizedAccessException;
import com.example.EmployeeManagementSystem.model.*;
import com.example.EmployeeManagementSystem.repository.AuditLogRepository;
import com.example.EmployeeManagementSystem.repository.DepartmentRepository;
import com.example.EmployeeManagementSystem.repository.TaskRepository;
import com.example.EmployeeManagementSystem.repository.UserRepository;
import com.example.EmployeeManagementSystem.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final TaskRepository taskRepository;
    private final AuditLogRepository  auditLogRepository;


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

    private void writeAuditLog(User performedBy, String action, Long targetId,
                               String targetType, String details) {
        AuditLog log = new AuditLog();
        log.setAction(action);
        log.setPerformedBy(performedBy);
        log.setTargetId(targetId);
        log.setTargetType(targetType);
        log.setDetails(details != null && details.length() > 1000
                ? details.substring(0, 1000)
                : details);
        log.setTimestamp(LocalDateTime.now());
        auditLogRepository.save(log);
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

        User savedUser = userRepository.save(newUser);

        writeAuditLog(currentUser, "CREATE_USER", savedUser.getId(), "User",
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
    public List<UserResponse> getAllUsers() {
        User currentUser = getCurrentUser();
        Role role = currentUser.getRole();

        if (role == Role.ADMIN) {
            return userRepository.findAll()
                    .stream()
                    .map(this::toUserResponse)
                    .collect(Collectors.toList());
        }

        if (role == Role.MANAGER) {
            Department dept = currentUser.getDepartment();
            if (dept == null) {
                return List.of();
            }
            return userRepository.findByDepartment(dept)
                    .stream()
                    .map(this::toUserResponse)
                    .collect(Collectors.toList());
        }

        return List.of(toUserResponse(currentUser));

    }

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

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        User updatedUser = userRepository.save(user);

        writeAuditLog(currentUser, "UPDATE_USER", updatedUser.getId(), "User",
                "Updated profile for user '" + updatedUser.getUsername() + "'");

        return toUserResponse(updatedUser);
    }


    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public void deleteUser(Long id) {
        User currentUser = getCurrentUser();
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        userRepository.delete(user);
        writeAuditLog(currentUser, "DELETE_USER", id, "User", "Deleted user '" + user.getUsername() + "'");
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
                throw new IllegalStateException(
                        "Cannot promote user: employee still has active (non-DONE) tasks");
            }

            Department department = user.getDepartment();
            if (department == null) {
                throw new IllegalStateException("Cannot promote user: user has no department assigned");
            }
            // Single Manager Constraint: department must not already have a manager
            if (department.getManager() != null) {
                throw new IllegalStateException(
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

        writeAuditLog(currentUser, "CHANGE_ROLE", updatedUser.getId(), "User",
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

        writeAuditLog(currentUser, "ASSIGN_DEPARTMENT", updatedUser.getId(), "User",
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
            writeAuditLog(currentUser, "RESET_PASSWORD", updatedUser.getId(), "User",
                    "Admin reset password for user '" + updatedUser.getUsername() + "'");
        }

        return toUserResponse(updatedUser);



    }
}
