import { Dialog, DialogTitle, DialogContent, DialogContentText, DialogActions, Button, Alert } from "@mui/material";

interface ConfirmDialogProps {
  open: boolean;
  title: string;
  message: string;
  confirmLabel?: string;
  confirmColor?: "error" | "primary";
  isSubmitting?: boolean;
  error?: string | null;
  onConfirm: () => void;
  onClose: () => void;
}

// Shared across every "are you sure?" action (delete user, delete
// department, remove manager) so we're not rebuilding the same dialog
// shape repeatedly with slightly different wording each time.
export default function ConfirmDialog({
  open,
  title,
  message,
  confirmLabel = "Confirm",
  confirmColor = "error",
  isSubmitting = false,
  error = null,
  onConfirm,
  onClose,
}: ConfirmDialogProps) {
  return (
    <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle sx={{ fontWeight: 600 }}>{title}</DialogTitle>
      <DialogContent>
        <DialogContentText sx={{ fontSize: 14 }}>{message}</DialogContentText>
        {error && (
          <Alert severity="error" sx={{ mt: 2 }}>
            {error}
          </Alert>
        )}
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2.5 }}>
        <Button onClick={onClose} disabled={isSubmitting}>
          Cancel
        </Button>
        <Button
          variant="contained"
          color={confirmColor}
          onClick={onConfirm}
          disabled={isSubmitting}
          sx={{ textTransform: "none", fontWeight: 600 }}
        >
          {confirmLabel}
        </Button>
      </DialogActions>
    </Dialog>
  );
}
