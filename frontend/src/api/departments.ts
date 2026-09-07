import apiClient from "./client";
import type { PageResponse } from "../types/pagination";
import type { Department, CreateDepartmentRequest, UpdateDepartmentRequest, AssignManagerRequest } from "../types/department";

// Fetches a large page size to effectively get "all" departments, since
// there's no dedicated non-paginated endpoint. Fine at company scale;
// revisit if the department list ever gets huge.
export async function getAllDepartments(): Promise<Department[]> {
  const response = await apiClient.get<PageResponse<Department>>("/departments", {
    params: { page: 0, size: 100 },
  });
  return response.data.content;
}

// Paginated version for the Departments table (getAllDepartments above
// stays as-is, used only for dropdown pickers elsewhere)
export async function getDepartments(page: number, size: number): Promise<PageResponse<Department>> {
  const response = await apiClient.get<PageResponse<Department>>("/departments", {
    params: { page, size },
  });
  return response.data;
}

export async function createDepartment(request: CreateDepartmentRequest): Promise<Department> {
  const response = await apiClient.post<Department>("/departments", request);
  return response.data;
}

export async function updateDepartment(id: number, request: UpdateDepartmentRequest): Promise<Department> {
  const response = await apiClient.put<Department>(`/departments/${id}`, request);
  return response.data;
}

export async function deleteDepartment(id: number): Promise<void> {
  await apiClient.delete(`/departments/${id}`);
}

export async function assignManager(id: number, request: AssignManagerRequest): Promise<Department> {
  const response = await apiClient.patch<Department>(`/departments/${id}/manager`, request);
  return response.data;
}

export async function removeManager(id: number): Promise<Department> {
  const response = await apiClient.delete<Department>(`/departments/${id}/manager`);
  return response.data;
}