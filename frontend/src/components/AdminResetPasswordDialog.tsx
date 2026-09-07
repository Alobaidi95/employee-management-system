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
  Typography,
} from "@mui/material";
import { changePassword } from "../api/users";
import type { User } from "../types/user";

interface AdminResetPasswordDialogProps {
  open: boolean;
  user: User | null; // the user whose password is being reset
  onClose: () => void;
}

// Distinct from ChangePasswordDialog: this is an admin resetting SOMEONE
// ELSE's password. Your backend's UserServiceImpl.updatePassword() only
// checks the old password when isSelf is true - an admin resetting a
// different user's password skips that check entirely, so there's no
// "current password" field here at all, matching that rule exactly.
export default function AdminResetPasswordDialog({ open, user, onClose }: AdminResetPasswordDialogProps) {
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [succeeded, setSucceeded] = useState(false);

  useEffect(() => {
    if (!open) return;
    setNewPassword("");
    setConfirmPassword("");
    setError(null);
    setSucceeded(false);
  }, [open]);

  async function handleSubmit() {
    if (!user) return;

    if (newPassword !== confirmPassword) {
      setError("New password and confirmation don't match.");
      return;
    }

    setError(null);
    setIsSubmitting(true);

    try {
      // oldPassword is required by the DTO's shape but ignored server-side
      // whenever the caller is an admin resetting someone else's password
      await changePassword(user.id, { oldPassword: "", newPassword });
      setSucceeded(true);
    } catch (err: any) {
      setError(err?.response?.data?.message ?? "Something went wrong resetting this password.");
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle sx={{ fontWeight: 600 }}>Reset password</DialogTitle>
      <DialogContent>
        {succeeded ? (
          <Alert severity="success" sx={{ mt: 1 }}>
            {user?.username}'s password has been reset.
          </Alert>
        ) : (
          <Box sx={{ display: "flex", flexDirection: "column", gap: 2, pt: 1 }}>
            <Typography sx={{ fontSize: 13, color: "text.secondary" }}>
              Setting a new password for <strong>{user?.username}</strong>. They won't be asked to confirm their old one.
            </Typography>
            <TextField
              label="New password"
              type="password"
              fullWidth
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              required
              autoFocus
            />
            <TextField
              label="Confirm new password"
              type="password"
              fullWidth
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              required
            />
            {error && <Alert severity="error">{error}</Alert>}
          </Box>
        )}
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2.5 }}>
        {succeeded ? (
          <Button variant="contained" onClick={onClose} sx={{ textTransform: "none", fontWeight: 600 }}>
            Done
          </Button>
        ) : (
          <>
            <Button onClick={onClose} disabled={isSubmitting}>Cancel</Button>
            <Button
              variant="contained"
              onClick={handleSubmit}
              disabled={isSubmitting || !newPassword || !confirmPassword}
              sx={{ textTransform: "none", fontWeight: 600 }}
            >
              {isSubmitting ? <CircularProgress size={20} sx={{ color: "#fff" }} /> : "Reset password"}
            </Button>
          </>
        )}
      </DialogActions>
    </Dialog>
  );
}
