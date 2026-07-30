package com.example.EmployeeManagementSystem.repository;

import com.example.EmployeeManagementSystem.model.Department;
import com.example.EmployeeManagementSystem.model.Status;
import com.example.EmployeeManagementSystem.model.Task;
import com.example.EmployeeManagementSystem.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    boolean existsByAssignedToAndStatusNot(User user, Status status);

    List<Task> findByAssignedTo(User user);
    List<Task> findByStatus(Status status);
    List<Task> findByAssignedBy(User user);


    Page<Task> findByAssignedTo(User user, Pageable pageable);
    Page<Task> findByAssignedBy(User user, Pageable pageable);


    @Query("SELECT t FROM Task t WHERE t.assignedBy = :manager OR t.assignedTo.department = :department")
    Page<Task> findVisibleToManager(@Param("manager") User manager,
                                    @Param("department") Department department,
                                    Pageable pageable);
}
