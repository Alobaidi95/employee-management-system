package com.example.EmployeeManagementSystem.service.impl;

import com.example.EmployeeManagementSystem.dto.response.AuditLogResponse;
import com.example.EmployeeManagementSystem.dto.response.PageResponse;
import com.example.EmployeeManagementSystem.exception.ResourceNotFoundException;
import com.example.EmployeeManagementSystem.model.AuditLog;
import com.example.EmployeeManagementSystem.model.AuditTargetType;
import com.example.EmployeeManagementSystem.model.User;
import com.example.EmployeeManagementSystem.repository.AuditLogRepository;
import com.example.EmployeeManagementSystem.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceImplTest {

    @Mock private AuditLogRepository auditLogRepository;
    @Mock private UserRepository userRepository;

    private AuditLogServiceImpl auditLogService;

    private User admin;
    private final Pageable pageable = PageRequest.of(0, 20);

    @BeforeEach
    void setUp() {
        auditLogService = new AuditLogServiceImpl(auditLogRepository, userRepository);

        admin = new User();
        admin.setId(1L);
        admin.setUsername("admin");
    }

    private AuditLog logWith(Long id, User performedBy, String action) {
        AuditLog log = new AuditLog();
        log.setId(id);
        log.setPerformedBy(performedBy);
        log.setAction(action);
        log.setTargetId(99L);
        log.setTargetType(AuditTargetType.USER);
        log.setDetails("some details");
        return log;
    }



    @Nested
    class GetAllLogs {

        @Test
        void returnsAllLogsPaginated() {
            Page<AuditLog> page = new PageImpl<>(
                    List.of(logWith(1L, admin, "CREATE_USER"), logWith(2L, admin, "DELETE_USER")),
                    pageable, 2);
            when(auditLogRepository.findAll(pageable)).thenReturn(page);

            PageResponse<AuditLogResponse> result = auditLogService.getAllLogs(pageable);

            assertThat(result.getContent()).hasSize(2)
                    .extracting(AuditLogResponse::getAction)
                    .containsExactlyInAnyOrder("CREATE_USER", "DELETE_USER");
        }
    }

    @Nested
    class GetLogsByUser {

        @Test
        void returnsLogsForExistingUser() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
            Page<AuditLog> page = new PageImpl<>(List.of(logWith(1L, admin, "CREATE_USER")), pageable, 1);
            when(auditLogRepository.findByPerformedBy(admin, pageable)).thenReturn(page);

            PageResponse<AuditLogResponse> result = auditLogService.getLogsByUser(1L, pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getPerformedByUsername()).isEqualTo("admin");
        }

        @Test
        void throwsNotFound_whenUserDoesNotExist() {
            when(userRepository.findById(404L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> auditLogService.getLogsByUser(404L, pageable))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    class GetLogsForTarget {

        @Test
        void returnsLogsForGivenTargetIdAndType() {
            Page<AuditLog> page = new PageImpl<>(List.of(logWith(1L, admin, "CHANGE_ROLE")), pageable, 1);
            when(auditLogRepository.findByTargetIdAndTargetType(99L, AuditTargetType.USER, pageable))
                    .thenReturn(page);

            PageResponse<AuditLogResponse> result =
                    auditLogService.getLogsForTarget(99L, AuditTargetType.USER, pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getTargetType()).isEqualTo(AuditTargetType.USER);
        }
    }

    @Nested
    class Record {

        @Test
        void savesLogWithAllFieldsPopulated() {
            ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);

            auditLogService.record(admin, "CREATE_USER", 42L, AuditTargetType.USER, "Created user 'jdoe'");

            verify(auditLogRepository).save(captor.capture());
            AuditLog saved = captor.getValue();
            assertThat(saved.getPerformedBy()).isEqualTo(admin);
            assertThat(saved.getAction()).isEqualTo("CREATE_USER");
            assertThat(saved.getTargetId()).isEqualTo(42L);
            assertThat(saved.getTargetType()).isEqualTo(AuditTargetType.USER);
            assertThat(saved.getDetails()).isEqualTo("Created user 'jdoe'");
            assertThat(saved.getTimestamp()).isNotNull();
        }

        @Test
        void truncatesDetailsLongerThan1000Characters() {
            String longDetails = "x".repeat(1500);
            ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);

            auditLogService.record(admin, "UPDATE_USER", 42L, AuditTargetType.USER, longDetails);

            verify(auditLogRepository).save(captor.capture());
            assertThat(captor.getValue().getDetails()).hasSize(1000);
        }

        @Test
        void leavesShortDetailsUntouched() {
            ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);

            auditLogService.record(admin, "UPDATE_USER", 42L, AuditTargetType.USER, "short");

            verify(auditLogRepository).save(captor.capture());
            assertThat(captor.getValue().getDetails()).isEqualTo("short");
        }

        @Test
        void handlesNullDetailsGracefully() {
            ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);

            auditLogService.record(admin, "UPDATE_USER", 42L, AuditTargetType.USER, null);

            verify(auditLogRepository).save(captor.capture());
            assertThat(captor.getValue().getDetails()).isNull();
        }
    }
}
