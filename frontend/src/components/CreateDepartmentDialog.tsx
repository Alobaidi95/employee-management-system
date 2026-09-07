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
import { createDepartment } from "../api/departments";

interface CreateDepartmentDialogProps {
  open: boolean;
  onClose: () => void;
  onCreated: () => void;
}

export default function CreateDepartmentDialog({ open, onClose, onCreated }: CreateDepartmentDialogProps) {
  const [name, setName] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!open) return;
    setName("");
    setError(null);
  }, [open]);

  async function handleSubmit() {
    setError(null);
    setIsSubmitting(true);

    try {
      await createDepartment({ name });
      onCreated();
      onClose();
    } catch (err: any) {
      // Surfaces your backend's actual message, e.g. "Department name
      // already exists" from DuplicatedException
      setError(err?.response?.data?.message ?? "Something went wrong creating the department.");
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle sx={{ fontWeight: 600 }}>Add department</DialogTitle>

      <DialogContent>
        <Box sx={{ pt: 1 }}>
          <TextField
            label="Department name"
            fullWidth
            value={name}
            onChange={(e) => setName(e.target.value)}
            required
            autoFocus
          />

          {error && (
            <Alert severity="error" sx={{ mt: 2 }}>
              {error}
            </Alert>
          )}
        </Box>
      </DialogContent>

      <DialogActions sx={{ px: 3, pb: 2.5 }}>
        <Button onClick={onClose} disabled={isSubmitting}>
          Cancel
        </Button>
        <Button
          variant="contained"
          onClick={handleSubmit}
          disabled={isSubmitting || !name.trim()}
          sx={{ textTransform: "none", fontWeight: 600 }}
        >
          {isSubmitting ? <CircularProgress size={20} sx={{ color: "#fff" }} /> : "Create department"}
        </Button>
      </DialogActions>
    </Dialog>
  );
}
