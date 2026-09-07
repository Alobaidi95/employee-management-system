import { useEffect, useState } from "react";
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Button,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  Alert,
  CircularProgress,
  Box,
  Typography,
} from "@mui/material";
import { createTask } from "../api/tasks";
import { getAllManagers, getUserByUsername, getEmployeesInDepartment } from "../api/users";
import { useAuth } from "../context/AuthContext";
import type { User } from "../types/user";

interface CreateTaskDialogProps {
  open: boolean;
  onClose: () => void;
  onCreated: () => void;
}

const emptyForm = {
  title: "",
  description: "",
  assignedToId: "" as number | "",
  startDate: "",
  endDate: "",
};

export default function CreateTaskDialog({ open, onClose, onCreated }: CreateTaskDialogProps) {
  const { user: currentUser } = useAuth();
  const [form, setForm] = useState(emptyForm);
  const [assignees, setAssignees] = useState<User[]>([]);
  const [isLoadingAssignees, setIsLoadingAssignees] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!open || !currentUser) return;
    setForm(emptyForm);
    setError(null);
    setIsLoadingAssignees(true);

    // ADMIN can only assign to MANAGERs; MANAGER can only assign to
    // EMPLOYEEs in their own department - mirrors the exact rule
    // enforced in TaskServiceImpl.createTask(), so the dropdown never
    // offers a choice the backend would reject anyway
    const loadAssignees =
      currentUser.role === "ADMIN"
        ? getAllManagers()
        : getUserByUsername(currentUser.username).then((profile) =>
            profile.departmentName ? getEmployeesInDepartment(profile.departmentName) : []
          );

    loadAssignees
      .then(setAssignees)
      .catch(() => setAssignees([]))
      .finally(() => setIsLoadingAssignees(false));
  }, [open, currentUser]);

  function updateField<K extends keyof typeof form>(field: K, value: (typeof form)[K]) {
    setForm((prev) => ({ ...prev, [field]: value }));
  }

  async function handleSubmit() {
    if (form.assignedToId === "") return;
    setError(null);
    setIsSubmitting(true);

    try {
      await createTask({ ...form, assignedToId: form.assignedToId });
      onCreated();
      onClose();
    } catch (err: any) {
      // Covers both role-mismatch rejections and "End date cannot be
      // before start date" from InvalidTaskStateException
      setError(err?.response?.data?.message ?? "Something went wrong creating the task.");
    } finally {
      setIsSubmitting(false);
    }
  }

  const assigneeLabel = currentUser?.role === "ADMIN" ? "Manager" : "Employee";

  return (
    <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle sx={{ fontWeight: 600 }}>New task</DialogTitle>
      <DialogContent>
        <Box sx={{ display: "flex", flexDirection: "column", gap: 2, pt: 1 }}>
          <TextField label="Title" fullWidth value={form.title} onChange={(e) => updateField("title", e.target.value)} required autoFocus />
          <TextField
            label="Description"
            fullWidth
            multiline
            rows={2}
            value={form.description}
            onChange={(e) => updateField("description", e.target.value)}
            required
          />

          {isLoadingAssignees ? (
            <Box sx={{ display: "flex", justifyContent: "center", py: 1 }}>
              <CircularProgress size={20} />
            </Box>
          ) : assignees.length === 0 ? (
            <Typography sx={{ fontSize: 13, color: "text.secondary" }}>
              {currentUser?.role === "ADMIN"
                ? "No users currently hold the MANAGER role to assign a task to."
                : "No employees found in your department, or you have no department assigned."}
            </Typography>
          ) : (
            <FormControl fullWidth>
              <InputLabel id="assignee-label">{assigneeLabel}</InputLabel>
              <Select
                labelId="assignee-label"
                label={assigneeLabel}
                value={form.assignedToId}
                onChange={(e) => updateField("assignedToId", e.target.value as number)}
              >
                {assignees.map((a) => (
                  <MenuItem key={a.id} value={a.id}>
                    {a.firstName} {a.lastName} ({a.username})
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          )}

          <Box sx={{ display: "flex", gap: 2 }}>
            <TextField
              label="Start date"
              type="date"
              fullWidth
              value={form.startDate}
              onChange={(e) => updateField("startDate", e.target.value)}
              slotProps={{ inputLabel: { shrink: true } }}
              required
            />
            <TextField
              label="End date"
              type="date"
              fullWidth
              value={form.endDate}
              onChange={(e) => updateField("endDate", e.target.value)}
              slotProps={{ inputLabel: { shrink: true } }}
              required
            />
          </Box>

          {error && <Alert severity="error">{error}</Alert>}
        </Box>
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2.5 }}>
        <Button onClick={onClose} disabled={isSubmitting}>Cancel</Button>
        <Button
          variant="contained"
          onClick={handleSubmit}
          disabled={isSubmitting || form.assignedToId === "" || !form.title || !form.startDate || !form.endDate}
          sx={{ textTransform: "none", fontWeight: 600 }}
        >
          {isSubmitting ? <CircularProgress size={20} sx={{ color: "#fff" }} /> : "Create task"}
        </Button>
      </DialogActions>
    </Dialog>
  );
}
