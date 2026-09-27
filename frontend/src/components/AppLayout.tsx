import { Outlet, NavLink, useNavigate } from "react-router-dom";
import {
  Box,
  Typography,
  Chip,
  Snackbar,
  Alert,
  Drawer,
  AppBar,
  Toolbar,
  IconButton,
  useMediaQuery,
  useTheme,
} from "@mui/material";
import MenuIcon from "@mui/icons-material/Menu";
import DashboardOutlinedIcon from "@mui/icons-material/DashboardOutlined";
import PeopleOutlineIcon from "@mui/icons-material/PeopleOutlined";
import ApartmentOutlinedIcon from "@mui/icons-material/ApartmentOutlined";
import AssignmentOutlinedIcon from "@mui/icons-material/AssignmentOutlined";
import HistoryOutlinedIcon from "@mui/icons-material/HistoryOutlined";
import LogoutOutlinedIcon from "@mui/icons-material/LogoutOutlined";
import LockResetOutlinedIcon from "@mui/icons-material/LockResetOutlined";
import { useState } from "react";
import { useAuth } from "../context/AuthContext";
import ChangePasswordDialog from "./ChangePasswordDialog";
import { colors } from "../theme/theme";
import type { Role } from "../types/auth";

interface NavItem {
  label: string;
  to: string;
  icon: React.ReactNode;
  roles: Role[]; // which roles see this link
}

const navItems: NavItem[] = [
  { label: "Dashboard", to: "/dashboard", icon: <DashboardOutlinedIcon fontSize="small" />, roles: ["ADMIN", "MANAGER", "EMPLOYEE"] },
  { label: "Users", to: "/users", icon: <PeopleOutlineIcon fontSize="small" />, roles: ["ADMIN", "MANAGER"] },
  { label: "Departments", to: "/departments", icon: <ApartmentOutlinedIcon fontSize="small" />, roles: ["ADMIN"] },
  { label: "Tasks", to: "/tasks", icon: <AssignmentOutlinedIcon fontSize="small" />, roles: ["ADMIN", "MANAGER", "EMPLOYEE"] },
  { label: "Audit Log", to: "/audit-log", icon: <HistoryOutlinedIcon fontSize="small" />, roles: ["ADMIN"] },
];

const DRAWER_WIDTH = 240;

