package com.example.EmployeeManagementSystem.dto.response;

import com.example.EmployeeManagementSystem.model.Status;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Builder
public class TaskResponse {
    private Long id;
    private String title;
    private String description;
    private Status status;
    private Long assignedToId;
    private String assignedToUsername;
    private Long assignedById;
    private String assignedByUsername;
    private LocalDate startDate;
    private LocalDate endDate;
}