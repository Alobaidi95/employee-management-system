package com.example.EmployeeManagementSystem.dto.response;

import com.example.EmployeeManagementSystem.model.Role;
import lombok.*;

import java.time.LocalDateTime;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String username;
    private Role role;
    private String departmentName;
    private LocalDateTime createdAt;
}
