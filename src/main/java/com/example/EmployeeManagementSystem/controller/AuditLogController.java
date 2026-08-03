package com.example.EmployeeManagementSystem.controller;

import com.example.EmployeeManagementSystem.dto.response.AuditLogResponse;
import com.example.EmployeeManagementSystem.dto.response.PageResponse;
import com.example.EmployeeManagementSystem.model.AuditTargetType;
import com.example.EmployeeManagementSystem.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    // ADMIN only (enforced in AuditLogServiceImpl). Newest first by default.
    @GetMapping
    public ResponseEntity<PageResponse<AuditLogResponse>> getAllLogs(
            @PageableDefault(size = 20, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(auditLogService.getAllLogs(pageable));
    }

    // "What has this user done?"
    @GetMapping("/by-user/{userId}")
    public ResponseEntity<PageResponse<AuditLogResponse>> getLogsByUser(
            @PathVariable Long userId,
            @PageableDefault(size = 20, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(auditLogService.getLogsByUser(userId, pageable));
    }

    // "What has happened to this specific entity?"
    // e.g. GET /api/audit-logs/by-target/5?targetType=USER
    @GetMapping("/by-target/{targetId}")
    public ResponseEntity<PageResponse<AuditLogResponse>> getLogsForTarget(
            @PathVariable Long targetId,
            @RequestParam AuditTargetType targetType,
            @PageableDefault(size = 20, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(auditLogService.getLogsForTarget(targetId, targetType, pageable));
    }
}