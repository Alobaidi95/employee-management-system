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
import { assignUserDepartment } from "../api/users";
import { getAllDepartments } from "../api/departments";
import type { User } from "../types/user";
import type { Department } from "../types/department";

interface AssignUserDepartmentDialogProps {
  open: boolean;
  user: User | null;
  onClose: () => void;
  onUpdated: () => void;
}

export default function AssignUserDepartmentDialog({ open, user, onClose, onUpdated }: AssignUserDepartmentDialogProps) {
  const [departments, setDepartments] = useState<Department[]>([]);
  const [departmentId, setDepartmentId] = useState<number | "">("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!open || !user) return;
    setError(null);
    setDepartmentId("");
    getAllDepartments().then(setDepartments).catch(() => setDepartments([]));
  }, [open, user]);

  async function handleSubmit() {
    if (!user || departmentId === "") return;
    setError(null);
    setIsSubmitting(true);

    try {
      await assignUserDepartment(user.id, { departmentId });
      onUpdated();
      onClose();
    } catch (err: any) {
      setError(err?.response?.data?.message ?? "Something went wrong assigning this department.");
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle sx={{ fontWeight: 600 }}>Assign department</DialogTitle>
      <DialogContent>
        <Box sx={{ pt: 1 }}>
          <FormControl fullWidth>
            <InputLabel id="assign-dept-label">Department</InputLabel>
            <Select
              labelId="assign-dept-label"
              label="Department"
              value={departmentId}
              onChange={(e) => setDepartmentId(e.target.value as number)}
            >
              {departments.map((d) => (
                <MenuItem key={d.id} value={d.id}>
                  {d.name}
                </MenuItem>
              ))}
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
        <Button
          variant="contained"
          onClick={handleSubmit}
          disabled={isSubmitting || departmentId === ""}
          sx={{ textTransform: "none", fontWeight: 600 }}
        >
          {isSubmitting ? <CircularProgress size={20} sx={{ color: "#fff" }} /> : "Assign"}
        </Button>
      </DialogActions>
    </Dialog>
  );
}
