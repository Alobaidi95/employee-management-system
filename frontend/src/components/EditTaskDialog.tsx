import { useEffect, useState } from "react";
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Button,
  Alert,
  CircularProgress,
  Box,
} from "@mui/material";
import { updateTask } from "../api/tasks";
import type { Task } from "../types/task";

interface EditTaskDialogProps {
  open: boolean;
  task: Task | null;
  onClose: () => void;
  onUpdated: () => void;
}

export default function EditTaskDialog({ open, task, onClose, onUpdated }: EditTaskDialogProps) {
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!task) return;
    setTitle(task.title);
    setDescription(task.description);
    setStartDate(task.startDate);
    setEndDate(task.endDate);
    setError(null);
  }, [task]);

  async function handleSubmit() {
    if (!task) return;
    setError(null);
    setIsSubmitting(true);

    try {
      await updateTask(task.id, { title, description, startDate, endDate });
      onUpdated();
      onClose();
    } catch (err: any) {
      setError(err?.response?.data?.message ?? "Something went wrong updating this task.");
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle sx={{ fontWeight: 600 }}>Edit task</DialogTitle>
      <DialogContent>
        <Box sx={{ display: "flex", flexDirection: "column", gap: 2, pt: 1 }}>
          <TextField label="Title" fullWidth value={title} onChange={(e) => setTitle(e.target.value)} required />
          <TextField label="Description" fullWidth multiline rows={2} value={description} onChange={(e) => setDescription(e.target.value)} required />
          <Box sx={{ display: "flex", gap: 2 }}>
            <TextField
              label="Start date"
              type="date"
              fullWidth
              value={startDate}
              onChange={(e) => setStartDate(e.target.value)}
              slotProps={{ inputLabel: { shrink: true } }}
              required
            />
            <TextField
              label="End date"
              type="date"
              fullWidth
              value={endDate}
              onChange={(e) => setEndDate(e.target.value)}
              slotProps={{ inputLabel: { shrink: true } }}
              required
            />
          </Box>
          {error && <Alert severity="error">{error}</Alert>}
        </Box>
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2.5 }}>
        <Button onClick={onClose} disabled={isSubmitting}>Cancel</Button>
        <Button variant="contained" onClick={handleSubmit} disabled={isSubmitting} sx={{ textTransform: "none", fontWeight: 600 }}>
          {isSubmitting ? <CircularProgress size={20} sx={{ color: "#fff" }} /> : "Save changes"}
        </Button>
      </DialogActions>
    </Dialog>
  );
}
