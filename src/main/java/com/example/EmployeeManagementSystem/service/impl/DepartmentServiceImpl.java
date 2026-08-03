package com.example.EmployeeManagementSystem.service.impl;

import com.example.EmployeeManagementSystem.dto.request.AssignManagerRequest;
import com.example.EmployeeManagementSystem.dto.request.CreateDepartmentRequest;
import com.example.EmployeeManagementSystem.dto.request.UpdateDepartmentRequest;
import com.example.EmployeeManagementSystem.dto.response.DepartmentResponse;
import com.example.EmployeeManagementSystem.dto.response.PageResponse;
import com.example.EmployeeManagementSystem.exception.DuplicatedException;
import com.example.EmployeeManagementSystem.exception.InvalidTaskStateException;
import com.example.EmployeeManagementSystem.exception.ResourceNotFoundException;
import com.example.EmployeeManagementSystem.model.*;
import com.example.EmployeeManagementSystem.repository.AuditLogRepository;
import com.example.EmployeeManagementSystem.repository.DepartmentRepository;
import com.example.EmployeeManagementSystem.repository.UserRepository;
import com.example.EmployeeManagementSystem.service.AuditLogService;
import com.example.EmployeeManagementSystem.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
    }


    private DepartmentResponse toResponse(Department department) {
        return DepartmentResponse.builder()
                .id(department.getId())
                .name(department.getName())
                .managerId(department.getManager() != null ? department.getManager().getId() : null)
                .managerUsername(department.getManager() != null ? department.getManager().getUsername() : null)
                .employeeCount(department.getEmployees() != null ? department.getEmployees().size() : 0)
                .build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public DepartmentResponse createDepartment(CreateDepartmentRequest request) {
        User currentUser = getCurrentUser();
        if (departmentRepository.existsByName(request.getName())) {
            throw new DuplicatedException("Department name already exists");
        }

        Department department = new Department();
        department.setName(request.getName());
        Department saved = departmentRepository.save(department);

        auditLogService.record(currentUser, "CREATE_DEPARTMENT", saved.getId(), AuditTargetType.DEPARTMENT,
                "Created department '" + saved.getName() + "'");


        return toResponse(saved);
    }

    @Override
    public DepartmentResponse getDepartmentById(Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + id));
        return toResponse(department);
    }

    @Override
    public PageResponse<DepartmentResponse> getAllDepartments(Pageable pageable) {
        Page<DepartmentResponse> page = departmentRepository.findAll(pageable).map(this::toResponse);
        return PageResponse.from(page);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public DepartmentResponse updateDepartment(Long id, UpdateDepartmentRequest request) {
        User currentUser = getCurrentUser();
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + id));

        if (!department.getName().equals(request.getName())
                && departmentRepository.existsByName(request.getName())) {
            throw new DuplicatedException("Department name already exists");
        }

        department.setName(request.getName());
        Department updated = departmentRepository.save(department);

        auditLogService.record(currentUser, "UPDATE_DEPARTMENT", updated.getId(), AuditTargetType.DEPARTMENT,
                "Renamed department to '" + updated.getName() + "'");


        return toResponse(updated);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public void deleteDepartment(Long id) {
        User currentUser = getCurrentUser();
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + id));


        // block the deletion if the department has employees assigned to it.
        // admin need to reassign them explicitly first.
        List<User> employees = userRepository.findByDepartment(department);
        if (!employees.isEmpty()) {
            throw new InvalidTaskStateException(
                    "Cannot delete department '" + department.getName() + "': it still has "
                            + employees.size() + " employee(s) assigned. Reassign them first.");
        }

        //delete the department
        departmentRepository.delete(department);

        auditLogService.record(currentUser, "DELETE_DEPARTMENT", id, AuditTargetType.DEPARTMENT,
                "Deleted department '" + department.getName() + "'");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public DepartmentResponse assignManager(Long departmentId, AssignManagerRequest request) {
        User currentUser = getCurrentUser();
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + departmentId));

        // Single Manager Constraint
        if (department.getManager() != null) {
            throw new InvalidTaskStateException(
                    "Department '" + department.getName()
                            + "' already has a manager. Remove the current manager before assigning a new one.");
        }

        User newManager = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.getUserId()));

        if (newManager.getRole() != Role.MANAGER) {
            throw new InvalidTaskStateException(
                    "User must already hold the MANAGER role before being assigned to head a department");
        }

        department.setManager(newManager);
        Department updated = departmentRepository.save(department);

        auditLogService.record(currentUser, "ASSIGN_MANAGER", updated.getId(), AuditTargetType.DEPARTMENT,
                "Assigned '" + newManager.getUsername() + "' as manager of '" + updated.getName() + "'");

        return toResponse(updated);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public DepartmentResponse removeManager(Long departmentId) {
        User currentUser = getCurrentUser();
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + departmentId));

        String previousManagerUsername = department.getManager() != null ? department.getManager().getUsername() : null;
        department.setManager(null);
        Department updated = departmentRepository.save(department);

        auditLogService.record(currentUser, "REMOVE_MANAGER", updated.getId(), AuditTargetType.DEPARTMENT,
                "Removed manager" + (previousManagerUsername != null ? " '" + previousManagerUsername + "'" : "")
                        + " from '" + updated.getName() + "'");

        return toResponse(updated);
    }
}