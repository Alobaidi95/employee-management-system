package com.example.EmployeeManagementSystem.service;

import com.example.EmployeeManagementSystem.dto.request.AssignDepartmentRequest;
import com.example.EmployeeManagementSystem.dto.request.ChangeRoleRequest;
import com.example.EmployeeManagementSystem.dto.request.CreateUserRequest;
import com.example.EmployeeManagementSystem.dto.request.UpdateUserRequest;
import com.example.EmployeeManagementSystem.dto.response.UserResponse;
import com.example.EmployeeManagementSystem.model.User;

import java.util.List;

public interface UserService{

    UserResponse createUser(CreateUserRequest request);

    UserResponse getUserById(Long id);

    UserResponse getUserByUsername(String username);

    List<UserResponse> getAllUsers();

    UserResponse updateUser(Long id, UpdateUserRequest request);

    void deleteUser(Long id);

    UserResponse changeRole(Long id, ChangeRoleRequest request);

    UserResponse assignDepartment (Long id, AssignDepartmentRequest request);


}
