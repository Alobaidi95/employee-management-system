import { Box, Button, Container, Paper, Typography, Chip } from "@mui/material";
import { useAuth } from "../context/AuthContext";

export default function Dashboard() {
  const { user, logout } = useAuth();

  return (
    <Container maxWidth="sm">
      <Box sx={{ mt: 10 }}>
        <Paper elevation={3} sx={{ p: 4 }}>
          <Typography variant="h4" component="h1" gutterBottom>
            Dashboard
          </Typography>

          <Box sx={{ display: "flex", alignItems: "center", gap: 1, mb: 3 }}>
            <Typography>
              Logged in as <strong>{user?.username}</strong>
            </Typography>
            <Chip label={user?.role} color="primary" size="small" />
          </Box>

          <Button variant="outlined" color="error" onClick={logout}>
            Log out
          </Button>
        </Paper>
      </Box>
    </Container>
  );
}
