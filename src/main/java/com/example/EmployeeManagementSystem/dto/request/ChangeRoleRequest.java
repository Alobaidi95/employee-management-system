package com.example.EmployeeManagementSystem.dto.request;

import com.example.EmployeeManagementSystem.model.Role;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ChangeRoleRequest {
    @NotNull(message = "Role is required")
    private Role role;
}
