import apiClient from "./client";
import type { PageResponse } from "../types/pagination";
import type { User, CreateUserRequest, UpdateUserRequest, ChangeRoleRequest, AssignDepartmentRequest, PasswordChangeRequest } from "../types/user";

// baseURL in client.ts already includes "/api", so this hits
// GET http://localhost:8080/api/users?page=0&size=20
export async function getUsers(page: number, size: number): Promise<PageResponse<User>> {
  const response = await apiClient.get<PageResponse<User>>("/users", {
    params: { page, size },
  });
  return response.data;
}

export async function createUser(request: CreateUserRequest): Promise<User> {
  const response = await apiClient.post<User>("/users", request);
  return response.data;
}

export async function updateUser(id: number, request: UpdateUserRequest): Promise<User> {
  const response = await apiClient.put<User>(`/users/${id}`, request);
  return response.data;
}

export async function deleteUser(id: number): Promise<void> {
  await apiClient.delete(`/users/${id}`);
}

export async function changeRole(id: number, request: ChangeRoleRequest): Promise<User> {
  const response = await apiClient.patch<User>(`/users/${id}/role`, request);
  return response.data;
}

export async function assignUserDepartment(id: number, request: AssignDepartmentRequest): Promise<User> {
  const response = await apiClient.patch<User>(`/users/${id}/department`, request);
  return response.data;
}

// Fetches every MANAGER, used to populate the "assign manager" dropdown
// on the Departments page. Reuses the same paginated /users endpoint
// with a large size, same tradeoff as getAllDepartments.
export async function getAllManagers(): Promise<User[]> {
  const response = await apiClient.get<{ content: User[] }>("/users", {
    params: { page: 0, size: 100 },
  });
  return response.data.content.filter((u) => u.role === "MANAGER");
}

export async function getUserByUsername(username: string): Promise<User> {
  const response = await apiClient.get<User>(`/users/username/${username}`);
  return response.data;
}

// Used by CreateTaskDialog when a MANAGER is creating a task - they can
// only assign to EMPLOYEEs in their own department, so this filters
// client-side after fetching. Same "fetch a big page, filter locally"
// tradeoff as getAllManagers.
export async function getEmployeesInDepartment(departmentName: string): Promise<User[]> {
  const response = await apiClient.get<{ content: User[] }>("/users", {
    params: { page: 0, size: 100 },
  });
  return response.data.content.filter((u) => u.role === "EMPLOYEE" && u.departmentName === departmentName);
}

export async function changePassword(id: number, request: PasswordChangeRequest): Promise<User> {
  const response = await apiClient.patch<User>(`/users/${id}/password`, request);
  return response.data;
}