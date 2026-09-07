import { createContext, useContext, useState, useEffect, useRef } from "react";
import type { ReactNode } from "react";
import { useNavigate } from "react-router-dom";
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
  sessionWarning: boolean;
  dismissSessionWarning: () => void;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

const WARNING_LEAD_TIME_MS = 2 * 60 * 1000; // warn 2 minutes before expiry

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [sessionWarning, setSessionWarning] = useState(false);
  const navigate = useNavigate();

  // Timer ids live in refs, not state - they're side-effect bookkeeping,
  // not something a re-render should ever depend on
  const warningTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const expiryTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  function clearScheduledTimers() {
    if (warningTimerRef.current) clearTimeout(warningTimerRef.current);
    if (expiryTimerRef.current) clearTimeout(expiryTimerRef.current);
    warningTimerRef.current = null;
    expiryTimerRef.current = null;
  }

  function scheduleExpiryHandling(expiresAtMs: number) {
    clearScheduledTimers();
    setSessionWarning(false);

    const now = Date.now();
    const msUntilExpiry = expiresAtMs - now;
    const msUntilWarning = msUntilExpiry - WARNING_LEAD_TIME_MS;

    // If there's more than the lead time left, schedule the warning
    // normally. If we're already within the lead time (e.g. the page was
    // reloaded with a token that's about to expire), show it immediately
    // instead of scheduling a negative-delay timer.
    if (msUntilWarning > 0) {
      warningTimerRef.current = setTimeout(() => setSessionWarning(true), msUntilWarning);
    } else if (msUntilExpiry > 0) {
      setSessionWarning(true);
    }

    if (msUntilExpiry > 0) {
      expiryTimerRef.current = setTimeout(() => {
        localStorage.removeItem("token");
        setUser(null);
        setSessionWarning(false);
        navigate("/login", { state: { expired: true } });
      }, msUntilExpiry);
    }
  }

  useEffect(() => {
    const token = localStorage.getItem("token");
    if (!token) return;

    const decoded = decodeToken(token);
    if (!decoded || isTokenExpired(decoded)) {
      localStorage.removeItem("token");
      return;
    }

    setUser({ username: decoded.sub, role: decoded.role });
    scheduleExpiryHandling(decoded.exp * 1000);

    // Timers must be cleared on unmount (StrictMode double-invokes
    // effects in dev, and this avoids leaking timers across remounts)
    return clearScheduledTimers;
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function login(username: string, password: string) {
    const { token } = await loginApi({ username, password });
    const decoded = decodeToken(token);
    if (!decoded) {
      throw new Error("Received an invalid token from the server");
    }

    localStorage.setItem("token", token);
    setUser({ username: decoded.sub, role: decoded.role });
    scheduleExpiryHandling(decoded.exp * 1000);
  }

  function logout() {
    clearScheduledTimers();
    localStorage.removeItem("token");
    setUser(null);
    setSessionWarning(false);
  }

  function dismissSessionWarning() {
    setSessionWarning(false);
  }

  return (
    <AuthContext.Provider value={{ user, isAuthenticated: !!user, login, logout, sessionWarning, dismissSessionWarning }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
}
