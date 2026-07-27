package com.example.EmployeeManagementSystem.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class DepartmentResponse {
    private Long id;
    private String name;
    private Long managerId;
    private String managerUsername;
    private int employeeCount;
}