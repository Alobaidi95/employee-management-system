package com.example.EmployeeManagementSystem.service.impl;

import com.example.EmployeeManagementSystem.dto.request.CreateTaskRequest;
import com.example.EmployeeManagementSystem.dto.request.UpdateTaskRequest;
import com.example.EmployeeManagementSystem.dto.response.PageResponse;
import com.example.EmployeeManagementSystem.dto.response.TaskResponse;
import com.example.EmployeeManagementSystem.exception.InvalidTaskStateException;
import com.example.EmployeeManagementSystem.exception.ResourceNotFoundException;
import com.example.EmployeeManagementSystem.exception.UnauthorizedAccessException;
import com.example.EmployeeManagementSystem.model.*;
import com.example.EmployeeManagementSystem.repository.AuditLogRepository;
import com.example.EmployeeManagementSystem.repository.TaskRepository;
import com.example.EmployeeManagementSystem.repository.UserRepository;
import com.example.EmployeeManagementSystem.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
    }

    private void writeAuditLog(User performedBy, String action, Long targetId, String targetType, String details) {
        AuditLog log = new AuditLog();
        log.setAction(action);
        log.setPerformedBy(performedBy);
        log.setTargetId(targetId);
        log.setTargetType(targetType);
        log.setDetails(details != null && details.length() > 1000 ? details.substring(0, 1000) : details);
        log.setTimestamp(LocalDateTime.now());
        auditLogRepository.save(log);
    }

    private TaskResponse toResponse(Task task) {
        return TaskResponse.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus())
                .assignedToId(task.getAssignedTo() != null ? task.getAssignedTo().getId() : null)
                .assignedToUsername(task.getAssignedTo() != null ? task.getAssignedTo().getUsername() : null)
                .assignedById(task.getAssignedBy() != null ? task.getAssignedBy().getId() : null)
                .assignedByUsername(task.getAssignedBy() != null ? task.getAssignedBy().getUsername() : null)
                .startDate(task.getStartDate())
                .endDate(task.getEndDate())
                .build();
    }

    private boolean sameDepartment(User a, User b) {
        return a.getDepartment() != null && b.getDepartment() != null
                && a.getDepartment().getId().equals(b.getDepartment().getId());
    }

    @Override
    public TaskResponse createTask(CreateTaskRequest request) {
        User currentUser = getCurrentUser();
        User assignedTo = userRepository.findById(request.getAssignedToId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with id: " + request.getAssignedToId()));

        if (currentUser.getRole() == Role.ADMIN) {
            if (assignedTo.getRole() != Role.MANAGER) {
                throw new UnauthorizedAccessException("Admins can only assign tasks to managers");
            }
        } else if (currentUser.getRole() == Role.MANAGER) {
            if (assignedTo.getRole() != Role.EMPLOYEE || !sameDepartment(currentUser, assignedTo)) {
                throw new UnauthorizedAccessException(
                        "Managers can only assign tasks to employees in their own department");
            }
        } else {
            throw new UnauthorizedAccessException("You don't have permission to assign tasks");
        }

        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new InvalidTaskStateException("End date cannot be before start date");
        }

        Task task = new Task();
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setStatus(Status.ASSIGNED);
        task.setAssignedTo(assignedTo);
        task.setAssignedBy(currentUser);
        task.setStartDate(request.getStartDate());
        task.setEndDate(request.getEndDate());

        Task saved = taskRepository.save(task);

        writeAuditLog(currentUser, "CREATE_TASK", saved.getId(), "Task",
                "Assigned task '" + saved.getTitle() + "' to '" + assignedTo.getUsername() + "'");

        return toResponse(saved);
    }

    private boolean canView(User currentUser, Task task) {
        if (currentUser.getRole() == Role.ADMIN) {
            return true;
        }
        if (currentUser.getRole() == Role.MANAGER) {
            return currentUser.getId().equals(task.getAssignedBy().getId())
                    || sameDepartment(currentUser, task.getAssignedTo());
        }
        return task.getAssignedTo() != null && currentUser.getId().equals(task.getAssignedTo().getId());
    }

    @Override
    public TaskResponse getTaskById(Long id) {
        User currentUser = getCurrentUser();
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));

        if (!canView(currentUser, task)) {
            throw new UnauthorizedAccessException("You don't have permission to view this task");
        }
        return toResponse(task);
    }

    @Override
    public PageResponse<TaskResponse> getAllTasks(Pageable pageable) {
        User currentUser = getCurrentUser();

        if (currentUser.getRole() == Role.ADMIN) {
            Page<TaskResponse> page = taskRepository.findAll(pageable).map(this::toResponse);
            return PageResponse.from(page);
        }

        if (currentUser.getRole() == Role.MANAGER) {
            Department dept = currentUser.getDepartment();
            Page<Task> taskPage = dept != null
                    ? taskRepository.findVisibleToManager(currentUser, dept, pageable)
                    : taskRepository.findByAssignedBy(currentUser, pageable);
            return PageResponse.from(taskPage.map(this::toResponse));
        }

        Page<TaskResponse> page = taskRepository.findByAssignedTo(currentUser, pageable).map(this::toResponse);
        return PageResponse.from(page);
    }

    @Override
    public TaskResponse updateTask(Long id, UpdateTaskRequest request) {
        User currentUser = getCurrentUser();
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));

        boolean allowed = currentUser.getRole() == Role.ADMIN
                || currentUser.getId().equals(task.getAssignedBy().getId());
        if (!allowed) {
            throw new UnauthorizedAccessException("Only the assigner or an admin can edit this task");
        }

        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new InvalidTaskStateException("End date cannot be before start date");
        }

        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setStartDate(request.getStartDate());
        task.setEndDate(request.getEndDate());
        Task updated = taskRepository.save(task);

        writeAuditLog(currentUser, "UPDATE_TASK", updated.getId(), "Task",
                "Updated task '" + updated.getTitle() + "'");

        return toResponse(updated);
    }

    private Task findTask(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));
    }

    private boolean isAssignee(User user, Task task) {
        return task.getAssignedTo() != null && user.getId().equals(task.getAssignedTo().getId());
    }

    private boolean isAssignerOrAdmin(User user, Task task) {
        return user.getRole() == Role.ADMIN || user.getId().equals(task.getAssignedBy().getId());
    }

    private Task transition(Task task, Status expectedCurrent, Status next, String action) {
        if (task.getStatus() != expectedCurrent) {
            throw new InvalidTaskStateException(
                    "Cannot " + action + " task '" + task.getTitle() + "': expected status " + expectedCurrent
                            + " but task is currently " + task.getStatus());
        }
        task.setStatus(next);
        return taskRepository.save(task);
    }

    @Override
    public TaskResponse startTask(Long id) {
        User currentUser = getCurrentUser();
        Task task = findTask(id);

        if (!isAssignee(currentUser, task)) {
            throw new UnauthorizedAccessException("Only the assigned employee can start this task");
        }

        Task updated = transition(task, Status.ASSIGNED, Status.STARTED, "start");

        writeAuditLog(currentUser, "START_TASK", updated.getId(), "Task",
                "Started task '" + updated.getTitle() + "'");

        return toResponse(updated);
    }

    @Override
    public TaskResponse submitForReview(Long id) {
        User currentUser = getCurrentUser();
        Task task = findTask(id);

        if (!isAssignee(currentUser, task)) {
            throw new UnauthorizedAccessException("Only the assigned employee can submit this task for review");
        }

        Task updated = transition(task, Status.STARTED, Status.UNDER_REVIEW, "submit for review");

        writeAuditLog(currentUser, "SUBMIT_TASK_FOR_REVIEW", updated.getId(), "Task",
                "Submitted task '" + updated.getTitle() + "' for review");

        return toResponse(updated);
    }

    @Override
    public TaskResponse approveTask(Long id) {
        User currentUser = getCurrentUser();
        Task task = findTask(id);

        if (!isAssignerOrAdmin(currentUser, task)) {
            throw new UnauthorizedAccessException("Only the assigner or an admin can approve this task");
        }

        Task updated = transition(task, Status.UNDER_REVIEW, Status.DONE, "approve");

        writeAuditLog(currentUser, "APPROVE_TASK", updated.getId(), "Task",
                "Approved task '" + updated.getTitle() + "'");

        return toResponse(updated);
    }

    @Override
    public TaskResponse rejectTask(Long id) {
        User currentUser = getCurrentUser();
        Task task = findTask(id);

        if (!isAssignerOrAdmin(currentUser, task)) {
            throw new UnauthorizedAccessException("Only the assigner or an admin can reject this task");
        }

        Task updated = transition(task, Status.UNDER_REVIEW, Status.ASSIGNED, "reject");

        writeAuditLog(currentUser, "REJECT_TASK", updated.getId(), "Task",
                "Rejected task '" + updated.getTitle() + "' - sent back to " + updated.getAssignedTo().getUsername());

        return toResponse(updated);
    }

    @Override
    public void deleteTask(Long id) {
        User currentUser = getCurrentUser();
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));

        boolean allowed = currentUser.getRole() == Role.ADMIN
                || currentUser.getId().equals(task.getAssignedBy().getId());
        if (!allowed) {
            throw new UnauthorizedAccessException("Only the assigner or an admin can delete this task");
        }

        taskRepository.delete(task);

        writeAuditLog(currentUser, "DELETE_TASK", id, "Task", "Deleted task '" + task.getTitle() + "'");
    }
}