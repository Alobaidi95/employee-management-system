import { useState } from "react";
import type { FormEvent } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import { Box, TextField, Button, Alert, CircularProgress } from "@mui/material";
import { useAuth } from "../context/AuthContext";

export default function Login() {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const sessionExpired = (location.state as { expired?: boolean } | null)?.expired ?? false;

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setIsSubmitting(true);

    try {
      await login(username, password);
      navigate("/dashboard");
    } catch {
      setError("Invalid username or password");
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <Box
      sx={{
        minHeight: "100vh",
        width: "100%",
        bgcolor: "#12141C",
        backgroundImage:
          "linear-gradient(rgba(255,255,255,0.03) 1px, transparent 1px), linear-gradient(90deg, rgba(255,255,255,0.03) 1px, transparent 1px)",
        backgroundSize: "28px 28px",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        px: 2,
      }}
    >
      {/* Badge card */}
      <Box
        sx={{
          position: "relative",
          width: "100%",
          maxWidth: 400,
          bgcolor: "#F7F7FA",
          borderRadius: "18px",
          boxShadow: "0 30px 60px -20px rgba(0,0,0,0.55)",
          overflow: "hidden",
        }}
      >
        {/* Lanyard strip */}
        <Box
          sx={{
            height: 10,
            width: "100%",
            bgcolor: "#E8B34A",
          }}
        />

        {/* Grommet hole */}
        <Box
          sx={{
            position: "absolute",
            top: -1,
            left: "50%",
            transform: "translate(-50%, 0)",
            width: 18,
            height: 18,
            borderRadius: "50%",
            bgcolor: "#12141C",
            border: "3px solid #F7F7FA",
          }}
        />

        <Box sx={{ px: 4, pt: 5, pb: 4 }}>
          <Box
            component="p"
            sx={{
              m: 0,
              mb: 1,
              fontFamily: '"IBM Plex Mono", monospace',
              fontSize: 11,
              letterSpacing: "0.18em",
              textTransform: "uppercase",
              color: "#6B7089",
              textAlign: "center",
            }}
          >
            Employee Management System
          </Box>

          <Box
            component="h1"
            sx={{
              m: 0,
              mb: 0.5,
              fontFamily: '"IBM Plex Sans", sans-serif',
              fontWeight: 600,
              fontSize: 22,
              color: "#1B1E2B",
              textAlign: "center",
            }}
          >
            Sign in to your workspace
          </Box>

          <Box
            component="p"
            sx={{
              m: 0,
              mb: 3.5,
              fontFamily: '"IBM Plex Sans", sans-serif',
              fontSize: 13.5,
              color: "#6B7089",
              textAlign: "center",
            }}
          >
            Enter your credentials to continue
          </Box>

          <Box component="form" onSubmit={handleSubmit} noValidate>
            <TextField
              label="Username"
              fullWidth
              margin="normal"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              required
              autoFocus
              slotProps={{ inputLabel: { sx: { fontFamily: '"IBM Plex Sans", sans-serif' } } }}
              sx={{
                "& .MuiOutlinedInput-root": {
                  borderRadius: "10px",
                  fontFamily: '"IBM Plex Sans", sans-serif',
                  "&.Mui-focused fieldset": { borderColor: "#4A5FE8" },
                },
                "& label.Mui-focused": { color: "#4A5FE8" },
              }}
            />

            <TextField
              label="Password"
              type="password"
              fullWidth
              margin="normal"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              slotProps={{ inputLabel: { sx: { fontFamily: '"IBM Plex Sans", sans-serif' } } }}
              sx={{
                "& .MuiOutlinedInput-root": {
                  borderRadius: "10px",
                  fontFamily: '"IBM Plex Sans", sans-serif',
                  "&.Mui-focused fieldset": { borderColor: "#4A5FE8" },
                },
                "& label.Mui-focused": { color: "#4A5FE8" },
              }}
            />

            {sessionExpired && !error && (
              <Alert
                severity="info"
                sx={{ mt: 2, borderRadius: "10px", fontFamily: '"IBM Plex Sans", sans-serif' }}
              >
                Your session expired. Please sign in again.
              </Alert>
            )}

            {error && (
              <Alert
                severity="error"
                sx={{ mt: 2, borderRadius: "10px", fontFamily: '"IBM Plex Sans", sans-serif' }}
              >
                {error}
              </Alert>
            )}

            <Button
              type="submit"
              fullWidth
              disabled={isSubmitting}
              sx={{
                mt: 3,
                py: 1.3,
                borderRadius: "10px",
                bgcolor: "#4A5FE8",
                color: "#fff",
                fontFamily: '"IBM Plex Sans", sans-serif',
                fontWeight: 600,
                letterSpacing: "0.04em",
                textTransform: "uppercase",
                fontSize: 13,
                "&:hover": { bgcolor: "#3B4CD1" },
                "&.Mui-disabled": { bgcolor: "#A9B0EE", color: "#fff" },
              }}
            >
              {isSubmitting ? <CircularProgress size={20} sx={{ color: "#fff" }} /> : "Sign in"}
            </Button>
          </Box>

          {/* Role legend - real info: the three roles this system supports */}
          <Box
            sx={{
              mt: 3.5,
              pt: 2.5,
              borderTop: "1px solid #E4E4EC",
              display: "flex",
              justifyContent: "center",
              gap: 1.2,
              fontFamily: '"IBM Plex Mono", monospace',
              fontSize: 10.5,
              letterSpacing: "0.08em",
              color: "#9599AC",
              textTransform: "uppercase",
            }}
          >
            <span>Admin</span>
            <span>·</span>
            <span>Manager</span>
            <span>·</span>
            <span>Employee</span>
          </Box>
        </Box>
      </Box>
    </Box>
  );
}
