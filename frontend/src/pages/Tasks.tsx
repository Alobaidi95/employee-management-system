import { useEffect, useState } from "react";
import {
  Box,
  Typography,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TablePagination,
  Chip,
  Button,
  CircularProgress,
  Alert,
  IconButton,
  Menu,
  MenuItem,
  Stack,
  TextField,
  InputAdornment,
} from "@mui/material";
import AddIcon from "@mui/icons-material/Add";
import MoreVertIcon from "@mui/icons-material/MoreVert";
import SearchIcon from "@mui/icons-material/Search";
import { getTasks, deleteTask, startTask, submitForReview, approveTask, rejectTask } from "../api/tasks";
import type { Task, Status } from "../types/task";
import { useAuth } from "../context/AuthContext";
import { colors } from "../theme/theme";
import CreateTaskDialog from "../components/CreateTaskDialog";
import EditTaskDialog from "../components/EditTaskDialog";
import ConfirmDialog from "../components/ConfirmDialog";

const statusStyles: Record<Status, { bg: string; color: string }> = {
  ASSIGNED: { bg: "rgba(74,95,232,0.1)", color: colors.primary },
  STARTED: { bg: "rgba(232,179,74,0.18)", color: "#9C7412" },
  UNDER_REVIEW: { bg: "rgba(155,89,232,0.12)", color: "#7A3FC9" },
  DONE: { bg: "rgba(46,166,95,0.12)", color: "#1E8A4C" },
};

