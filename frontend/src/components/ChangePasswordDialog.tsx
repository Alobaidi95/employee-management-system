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
import { changePassword, getUserByUsername } from "../api/users";
import { useAuth } from "../context/AuthContext";

interface ChangePasswordDialogProps {
  open: boolean;
  onClose: () => void;
}

export default function ChangePasswordDialog({ open, onClose }: ChangePasswordDialogProps) {
  const { user: currentUser } = useAuth();
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [succeeded, setSucceeded] = useState(false);

  useEffect(() => {
    if (!open) return;
    setCurrentPassword("");
    setNewPassword("");
    setConfirmPassword("");
    setError(null);
    setSucceeded(false);
  }, [open]);

  async function handleSubmit() {
    if (!currentUser) return;

    if (newPassword !== confirmPassword) {
      setError("New password and confirmation don't match.");
      return;
    }

    setError(null);
    setIsSubmitting(true);

    try {
      // The JWT never carries a numeric user id (only username + role),
      // so we look up our own record first purely to get the id the
      // PATCH endpoint requires in its URL.
      const profile = await getUserByUsername(currentUser.username);
      await changePassword(profile.id, { oldPassword: currentPassword, newPassword });
      setSucceeded(true);
    } catch (err: any) {
      // Backend returns "Old passwords don't match" via UnMatchedPasswordsException
      setError(err?.response?.data?.message ?? "Something went wrong changing your password.");
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle sx={{ fontWeight: 600 }}>Change password</DialogTitle>
      <DialogContent>
        {succeeded ? (
          <Alert severity="success" sx={{ mt: 1 }}>
            Your password has been updated.
          </Alert>
        ) : (
          <Box sx={{ display: "flex", flexDirection: "column", gap: 2, pt: 1 }}>
            <TextField
              label="Current password"
              type="password"
              fullWidth
              value={currentPassword}
              onChange={(e) => setCurrentPassword(e.target.value)}
              required
              autoFocus
            />
            <TextField
              label="New password"
              type="password"
              fullWidth
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              required
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
              disabled={isSubmitting || !currentPassword || !newPassword || !confirmPassword}
              sx={{ textTransform: "none", fontWeight: 600 }}
            >
              {isSubmitting ? <CircularProgress size={20} sx={{ color: "#fff" }} /> : "Update password"}
            </Button>
          </>
        )}
      </DialogActions>
    </Dialog>
  );
}
