// Matches your backend's Role enum exactly (model/Role.java)
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

// What we decode out of the JWT payload itself (see JwtUtil.generateToken:
// it puts the username in "sub" and the role in a custom "role" claim)
export interface DecodedToken {
  sub: string; // username
  role: Role;
  iat: number; // issued-at (unix seconds)
  exp: number; // expiration (unix seconds)
}