export default function Tasks() {
  const { user: currentUser } = useAuth();
  // Same client-side search/pagination tradeoff as Users.tsx and
  // Departments.tsx - see the comment there for the caveat.
  const [allTasks, setAllTasks] = useState<Task[]>([]);
  const [searchTerm, setSearchTerm] = useState("");
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(10);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [refreshKey, setRefreshKey] = useState(0);
  const [actionError, setActionError] = useState<string | null>(null);

  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [menuAnchor, setMenuAnchor] = useState<HTMLElement | null>(null);
  const [selectedTask, setSelectedTask] = useState<Task | null>(null);
  const [editOpen, setEditOpen] = useState(false);
  const [deleteOpen, setDeleteOpen] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    async function loadTasks() {
      setIsLoading(true);
      setError(null);
      try {
        const data = await getTasks(0, 100);
        if (!cancelled) setAllTasks(data.content);
      } catch {
        if (!cancelled) setError("Couldn't load tasks. Try refreshing the page.");
      } finally {
        if (!cancelled) setIsLoading(false);
      }
    }

    loadTasks();
    return () => {
      cancelled = true;
    };
  }, [refreshKey]);

  useEffect(() => setPage(0), [searchTerm]);

  const filteredTasks = allTasks.filter((t) => {
    const term = searchTerm.trim().toLowerCase();
    if (!term) return true;
    return (
      t.title.toLowerCase().includes(term) ||
      t.assignedToUsername.toLowerCase().includes(term) ||
      t.assignedByUsername.toLowerCase().includes(term) ||
      t.status.toLowerCase().includes(term)
    );
  });

  const tasks = filteredTasks.slice(page * rowsPerPage, page * rowsPerPage + rowsPerPage);
  const totalElements = filteredTasks.length;

  function openMenu(e: React.MouseEvent<HTMLElement>, t: Task) {
    setMenuAnchor(e.currentTarget);
    setSelectedTask(t);
  }
  function closeMenu() {
    setMenuAnchor(null);
  }

  async function runWorkflowAction(action: (id: number) => Promise<Task>, task: Task) {
    setActionError(null);
    try {
      await action(task.id);
      setRefreshKey((k) => k + 1);
    } catch (err: any) {
      setActionError(err?.response?.data?.message ?? "Something went wrong updating this task.");
    }
  }

  async function handleDelete() {
    if (!selectedTask) return;
    setDeleteError(null);
    setIsDeleting(true);
    try {
      await deleteTask(selectedTask.id);
      setDeleteOpen(false);
      setRefreshKey((k) => k + 1);
    } catch (err: any) {
      setDeleteError(err?.response?.data?.message ?? "Couldn't delete this task.");
    } finally {
      setIsDeleting(false);
    }
  }

  function renderActions(task: Task) {
    if (!currentUser) return null;
    const isAssignee = currentUser.username === task.assignedToUsername;
    const isAssignerOrAdmin = currentUser.role === "ADMIN" || currentUser.username === task.assignedByUsername;

    const buttons: React.ReactNode[] = [];

    if (isAssignee && task.status === "ASSIGNED") {
      buttons.push(
        <Button key="start" size="small" variant="outlined" onClick={() => runWorkflowAction(startTask, task)}>
          Start
        </Button>
      );
    }
    if (isAssignee && task.status === "STARTED") {
      buttons.push(
        <Button key="submit" size="small" variant="outlined" onClick={() => runWorkflowAction(submitForReview, task)}>
          Submit for review
        </Button>
      );
    }
    if (isAssignerOrAdmin && task.status === "UNDER_REVIEW") {
      buttons.push(
        <Button key="approve" size="small" variant="contained" color="success" onClick={() => runWorkflowAction(approveTask, task)}>
          Approve
        </Button>,
        <Button key="reject" size="small" variant="outlined" color="warning" onClick={() => runWorkflowAction(rejectTask, task)}>
          Reject
        </Button>
      );
    }

    return (
      <Stack direction="row" spacing={1} sx={{ justifyContent: "flex-end", alignItems: "center" }}>
        {buttons}
        {isAssignerOrAdmin && (
          <IconButton size="small" onClick={(e) => openMenu(e, task)}>
            <MoreVertIcon fontSize="small" />
          </IconButton>
        )}
      </Stack>
    );
  }

  return (
    <Box sx={{ p: 4 }}>
      <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "flex-end", mb: 3 }}>
        <Box>
          <Typography sx={{ fontFamily: '"IBM Plex Mono", monospace', fontSize: 11, letterSpacing: "0.14em", textTransform: "uppercase", color: colors.textFaint, mb: 0.5 }}>
            Workflow
          </Typography>
          <Typography variant="h5" sx={{ fontWeight: 600, color: colors.textDark }}>
            Tasks
          </Typography>
        </Box>

        {(currentUser?.role === "ADMIN" || currentUser?.role === "MANAGER") && (
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={() => setIsCreateOpen(true)}
            sx={{ bgcolor: colors.primary, textTransform: "none", fontWeight: 600, "&:hover": { bgcolor: colors.primaryHover } }}
          >
            New task
          </Button>
        )}
      </Box>

      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
      {actionError && <Alert severity="error" sx={{ mb: 2 }} onClose={() => setActionError(null)}>{actionError}</Alert>}

      <TextField
        placeholder="Search by title, assignee, or status"
        size="small"
        value={searchTerm}
        onChange={(e) => setSearchTerm(e.target.value)}
        sx={{ mb: 2, width: 340, bgcolor: "#fff" }}
        slotProps={{
          input: {
            startAdornment: (
              <InputAdornment position="start">
                <SearchIcon fontSize="small" sx={{ color: colors.textFaint }} />
              </InputAdornment>
            ),
          },
        }}
      />

      <Box sx={{ bgcolor: "#fff", border: `1px solid ${colors.border}`, borderRadius: 2, overflow: "hidden" }}>
        <TableContainer>
          <Table>
            <TableHead>
              <TableRow sx={{ bgcolor: "#FAFAFC" }}>
                <TableCell sx={{ fontWeight: 600, fontSize: 12.5, color: colors.textMuted }}>Title</TableCell>
                <TableCell sx={{ fontWeight: 600, fontSize: 12.5, color: colors.textMuted }}>Assigned to</TableCell>
                <TableCell sx={{ fontWeight: 600, fontSize: 12.5, color: colors.textMuted }}>Assigned by</TableCell>
                <TableCell sx={{ fontWeight: 600, fontSize: 12.5, color: colors.textMuted }}>Status</TableCell>
                <TableCell sx={{ fontWeight: 600, fontSize: 12.5, color: colors.textMuted }}>Due</TableCell>
                <TableCell align="right" sx={{ fontWeight: 600, fontSize: 12.5, color: colors.textMuted }}>Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {isLoading ? (
                <TableRow>
                  <TableCell colSpan={6} align="center" sx={{ py: 6 }}>
                    <CircularProgress size={28} />
                  </TableCell>
                </TableRow>
              ) : tasks.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={6} align="center" sx={{ py: 6, color: colors.textMuted }}>
                    {searchTerm ? "No tasks match your search." : "No tasks found."}
                  </TableCell>
                </TableRow>
              ) : (
                tasks.map((t) => (
                  <TableRow key={t.id} hover>
                    <TableCell sx={{ fontSize: 13.5, fontWeight: 500 }}>{t.title}</TableCell>
                    <TableCell sx={{ fontSize: 13.5 }}>{t.assignedToUsername}</TableCell>
                    <TableCell sx={{ fontSize: 13.5 }}>{t.assignedByUsername}</TableCell>
                    <TableCell>
                      <Chip
                        label={t.status.replace("_", " ")}
                        size="small"
                        sx={{
                          fontFamily: '"IBM Plex Mono", monospace',
                          fontSize: 10.5,
                          fontWeight: 600,
                          bgcolor: statusStyles[t.status].bg,
                          color: statusStyles[t.status].color,
                        }}
                      />
                    </TableCell>
                    <TableCell sx={{ fontSize: 13.5 }}>{t.endDate}</TableCell>
                    <TableCell align="right">{renderActions(t)}</TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </TableContainer>

        <TablePagination
          component="div"
          count={totalElements}
          page={page}
          onPageChange={(_, newPage) => setPage(newPage)}
          rowsPerPage={rowsPerPage}
          onRowsPerPageChange={(e) => { setRowsPerPage(parseInt(e.target.value, 10)); setPage(0); }}
          rowsPerPageOptions={[10, 20, 50]}
        />
      </Box>

      <Menu anchorEl={menuAnchor} open={!!menuAnchor} onClose={closeMenu}>
        <MenuItem onClick={() => { setEditOpen(true); closeMenu(); }}>Edit</MenuItem>
        <MenuItem onClick={() => { setDeleteError(null); setDeleteOpen(true); closeMenu(); }} sx={{ color: "error.main" }}>
          Delete
        </MenuItem>
      </Menu>

      <CreateTaskDialog open={isCreateOpen} onClose={() => setIsCreateOpen(false)} onCreated={() => setRefreshKey((k) => k + 1)} />

      <EditTaskDialog open={editOpen} task={selectedTask} onClose={() => setEditOpen(false)} onUpdated={() => setRefreshKey((k) => k + 1)} />

      <ConfirmDialog
        open={deleteOpen}
        title="Delete task"
        message={`Delete "${selectedTask?.title ?? "this task"}"? This cannot be undone.`}
        confirmLabel="Delete"
        confirmColor="error"
        isSubmitting={isDeleting}
        error={deleteError}
        onConfirm={handleDelete}
        onClose={() => setDeleteOpen(false)}
      />
    </Box>
  );
}
