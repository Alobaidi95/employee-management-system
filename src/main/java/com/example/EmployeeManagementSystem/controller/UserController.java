package com.example.EmployeeManagementSystem.controller;

import com.example.EmployeeManagementSystem.dto.request.*;
import com.example.EmployeeManagementSystem.dto.response.PageResponse;
import com.example.EmployeeManagementSystem.dto.response.UserResponse;
import com.example.EmployeeManagementSystem.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "APIs for managing user accounts and roles")
public class UserController {

    private final UserService userService;
    @Operation(summary = "Create user " , description = " only admin can create a user")
    @PostMapping
    public ResponseEntity<UserResponse> createUser(@RequestBody @Valid CreateUserRequest request)
            throws IllegalAccessException {
        UserResponse response = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ADMIN sees any user, MANAGER sees users in their own department, EMPLOYEE sees only themselves
    // (fine-grained check lives inside UserServiceImpl.getUserById)
    @GetMapping("/{id}")
    @Operation(summary = "get a user by his id " , description = " admin can get all users , Manager can get users in his department , user can get only his information")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @GetMapping("/username/{username}")
    @Operation(summary = "get a user by his username " , description = " admin can get all users , Manager can get users in his department , user can get only his information")
    public ResponseEntity<UserResponse> getUserByUsername(@PathVariable String username) {
        return ResponseEntity.ok(userService.getUserByUsername(username));
    }

    // ADMIN: all users. MANAGER: own department. EMPLOYEE: just themselves.
    @GetMapping
    @Operation(summary = "Get all users", description = "Retrieves a paginated list of all registered users in the system.")
    public ResponseEntity<PageResponse<UserResponse>> getAllUsers(
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(userService.getAllUsers(pageable));
    }

    // ADMIN or MANAGER (department-match enforced inside the service)
    @PutMapping("/{id}")
    @Operation(summary = "update user informations " , description = " admin can update all users , Manager can  update in his department")
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long id, @RequestBody @Valid UpdateUserRequest request) {
        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    // ADMIN only
    @DeleteMapping("/{id}")
    @Operation(summary = "delete a user")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    // ADMIN only - promotion rules (active tasks, single-manager constraint) enforced in service
    @PatchMapping("/{id}/role")
    @Operation(summary = "delete a user" , description = "only admin can perform this action")
    public ResponseEntity<UserResponse> changeRole(@PathVariable Long id, @RequestBody @Valid ChangeRoleRequest request) {
        return ResponseEntity.ok(userService.changeRole(id, request));
    }

    // ADMIN only
    @PatchMapping("/{id}/department")
    @Operation(summary = "set user to a department" , description = "only admin can perform this action")
    public ResponseEntity<UserResponse> assignDepartment(@PathVariable Long id, @RequestBody @Valid AssignDepartmentRequest request) {
        return ResponseEntity.ok(userService.assignDepartment(id, request));
    }

    // Self, or ADMIN resetting someone else's password (enforced inside the service)
    @PatchMapping("/{id}/password")
    @Operation(summary = "Update password" , description = "admin or self can change the password")
    public ResponseEntity<UserResponse> updatePassword(@PathVariable Long id, @RequestBody @Valid PasswordChangeRequest request) {
        return ResponseEntity.ok(userService.updatePassword(id, request));
    }
}