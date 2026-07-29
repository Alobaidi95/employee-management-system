package com.example.EmployeeManagementSystem.controller;

import com.example.EmployeeManagementSystem.dto.request.CreateTaskRequest;
import com.example.EmployeeManagementSystem.dto.request.UpdateTaskRequest;
import com.example.EmployeeManagementSystem.dto.response.TaskResponse;
import com.example.EmployeeManagementSystem.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    // ADMIN -> MANAGER only, MANAGER -> own-department EMPLOYEE only (enforced in service)
    @PostMapping
    public ResponseEntity<TaskResponse> createTask(@RequestBody CreateTaskRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.createTask(request));
    }

    // ADMIN: any task. MANAGER: tasks they assigned or tasks in their department.
    // EMPLOYEE: only their own tasks.
    @GetMapping("/{id}")
    public ResponseEntity<TaskResponse> getTaskById(@PathVariable Long id) {
        return ResponseEntity.ok(taskService.getTaskById(id));
    }

    @GetMapping
    public ResponseEntity<List<TaskResponse>> getAllTasks() {
        return ResponseEntity.ok(taskService.getAllTasks());
    }

    // Assigner or ADMIN only
    @PutMapping("/{id}")
    public ResponseEntity<TaskResponse> updateTask(@PathVariable Long id, @RequestBody UpdateTaskRequest request) {
        return ResponseEntity.ok(taskService.updateTask(id, request));
    }

    // Assignee only. Requires task to currently be ASSIGNED.
    @PatchMapping("/{id}/start")
    public ResponseEntity<TaskResponse> startTask(@PathVariable Long id) {
        return ResponseEntity.ok(taskService.startTask(id));
    }

    // Assignee only. Requires task to currently be STARTED.
    @PatchMapping("/{id}/submit-for-review")
    public ResponseEntity<TaskResponse> submitForReview(@PathVariable Long id) {
        return ResponseEntity.ok(taskService.submitForReview(id));
    }

    // Assigner or ADMIN only. Requires task to currently be UNDER_REVIEW.
    @PatchMapping("/{id}/approve")
    public ResponseEntity<TaskResponse> approveTask(@PathVariable Long id) {
        return ResponseEntity.ok(taskService.approveTask(id));
    }

    // Assigner or ADMIN only. Requires task to currently be UNDER_REVIEW.
    // Sends the task back to the employee as ASSIGNED.
    @PatchMapping("/{id}/reject")
    public ResponseEntity<TaskResponse> rejectTask(@PathVariable Long id) {
        return ResponseEntity.ok(taskService.rejectTask(id));
    }

    // Assigner or ADMIN only
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }
}