export default function AppLayout() {
  const { user, logout, sessionWarning, dismissSessionWarning } = useAuth();
  const navigate = useNavigate();
  const [isChangePasswordOpen, setIsChangePasswordOpen] = useState(false);
  const [mobileOpen, setMobileOpen] = useState(false);

  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down("md"));

  function handleLogout() {
    logout();
    navigate("/login");
  }

  const visibleItems = navItems.filter((item) => user && item.roles.includes(user.role));

  // The sidebar content is shared between the permanent (desktop) and
  // temporary (mobile) drawers so we only maintain it in one place.
  const sidebarContent = (
    <Box
      sx={{
        width: DRAWER_WIDTH,
        height: "100%",
        bgcolor: colors.ink,
        color: "#E7E9F5",
        display: "flex",
        flexDirection: "column",
      }}
    >
      {/* Brand */}
      <Box sx={{ px: 3, py: 3.5 }}>
        <Typography
          sx={{
            fontFamily: '"IBM Plex Mono", monospace',
            fontSize: 10.5,
            letterSpacing: "0.18em",
            textTransform: "uppercase",
            color: colors.accent,
            mb: 0.5,
          }}
        >
          EMS
        </Typography>
        <Typography sx={{ fontWeight: 600, fontSize: 15, lineHeight: 1.3 }}>
          Employee Management
        </Typography>
      </Box>

      {/* Nav links */}
      <Box component="ul" sx={{ listStyle: "none", m: 0, p: 0, px: 1.5, flex: 1 }}>
        {visibleItems.map((item) => (
          <Box component="li" key={item.to} sx={{ mb: 0.5 }}>
            <NavLink
              to={item.to}
              onClick={() => setMobileOpen(false)}
              style={({ isActive }) => ({
                display: "flex",
                alignItems: "center",
                gap: 10,
                padding: "9px 14px",
                borderRadius: 8,
                textDecoration: "none",
                color: isActive ? "#fff" : "#B4B8CC",
                background: isActive ? colors.primary : "transparent",
                fontFamily: '"IBM Plex Sans", sans-serif',
                fontSize: 13.5,
                fontWeight: isActive ? 600 : 500,
                transition: "background 0.15s ease",
              })}
            >
              {item.icon}
              {item.label}
            </NavLink>
          </Box>
        ))}
      </Box>

      {/* Current user + logout */}
      <Box sx={{ px: 2, py: 2.5, borderTop: "1px solid rgba(255,255,255,0.08)" }}>
        <Box sx={{ display: "flex", alignItems: "center", justifyContent: "space-between", mb: 1.5, px: 1 }}>
          <Box>
            <Typography sx={{ fontSize: 13, fontWeight: 600, color: "#fff" }}>
              {user?.username}
            </Typography>
            <Chip
              label={user?.role}
              size="small"
              sx={{
                mt: 0.5,
                height: 18,
                fontSize: 10,
                fontFamily: '"IBM Plex Mono", monospace',
                bgcolor: "rgba(232,179,74,0.15)",
                color: colors.accent,
                fontWeight: 600,
              }}
            />
          </Box>
        </Box>

        <Box
          component="button"
          onClick={() => setIsChangePasswordOpen(true)}
          sx={{
            display: "flex",
            alignItems: "center",
            gap: 1,
            width: "100%",
            px: 1.5,
            py: 1,
            border: "none",
            borderRadius: 8,
            bgcolor: "transparent",
            color: "#B4B8CC",
            fontFamily: '"IBM Plex Sans", sans-serif',
            fontSize: 13,
            cursor: "pointer",
            "&:hover": { bgcolor: "rgba(255,255,255,0.06)", color: "#fff" },
          }}
        >
          <LockResetOutlinedIcon fontSize="small" />
          Change password
        </Box>

        <Box
          component="button"
          onClick={handleLogout}
          sx={{
            display: "flex",
            alignItems: "center",
            gap: 1,
            width: "100%",
            px: 1.5,
            py: 1,
            border: "none",
            borderRadius: 8,
            bgcolor: "transparent",
            color: "#B4B8CC",
            fontFamily: '"IBM Plex Sans", sans-serif',
            fontSize: 13,
            cursor: "pointer",
            "&:hover": { bgcolor: "rgba(255,255,255,0.06)", color: "#fff" },
          }}
        >
          <LogoutOutlinedIcon fontSize="small" />
          Log out
        </Box>

        <ChangePasswordDialog open={isChangePasswordOpen} onClose={() => setIsChangePasswordOpen(false)} />
      </Box>
    </Box>
  );

  return (
    <Box sx={{ display: "flex", minHeight: "100vh" }}>
      {isMobile ? (
        <>
          {/* Top bar with hamburger menu — mobile only */}
          <AppBar
            position="fixed"
            elevation={0}
            sx={{
              bgcolor: colors.ink,
              borderBottom: `3px solid ${colors.accent}`,
            }}
          >
            <Toolbar sx={{ minHeight: 56 }}>
              <IconButton
                edge="start"
                onClick={() => setMobileOpen(true)}
                sx={{ color: "#fff", mr: 1 }}
                aria-label="open navigation menu"
              >
                <MenuIcon />
              </IconButton>
              <Typography sx={{ fontWeight: 600, fontSize: 15, color: "#fff" }}>
                Employee Management
              </Typography>
            </Toolbar>
          </AppBar>

          {/* Temporary drawer — slides over content, closes on link click or backdrop tap */}
          <Drawer
            variant="temporary"
            open={mobileOpen}
            onClose={() => setMobileOpen(false)}
            ModalProps={{ keepMounted: true }} // better mobile performance on reopen
            sx={{
              "& .MuiDrawer-paper": {
                width: DRAWER_WIDTH,
                border: "none",
              },
            }}
          >
            {sidebarContent}
          </Drawer>
        </>
      ) : (
        // Permanent sidebar — desktop/tablet, unchanged from before
        <Box
          component="nav"
          sx={{
            width: DRAWER_WIDTH,
            flexShrink: 0,
            borderRight: `3px solid ${colors.accent}`,
          }}
        >
          {sidebarContent}
        </Box>
      )}

      {/* Page content */}
      <Box
        component="main"
        sx={{
          flex: 1,
          bgcolor: "#F2F3F7",
          overflow: "auto",
          // push content below the fixed AppBar on mobile
          mt: isMobile ? "56px" : 0,
          width: "100%",
          minWidth: 0, // prevents flex children (tables etc.) from overflowing on small screens
        }}
      >
        <Outlet />
      </Box>

      <Snackbar
        open={sessionWarning}
        anchorOrigin={{ vertical: "bottom", horizontal: "center" }}
      >
        <Alert severity="warning" onClose={dismissSessionWarning} sx={{ fontFamily: '"IBM Plex Sans", sans-serif' }}>
          Your session will expire soon. Save any unfinished work - you'll need to log in again shortly.
        </Alert>
      </Snackbar>
    </Box>
  );
}
