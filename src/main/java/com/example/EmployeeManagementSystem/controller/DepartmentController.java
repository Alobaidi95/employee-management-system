package com.example.EmployeeManagementSystem.controller;

import com.example.EmployeeManagementSystem.dto.request.AssignManagerRequest;
import com.example.EmployeeManagementSystem.dto.request.CreateDepartmentRequest;
import com.example.EmployeeManagementSystem.dto.request.UpdateDepartmentRequest;
import com.example.EmployeeManagementSystem.dto.response.DepartmentResponse;
import com.example.EmployeeManagementSystem.dto.response.PageResponse;
import com.example.EmployeeManagementSystem.service.DepartmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
@Tag(name = "Department Management", description = "APIs for managing departments")
public class DepartmentController {

    private final DepartmentService departmentService;

    // ADMIN only
    @PostMapping
    @Operation(summary = "Create a department")
    public ResponseEntity<DepartmentResponse> createDepartment(@RequestBody @Valid CreateDepartmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(departmentService.createDepartment(request));
    }

    // any authenticated user
    @GetMapping("/{id}")
    @Operation(summary = "get department by its id")
    public ResponseEntity<DepartmentResponse> getDepartmentById(@PathVariable Long id) {
        return ResponseEntity.ok(departmentService.getDepartmentById(id));
    }

    // any authenticated user
    @GetMapping
    @Operation(summary = "Get all Departments" , description = "returns a list for all departments")
    public ResponseEntity<PageResponse<DepartmentResponse>> getAllDepartments(
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return ResponseEntity.ok(departmentService.getAllDepartments(pageable));
    }

    // ADMIN only
    @PutMapping("/{id}")
    @Operation(summary = "update departments information" , description = "only admin can update the department")
    public ResponseEntity<DepartmentResponse> updateDepartment(@PathVariable Long id, @RequestBody @Valid UpdateDepartmentRequest request) {
        return ResponseEntity.ok(departmentService.updateDepartment(id, request));
    }

    // ADMIN only
    @DeleteMapping("/{id}")
    @Operation(summary = "delete a department" , description = "Only admin can perform this action")
    public ResponseEntity<Void> deleteDepartment(@PathVariable Long id) {
        departmentService.deleteDepartment(id);
        return ResponseEntity.noContent().build();
    }

    // ADMIN only - fails if department already has a manager (single-manager constraint)
    @PatchMapping("/{id}/manager")
    @Operation(summary = "Assign a manager to a department" , description = "ADMIN only - fails if department already has a manager")
    public ResponseEntity<DepartmentResponse> assignManager(@PathVariable Long id, @RequestBody @Valid AssignManagerRequest request) {
        return ResponseEntity.ok(departmentService.assignManager(id, request));
    }

    // ADMIN only
    @DeleteMapping("/{id}/manager")
    @Operation(summary = "remove manager from department" , description = "Only Admin can perform this action")
    public ResponseEntity<DepartmentResponse> removeManager(@PathVariable Long id) {
        return ResponseEntity.ok(departmentService.removeManager(id));
    }
}