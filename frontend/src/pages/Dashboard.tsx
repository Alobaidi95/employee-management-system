import { useEffect, useState } from "react";
import { Box, Typography, Alert } from "@mui/material";
import { useAuth } from "../context/AuthContext";
import { colors } from "../theme/theme";
import { getTasks } from "../api/tasks";
import { getUsers } from "../api/users";
import { getDepartments } from "../api/departments";
import type { Task } from "../types/task";
import StatCard from "../components/StatCard";

interface TaskStats {
  total: number;
  started: number;
  underReview: number;
  done: number;
  overdue: number;
}

export default function Dashboard() {
  const { user } = useAuth();
  const [taskStats, setTaskStats] = useState<TaskStats | null>(null);
  const [totalUsers, setTotalUsers] = useState<number | null>(null);
  const [totalDepartments, setTotalDepartments] = useState<number | null>(null);
  const [teamSize, setTeamSize] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!user) return;
    const currentUser = user; // narrowed to non-null, and won't change - TS trusts this inside the closure below
    let cancelled = false;

    async function loadStats() {
      setError(null);
      try {
        // getTasks is already scoped server-side by role (ADMIN sees all,
        // MANAGER sees their own-assigned + department, EMPLOYEE sees
        // only their own) - so the same call and the same math work for
        // every role, no per-role branching needed here at all.
        // NOTE: size=100 means these stats undercount past 100 visible
        // tasks - fine at current scale, would need a dedicated backend
        // count/aggregate endpoint to be fully accurate at real scale.
        const taskPage = await getTasks(0, 100);
        if (cancelled) return;

        const tasks: Task[] = taskPage.content;
        const today = new Date().toISOString().slice(0, 10); // "YYYY-MM-DD"

        setTaskStats({
          total: taskPage.totalElements,
          started: tasks.filter((t) => t.status === "STARTED").length,
          underReview: tasks.filter((t) => t.status === "UNDER_REVIEW").length,
          done: tasks.filter((t) => t.status === "DONE").length,
          overdue: tasks.filter((t) => t.status !== "DONE" && t.endDate < today).length,
        });

        if (currentUser.role === "ADMIN") {
          const [usersPage, deptsPage] = await Promise.all([getUsers(0, 1), getDepartments(0, 1)]);
          if (!cancelled) {
            setTotalUsers(usersPage.totalElements);
            setTotalDepartments(deptsPage.totalElements);
          }
        } else if (currentUser.role === "MANAGER") {
          // GET /users is already scoped to the manager's own department
          // server-side, so this total IS the team size, no filtering needed
          const usersPage = await getUsers(0, 1);
          if (!cancelled) setTeamSize(usersPage.totalElements);
        }
      } catch {
        if (!cancelled) setError("Couldn't load dashboard stats.");
      }
    }

    loadStats();
    return () => {
      cancelled = true;
    };
  }, [user]);

  const taskLabel = user?.role === "EMPLOYEE" ? "My Tasks" : user?.role === "MANAGER" ? "Team Tasks" : "All Tasks";

  return (
    <Box sx={{ p: 4 }}>
      <Typography
        sx={{
          fontFamily: '"IBM Plex Mono", monospace',
          fontSize: 11,
          letterSpacing: "0.14em",
          textTransform: "uppercase",
          color: colors.textFaint,
          mb: 0.5,
        }}
      >
        Dashboard
      </Typography>
      <Typography variant="h5" sx={{ fontWeight: 600, color: colors.textDark, mb: 4 }}>
        Welcome back, {user?.username}
      </Typography>

      {error && <Alert severity="error" sx={{ mb: 3 }}>{error}</Alert>}

      <Box sx={{ display: "flex", flexWrap: "wrap", gap: 2 }}>
        <StatCard label={taskLabel} value={taskStats?.total ?? null} />
        <StatCard label="In Progress" value={taskStats?.started ?? null} accentColor="#9C7412" />
        <StatCard label="Awaiting Review" value={taskStats?.underReview ?? null} accentColor="#7A3FC9" />
        <StatCard label="Completed" value={taskStats?.done ?? null} accentColor="#1E8A4C" />
        <StatCard label="Overdue" value={taskStats?.overdue ?? null} accentColor="#D14343" />

        {user?.role === "ADMIN" && (
          <>
            <StatCard label="Total Users" value={totalUsers} />
            <StatCard label="Departments" value={totalDepartments} />
          </>
        )}

        {user?.role === "MANAGER" && <StatCard label="Team Size" value={teamSize} />}
      </Box>
    </Box>
  );
}
