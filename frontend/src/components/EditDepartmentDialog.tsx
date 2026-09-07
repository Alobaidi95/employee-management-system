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
import { updateDepartment } from "../api/departments";
import type { Department } from "../types/department";

interface EditDepartmentDialogProps {
  open: boolean;
  department: Department | null;
  onClose: () => void;
  onUpdated: () => void;
}

export default function EditDepartmentDialog({ open, department, onClose, onUpdated }: EditDepartmentDialogProps) {
  const [name, setName] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!department) return;
    setName(department.name);
    setError(null);
  }, [department]);

  async function handleSubmit() {
    if (!department) return;
    setError(null);
    setIsSubmitting(true);

    try {
      await updateDepartment(department.id, { name });
      onUpdated();
      onClose();
    } catch (err: any) {
      setError(err?.response?.data?.message ?? "Something went wrong renaming this department.");
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle sx={{ fontWeight: 600 }}>Rename department</DialogTitle>
      <DialogContent>
        <Box sx={{ pt: 1 }}>
          <TextField label="Department name" fullWidth value={name} onChange={(e) => setName(e.target.value)} required autoFocus />
          {error && <Alert severity="error" sx={{ mt: 2 }}>{error}</Alert>}
        </Box>
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2.5 }}>
        <Button onClick={onClose} disabled={isSubmitting}>Cancel</Button>
        <Button variant="contained" onClick={handleSubmit} disabled={isSubmitting || !name.trim()} sx={{ textTransform: "none", fontWeight: 600 }}>
          {isSubmitting ? <CircularProgress size={20} sx={{ color: "#fff" }} /> : "Save"}
        </Button>
      </DialogActions>
    </Dialog>
  );
}
