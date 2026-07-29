package com.example.EmployeeManagementSystem.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CreateTaskRequest {
    private String title;
    private String description;
    private Long assignedToId;
    private LocalDate startDate;
    private LocalDate endDate;
}