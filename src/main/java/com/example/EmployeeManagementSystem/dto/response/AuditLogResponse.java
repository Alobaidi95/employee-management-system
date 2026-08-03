package com.example.EmployeeManagementSystem.dto.response;

import com.example.EmployeeManagementSystem.model.AuditTargetType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class AuditLogResponse {

    private Long id;
    private String action;
    private Long performedById;
    private String performedByUsername;
    private Long targetId;
    private AuditTargetType targetType;
    private String details;
    private LocalDateTime timestamp;

}