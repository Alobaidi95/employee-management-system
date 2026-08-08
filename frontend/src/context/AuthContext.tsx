import { createContext, useContext, useState, useEffect } from "react";
import type { ReactNode } from "react";
import type { Role } from "../types/auth";
import { decodeToken, isTokenExpired } from "../utils/jwt";
import { login as loginApi } from "../api/auth";

interface AuthUser {
  username: string;
  role: Role;
}

interface AuthContextValue {
  user: AuthUser | null;
  isAuthenticated: boolean;
  login: (username: string, password: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null);

  // On first load (e.g. page refresh), check if a still-valid token
  // already exists in localStorage and restore the session from it -
  // otherwise every refresh would log the user out, which would be a bad
  // experience even though we chose localStorage over a refresh-token flow.
  useEffect(() => {
    const token = localStorage.getItem("token");
    if (!token) return;

    const decoded = decodeToken(token);
    if (!decoded || isTokenExpired(decoded)) {
      localStorage.removeItem("token");
      return;
    }

    setUser({ username: decoded.sub, role: decoded.role });
  }, []);

  async function login(username: string, password: string) {
    const { token } = await loginApi({ username, password });
    const decoded = decodeToken(token);
    if (!decoded) {
      throw new Error("Received an invalid token from the server");
    }

    localStorage.setItem("token", token);
    setUser({ username: decoded.sub, role: decoded.role });
  }

  function logout() {
    localStorage.removeItem("token");
    setUser(null);
  }

  return (
    <AuthContext.Provider value={{ user, isAuthenticated: !!user, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

// Custom hook so components just call useAuth() instead of importing
// useContext + AuthContext everywhere
export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
}
