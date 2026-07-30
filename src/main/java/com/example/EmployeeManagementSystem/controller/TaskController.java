package com.example.EmployeeManagementSystem.controller;

import com.example.EmployeeManagementSystem.dto.request.CreateTaskRequest;
import com.example.EmployeeManagementSystem.dto.request.UpdateTaskRequest;
import com.example.EmployeeManagementSystem.dto.response.TaskResponse;
import com.example.EmployeeManagementSystem.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@Tag(name = "Department Management", description = "APIs for managing tasks")
public class TaskController {

    private final TaskService taskService;

    // ADMIN -> MANAGER only, MANAGER -> own-department EMPLOYEE only (enforced in service)
    @PostMapping
    @Operation(summary = "Create task" , description = "ADMIN -> MANAGER only, MANAGER -> own-department EMPLOYEE only")
    public ResponseEntity<TaskResponse> createTask(@RequestBody @Valid CreateTaskRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.createTask(request));
    }

    // ADMIN: any task. MANAGER: tasks they assigned or tasks in their department.
    // EMPLOYEE: only their own tasks.
    @GetMapping("/{id}")
    @Operation(summary = "Get a task by its id" ,description = "ADMIN: any task. MANAGER: tasks they assigned or tasks in their department ,EMPLOYEE: only their own tasks")
    public ResponseEntity<TaskResponse> getTaskById(@PathVariable Long id) {
        return ResponseEntity.ok(taskService.getTaskById(id));
    }

    @GetMapping
    @Operation(summary = "Get all tasks")
    public ResponseEntity<List<TaskResponse>> getAllTasks() {
        return ResponseEntity.ok(taskService.getAllTasks());
    }

    // Assigner or ADMIN only
    @PutMapping("/{id}")
    @Operation(summary = "Update task" , description = "Assigner or ADMIN only")
    public ResponseEntity<TaskResponse> updateTask(@PathVariable Long id, @RequestBody @Valid UpdateTaskRequest request) {
        return ResponseEntity.ok(taskService.updateTask(id, request));
    }

    // Assignee only. Requires task to currently be ASSIGNED.
    @PatchMapping("/{id}/start")
    @Operation(summary = "Start task" , description = "when the employee accept and start the assigned task")
    public ResponseEntity<TaskResponse> startTask(@PathVariable Long id) {
        return ResponseEntity.ok(taskService.startTask(id));
    }

    // Assignee only. Requires task to currently be STARTED.
    @PatchMapping("/{id}/submit-for-review")
    @Operation(summary = "submit for review" , description = "employee submit the task for his manager to review it")
    public ResponseEntity<TaskResponse> submitForReview(@PathVariable Long id) {
        return ResponseEntity.ok(taskService.submitForReview(id));
    }

    // Assigner or ADMIN only. Requires task to currently be UNDER_REVIEW.
    @PatchMapping("/{id}/approve")
    @Operation(summary = "Approve task" , description = "manager can approve employee task")
    public ResponseEntity<TaskResponse> approveTask(@PathVariable Long id) {
        return ResponseEntity.ok(taskService.approveTask(id));
    }

    // Assigner or ADMIN only. Requires task to currently be UNDER_REVIEW.
    // Sends the task back to the employee as ASSIGNED.
    @PatchMapping("/{id}/reject")
    @Operation(summary = "reject task" , description = "manager can reject employee task")
    public ResponseEntity<TaskResponse> rejectTask(@PathVariable Long id) {
        return ResponseEntity.ok(taskService.rejectTask(id));
    }

    // Assigner or ADMIN only
    @DeleteMapping("/{id}")
    @Operation(summary = "delete task" , description = "Assigner or ADMIN only ")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }
}