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
} from "@mui/material";
import { changeRole } from "../api/users";
import type { User } from "../types/user";
import type { Role } from "../types/auth";

interface ChangeRoleDialogProps {
  open: boolean;
  user: User | null;
  onClose: () => void;
  onUpdated: () => void;
}

export default function ChangeRoleDialog({ open, user, onClose, onUpdated }: ChangeRoleDialogProps) {
  const [role, setRole] = useState<Role>("EMPLOYEE");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!user) return;
    setRole(user.role);
    setError(null);
  }, [user]);

  async function handleSubmit() {
    if (!user) return;
    setError(null);
    setIsSubmitting(true);

    try {
      await changeRole(user.id, { role });
      onUpdated();
      onClose();
    } catch (err: any) {
      // Surfaces real backend rules here, e.g. "Cannot promote user:
      // employee still has active (non-DONE) tasks" or the
      // single-manager-per-department constraint
      setError(err?.response?.data?.message ?? "Something went wrong changing this user's role.");
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle sx={{ fontWeight: 600 }}>Change role</DialogTitle>
      <DialogContent>
        <Box sx={{ pt: 1 }}>
          <FormControl fullWidth>
            <InputLabel id="change-role-label">Role</InputLabel>
            <Select
              labelId="change-role-label"
              label="Role"
              value={role}
              onChange={(e) => setRole(e.target.value as Role)}
            >
              <MenuItem value="EMPLOYEE">Employee</MenuItem>
              <MenuItem value="MANAGER">Manager</MenuItem>
              <MenuItem value="ADMIN">Admin</MenuItem>
            </Select>
          </FormControl>
          {error && (
            <Alert severity="error" sx={{ mt: 2 }}>
              {error}
            </Alert>
          )}
        </Box>
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2.5 }}>
        <Button onClick={onClose} disabled={isSubmitting}>Cancel</Button>
        <Button variant="contained" onClick={handleSubmit} disabled={isSubmitting} sx={{ textTransform: "none", fontWeight: 600 }}>
          {isSubmitting ? <CircularProgress size={20} sx={{ color: "#fff" }} /> : "Save"}
        </Button>
      </DialogActions>
    </Dialog>
  );
}
