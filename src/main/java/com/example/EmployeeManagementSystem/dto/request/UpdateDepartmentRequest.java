package com.example.EmployeeManagementSystem.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateDepartmentRequest {

    @NotBlank(message = "Department name is required")
    private String name;
}