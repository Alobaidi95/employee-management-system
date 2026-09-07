import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { ThemeProvider, CssBaseline } from "@mui/material";
import theme from "./theme/theme";
import { AuthProvider } from "./context/AuthContext";
import ProtectedRoute from "./routes/ProtectedRoute";
import AppLayout from "./components/AppLayout";
import Login from "./pages/Login";
import Dashboard from "./pages/Dashboard";
import Users from "./pages/Users";
import Departments from "./pages/Departments";
import Tasks from "./pages/Tasks";
import AuditLogPage from "./pages/AuditLog";
import ErrorBoundary from "./components/ErrorBoundary";

export default function App() {
  return (
    <ThemeProvider theme={theme}>
      {/* Normalizes default browser styling (margins, font smoothing,
          box-sizing) to match MUI's design system - the more thorough,
          theme-aware equivalent of the plain CSS reset in index.css */}
      <CssBaseline />
      <ErrorBoundary>
      <BrowserRouter>
        <AuthProvider>
          <Routes>
            <Route path="/login" element={<Login />} />

            {/* Every route nested inside here requires auth AND renders
                inside AppLayout (sidebar + content area). Add new pages
                as siblings of Dashboard below as we build them. */}
            <Route
              element={
                <ProtectedRoute>
                  <AppLayout />
                </ProtectedRoute>
              }
            >
              <Route path="/dashboard" element={<Dashboard />} />
              <Route path="/users" element={<Users />} />
              <Route path="/departments" element={<Departments />} />
              <Route path="/tasks" element={<Tasks />} />
              <Route path="/audit-log" element={<AuditLogPage />} />
            </Route>

            <Route path="*" element={<Navigate to="/dashboard" replace />} />
          </Routes>
        </AuthProvider>
      </BrowserRouter>
      </ErrorBoundary>
    </ThemeProvider>
  );
}
