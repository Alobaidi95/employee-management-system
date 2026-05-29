package com.example.EmployeeManagementSystem.repository;

import com.example.EmployeeManagementSystem.model.Status;
import com.example.EmployeeManagementSystem.model.Task;
import com.example.EmployeeManagementSystem.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    boolean existsByAssignedToAndStatusNot(User user, Status status);

    List<Task> findByAssignedTo(User user);
    List<Task> findByStatus(Status status);
    List<Task> findByAssignedBy(User user);
}
