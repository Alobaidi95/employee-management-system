// Matches  backend's Role enum exactly (model/Role.java)
export type Role = "ADMIN" | "MANAGER" | "EMPLOYEE";

// Matches LoginRequest.java
export interface LoginRequest {
  username: string;
  password: string;
}

// Matches AuthResponse.java
export interface AuthResponse {
  token: string;
}


export interface DecodedToken {
  sub: string; // username
  role: Role;
  iat: number; // issued-at (unix seconds)
  exp: number; // expirgetDepartmentsation (unix seconds)
}