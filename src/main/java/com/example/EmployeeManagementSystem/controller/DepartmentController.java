package com.example.EmployeeManagementSystem.controller;

import com.example.EmployeeManagementSystem.dto.request.AssignManagerRequest;
import com.example.EmployeeManagementSystem.dto.request.CreateDepartmentRequest;
import com.example.EmployeeManagementSystem.dto.request.UpdateDepartmentRequest;
import com.example.EmployeeManagementSystem.dto.response.DepartmentResponse;
import com.example.EmployeeManagementSystem.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    // ADMIN only
    @PostMapping
    public ResponseEntity<DepartmentResponse> createDepartment(@RequestBody CreateDepartmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(departmentService.createDepartment(request));
    }

    // any authenticated user
    @GetMapping("/{id}")
    public ResponseEntity<DepartmentResponse> getDepartmentById(@PathVariable Long id) {
        return ResponseEntity.ok(departmentService.getDepartmentById(id));
    }

    // any authenticated user
    @GetMapping
    public ResponseEntity<List<DepartmentResponse>> getAllDepartments() {
        return ResponseEntity.ok(departmentService.getAllDepartments());
    }

    // ADMIN only
    @PutMapping("/{id}")
    public ResponseEntity<DepartmentResponse> updateDepartment(@PathVariable Long id, @RequestBody UpdateDepartmentRequest request) {
        return ResponseEntity.ok(departmentService.updateDepartment(id, request));
    }

    // ADMIN only
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDepartment(@PathVariable Long id) {
        departmentService.deleteDepartment(id);
        return ResponseEntity.noContent().build();
    }

    // ADMIN only - fails if department already has a manager (single-manager constraint)
    @PatchMapping("/{id}/manager")
    public ResponseEntity<DepartmentResponse> assignManager(@PathVariable Long id, @RequestBody AssignManagerRequest request) {
        return ResponseEntity.ok(departmentService.assignManager(id, request));
    }

    // ADMIN only
    @DeleteMapping("/{id}/manager")
    public ResponseEntity<DepartmentResponse> removeManager(@PathVariable Long id) {
        return ResponseEntity.ok(departmentService.removeManager(id));
    }
}