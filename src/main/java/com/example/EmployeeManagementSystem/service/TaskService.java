package com.example.EmployeeManagementSystem.service;

import com.example.EmployeeManagementSystem.dto.request.CreateTaskRequest;
import com.example.EmployeeManagementSystem.dto.request.UpdateTaskRequest;
import com.example.EmployeeManagementSystem.dto.response.PageResponse;
import com.example.EmployeeManagementSystem.dto.response.TaskResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface TaskService {
    TaskResponse createTask(CreateTaskRequest request);
    TaskResponse getTaskById(Long id);
    PageResponse<TaskResponse> getAllTasks(Pageable pageable);
    TaskResponse updateTask(Long id, UpdateTaskRequest request);
    void deleteTask(Long id);


    TaskResponse startTask(Long id);           // ASSIGNED -> STARTED, by the assignee
    TaskResponse submitForReview(Long id);     // STARTED -> UNDER_REVIEW, by the assignee
    TaskResponse approveTask(Long id);         // UNDER_REVIEW -> DONE, by the assigner or ADMIN
    TaskResponse rejectTask(Long id);          // UNDER_REVIEW -> ASSIGNED, by the assigner or ADMIN
}