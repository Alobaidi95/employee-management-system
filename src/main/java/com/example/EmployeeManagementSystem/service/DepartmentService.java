package com.example.EmployeeManagementSystem.service;

import com.example.EmployeeManagementSystem.dto.request.AssignManagerRequest;
import com.example.EmployeeManagementSystem.dto.request.CreateDepartmentRequest;
import com.example.EmployeeManagementSystem.dto.request.UpdateDepartmentRequest;
import com.example.EmployeeManagementSystem.dto.response.DepartmentResponse;

import java.util.List;

public interface DepartmentService {
    DepartmentResponse createDepartment(CreateDepartmentRequest request);
    DepartmentResponse getDepartmentById(Long id);
    List<DepartmentResponse> getAllDepartments();
    DepartmentResponse updateDepartment(Long id, UpdateDepartmentRequest request);
    void deleteDepartment(Long id);
    DepartmentResponse assignManager(Long departmentId, AssignManagerRequest request);
    DepartmentResponse removeManager(Long departmentId);
}
