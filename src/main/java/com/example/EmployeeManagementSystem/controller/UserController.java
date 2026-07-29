package com.example.EmployeeManagementSystem.controller;

import com.example.EmployeeManagementSystem.dto.request.*;
import com.example.EmployeeManagementSystem.dto.response.UserResponse;
import com.example.EmployeeManagementSystem.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@RequestBody @Valid CreateUserRequest request)
            throws IllegalAccessException {
        UserResponse response = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ADMIN sees any user, MANAGER sees users in their own department, EMPLOYEE sees only themselves
    // (fine-grained check lives inside UserServiceImpl.getUserById)
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @GetMapping("/username/{username}")
    public ResponseEntity<UserResponse> getUserByUsername(@PathVariable String username) {
        return ResponseEntity.ok(userService.getUserByUsername(username));
    }

    // ADMIN: all users. MANAGER: own department. EMPLOYEE: just themselves.
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    // ADMIN or MANAGER (department-match enforced inside the service)
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long id, @RequestBody @Valid UpdateUserRequest request) {
        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    // ADMIN only
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    // ADMIN only - promotion rules (active tasks, single-manager constraint) enforced in service
    @PatchMapping("/{id}/role")
    public ResponseEntity<UserResponse> changeRole(@PathVariable Long id, @RequestBody @Valid ChangeRoleRequest request) {
        return ResponseEntity.ok(userService.changeRole(id, request));
    }

    // ADMIN only
    @PatchMapping("/{id}/department")
    public ResponseEntity<UserResponse> assignDepartment(@PathVariable Long id, @RequestBody @Valid AssignDepartmentRequest request) {
        return ResponseEntity.ok(userService.assignDepartment(id, request));
    }

    // Self, or ADMIN resetting someone else's password (enforced inside the service)
    @PatchMapping("/{id}/password")
    public ResponseEntity<UserResponse> updatePassword(@PathVariable Long id, @RequestBody @Valid PasswordChangeRequest request) {
        return ResponseEntity.ok(userService.updatePassword(id, request));
    }
}