import apiClient from "./client";
import type { PageResponse } from "../types/pagination";
import type { Task, CreateTaskRequest, UpdateTaskRequest } from "../types/task";

export async function getTasks(page: number, size: number): Promise<PageResponse<Task>> {
  const response = await apiClient.get<PageResponse<Task>>("/tasks", { params: { page, size } });
  return response.data;
}

export async function createTask(request: CreateTaskRequest): Promise<Task> {
  const response = await apiClient.post<Task>("/tasks", request);
  return response.data;
}

export async function updateTask(id: number, request: UpdateTaskRequest): Promise<Task> {
  const response = await apiClient.put<Task>(`/tasks/${id}`, request);
  return response.data;
}

export async function deleteTask(id: number): Promise<void> {
  await apiClient.delete(`/tasks/${id}`);
}

export async function startTask(id: number): Promise<Task> {
  const response = await apiClient.patch<Task>(`/tasks/${id}/start`);
  return response.data;
}

export async function submitForReview(id: number): Promise<Task> {
  const response = await apiClient.patch<Task>(`/tasks/${id}/submit-for-review`);
  return response.data;
}

export async function approveTask(id: number): Promise<Task> {
  const response = await apiClient.patch<Task>(`/tasks/${id}/approve`);
  return response.data;
}

export async function rejectTask(id: number): Promise<Task> {
  const response = await apiClient.patch<Task>(`/tasks/${id}/reject`);
  return response.data;
}