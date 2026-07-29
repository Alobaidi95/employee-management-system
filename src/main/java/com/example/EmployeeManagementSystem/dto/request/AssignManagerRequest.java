package com.example.EmployeeManagementSystem.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssignManagerRequest {

    @NotNull(message = "User id is required")
    private Long userId;
}