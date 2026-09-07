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
import { updateUser } from "../api/users";
import type { User } from "../types/user";

interface EditUserDialogProps {
  open: boolean;
  user: User | null; // the user being edited - null when the dialog is closed
  onClose: () => void;
  onUpdated: () => void;
}

export default function EditUserDialog({ open, user, onClose, onUpdated }: EditUserDialogProps) {
  const [firstName, setFirstName] = useState("");
  const [lastName, setLastName] = useState("");
  const [email, setEmail] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Pre-fill the form with the selected user's current values whenever
  // the dialog opens for a (possibly different) user
  useEffect(() => {
    if (!user) return;
    setFirstName(user.firstName);
    setLastName(user.lastName);
    setEmail(user.email);
    setError(null);
  }, [user]);

  async function handleSubmit() {
    if (!user) return;
    setError(null);
    setIsSubmitting(true);

    try {
      await updateUser(user.id, { firstName, lastName, email });
      onUpdated();
      onClose();
    } catch (err: any) {
      setError(err?.response?.data?.message ?? "Something went wrong updating this user.");
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle sx={{ fontWeight: 600 }}>Edit user</DialogTitle>
      <DialogContent>
        <Box sx={{ display: "flex", flexDirection: "column", gap: 2, pt: 1 }}>
          <Box sx={{ display: "flex", gap: 2 }}>
            <TextField label="First name" fullWidth value={firstName} onChange={(e) => setFirstName(e.target.value)} required />
            <TextField label="Last name" fullWidth value={lastName} onChange={(e) => setLastName(e.target.value)} required />
          </Box>
          <TextField label="Email" type="email" fullWidth value={email} onChange={(e) => setEmail(e.target.value)} required />
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
