import apiClient from "./client";
import type { LoginRequest, AuthResponse } from "../types/auth";

export async function login(credentials: LoginRequest): Promise<AuthResponse> {
  const response = await apiClient.post<AuthResponse>("/auth/login", credentials);
  return response.data;
}