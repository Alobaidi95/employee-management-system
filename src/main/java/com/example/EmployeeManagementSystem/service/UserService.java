package com.example.EmployeeManagementSystem.service;

import com.example.EmployeeManagementSystem.dto.request.*;
import com.example.EmployeeManagementSystem.dto.response.PageResponse;
import com.example.EmployeeManagementSystem.dto.response.UserResponse;
import com.example.EmployeeManagementSystem.model.User;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface UserService{

    UserResponse createUser(CreateUserRequest request) throws IllegalAccessException;

    UserResponse getUserById(Long id);

    UserResponse getUserByUsername(String username);

    PageResponse<UserResponse> getAllUsers(Pageable pageable);

    UserResponse updateUser(Long id, UpdateUserRequest request);

    void deleteUser(Long id);

    UserResponse changeRole(Long id, ChangeRoleRequest request);

    UserResponse assignDepartment (Long id, AssignDepartmentRequest request);

    UserResponse updatePassword (Long id, PasswordChangeRequest request);

}
