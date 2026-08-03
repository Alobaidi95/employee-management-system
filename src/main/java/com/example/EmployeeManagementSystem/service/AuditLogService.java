package com.example.EmployeeManagementSystem.service;

import com.example.EmployeeManagementSystem.dto.response.AuditLogResponse;
import com.example.EmployeeManagementSystem.dto.response.PageResponse;
import com.example.EmployeeManagementSystem.model.AuditTargetType;
import com.example.EmployeeManagementSystem.model.User;
import org.springframework.data.domain.Pageable;

public interface AuditLogService {

    // Read side - ADMIN only, enforced in the impl
    PageResponse<AuditLogResponse> getAllLogs(Pageable pageable);
    PageResponse<AuditLogResponse> getLogsByUser(Long userId, Pageable pageable);
    PageResponse<AuditLogResponse> getLogsForTarget(Long targetId, AuditTargetType targetType, Pageable pageable);

    // Write side - called internally by UserServiceImpl/DepartmentServiceImpl
    void record(User performedBy, String action, Long targetId, AuditTargetType targetType, String details);
}