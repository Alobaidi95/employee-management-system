package com.example.EmployeeManagementSystem.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class UpdateTaskRequest {
    private String title;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
}