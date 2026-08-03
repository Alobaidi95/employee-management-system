package com.example.EmployeeManagementSystem.repository;

import com.example.EmployeeManagementSystem.model.AuditLog;
import com.example.EmployeeManagementSystem.model.AuditTargetType;
import com.example.EmployeeManagementSystem.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByPerformedBy(User user);
    List<AuditLog> findByTargetIdAndTargetType(Long targetId, AuditTargetType targetType);

    Page<AuditLog> findByPerformedBy(User user, Pageable pageable);
    Page<AuditLog> findByTargetIdAndTargetType(Long targetId, AuditTargetType targetType, Pageable pageable);
}