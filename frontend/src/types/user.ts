import type { Role } from "./auth";

// Matches UserResponse.java exactly
export interface User {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  username: string;
  role: Role;
  departmentName: string | null;
  createdAt: string; // ISO datetime string - backend sends LocalDateTime as JSON string
}

// Matches CreateUserRequest.java
export interface CreateUserRequest {
  username: string;
  password: string;
  email: string;
  firstName: string;
  lastName: string;
  role: Role;
  departmentId: number | null;
}

// Matches UpdateUserRequest.java
export interface UpdateUserRequest {
  firstName: string;
  lastName: string;
  email: string;
}

// Matches ChangeRoleRequest.java
export interface ChangeRoleRequest {
  role: Role;
}

// Matches AssignDepartmentRequest.java
export interface AssignDepartmentRequest {
  departmentId: number;
}

// Matches PasswordChangeRequest.java
export interface PasswordChangeRequest {
  oldPassword: string;
  newPassword: string;
}