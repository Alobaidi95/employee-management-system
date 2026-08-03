package com.example.EmployeeManagementSystem.service.impl;

import com.example.EmployeeManagementSystem.dto.response.AuditLogResponse;
import com.example.EmployeeManagementSystem.dto.response.PageResponse;
import com.example.EmployeeManagementSystem.exception.ResourceNotFoundException;
import com.example.EmployeeManagementSystem.model.AuditLog;
import com.example.EmployeeManagementSystem.model.AuditTargetType;
import com.example.EmployeeManagementSystem.model.User;
import com.example.EmployeeManagementSystem.repository.AuditLogRepository;
import com.example.EmployeeManagementSystem.repository.UserRepository;
import com.example.EmployeeManagementSystem.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    private AuditLogResponse toResponse(AuditLog log) {
        return AuditLogResponse.builder()
                .id(log.getId())
                .action(log.getAction())
                .performedById(log.getPerformedBy() != null ? log.getPerformedBy().getId() : null)
                .performedByUsername(log.getPerformedBy() != null ? log.getPerformedBy().getUsername() : null)
                .targetId(log.getTargetId())
                .targetType(log.getTargetType())
                .details(log.getDetails())
                .timestamp(log.getTimestamp())
                .build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public PageResponse<AuditLogResponse> getAllLogs(Pageable pageable) {
        Page<AuditLogResponse> page = auditLogRepository.findAll(pageable).map(this::toResponse);
        return PageResponse.from(page);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public PageResponse<AuditLogResponse> getLogsByUser(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Page<AuditLogResponse> page = auditLogRepository.findByPerformedBy(user, pageable).map(this::toResponse);
        return PageResponse.from(page);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public PageResponse<AuditLogResponse> getLogsForTarget(Long targetId, AuditTargetType targetType, Pageable pageable) {
        Page<AuditLogResponse> page = auditLogRepository
                .findByTargetIdAndTargetType(targetId, targetType, pageable)
                .map(this::toResponse);
        return PageResponse.from(page);
    }


    @Override
    public void record(User performedBy, String action, Long targetId, AuditTargetType targetType, String details) {
        AuditLog log = new AuditLog();
        log.setAction(action);
        log.setPerformedBy(performedBy);
        log.setTargetId(targetId);
        log.setTargetType(targetType);
        log.setDetails(details != null && details.length() > 1000 ? details.substring(0, 1000) : details);
        log.setTimestamp(LocalDateTime.now());
        auditLogRepository.save(log);
    }
}