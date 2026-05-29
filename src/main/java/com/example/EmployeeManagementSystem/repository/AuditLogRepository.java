package com.example.EmployeeManagementSystem.repository;

import com.example.EmployeeManagementSystem.model.AuditLog;
import com.example.EmployeeManagementSystem.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByPerformedBy(User user);
    List<AuditLog> findByTargetIdAndTargetType(Long targetId, String targetType);
    List<AuditLog> findAllByOrderByTimestampDesc();
}
