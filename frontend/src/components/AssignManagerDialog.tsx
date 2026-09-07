import { useEffect, useState } from "react";
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
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
import { assignManager } from "../api/departments";
import { getAllManagers } from "../api/users";
import type { Department } from "../types/department";
import type { User } from "../types/user";

interface AssignManagerDialogProps {
  open: boolean;
  department: Department | null;
  onClose: () => void;
  onUpdated: () => void;
}

export default function AssignManagerDialog({ open, department, onClose, onUpdated }: AssignManagerDialogProps) {
  const [managers, setManagers] = useState<User[]>([]);
  const [userId, setUserId] = useState<number | "">("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!open) return;
    setUserId("");
    setError(null);
    getAllManagers().then(setManagers).catch(() => setManagers([]));
  }, [open]);

  async function handleSubmit() {
    if (!department || userId === "") return;
    setError(null);
    setIsSubmitting(true);

    try {
      await assignManager(department.id, { userId });
      onUpdated();
      onClose();
    } catch (err: any) {
      // Surfaces real backend rules: single-manager-per-department
      // constraint, or "must already hold the MANAGER role"
      setError(err?.response?.data?.message ?? "Something went wrong assigning a manager.");
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle sx={{ fontWeight: 600 }}>Assign manager</DialogTitle>
      <DialogContent>
        <Box sx={{ pt: 1 }}>
          {managers.length === 0 ? (
            <Typography sx={{ fontSize: 13.5, color: "text.secondary" }}>
              No users currently hold the MANAGER role. Promote someone to MANAGER first (Users → Change role).
            </Typography>
          ) : (
            <FormControl fullWidth>
              <InputLabel id="assign-manager-label">Manager</InputLabel>
              <Select
                labelId="assign-manager-label"
                label="Manager"
                value={userId}
                onChange={(e) => setUserId(e.target.value as number)}
              >
                {managers.map((m) => (
                  <MenuItem key={m.id} value={m.id}>
                    {m.firstName} {m.lastName} ({m.username})
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          )}
          {error && (
            <Alert severity="error" sx={{ mt: 2 }}>
              {error}
            </Alert>
          )}
        </Box>
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2.5 }}>
        <Button onClick={onClose} disabled={isSubmitting}>Cancel</Button>
        <Button
          variant="contained"
          onClick={handleSubmit}
          disabled={isSubmitting || userId === "" || managers.length === 0}
          sx={{ textTransform: "none", fontWeight: 600 }}
        >
          {isSubmitting ? <CircularProgress size={20} sx={{ color: "#fff" }} /> : "Assign"}
        </Button>
      </DialogActions>
    </Dialog>
  );
}
