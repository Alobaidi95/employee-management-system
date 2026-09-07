import { useEffect, useState } from "react";
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Button,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  Alert,
  CircularProgress,
  Box,
} from "@mui/material";
import { createUser } from "../api/users";
import { getAllDepartments } from "../api/departments";
import type { Role } from "../types/auth";
import type { Department } from "../types/department";

interface CreateUserDialogProps {
  open: boolean;
  onClose: () => void;
  onCreated: () => void; // called after a successful create, so the parent can refresh its list
}

const emptyForm = {
  username: "",
  password: "",
  email: "",
  firstName: "",
  lastName: "",
  role: "EMPLOYEE" as Role,
  departmentId: "" as number | "", // "" means "no department selected"
};

export default function CreateUserDialog({ open, onClose, onCreated }: CreateUserDialogProps) {
  const [form, setForm] = useState(emptyForm);
  const [departments, setDepartments] = useState<Department[]>([]);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Load departments for the dropdown each time the dialog opens - and
  // reset the form, so reopening after a successful create (or a cancel)
  // doesn't show stale data from last time
  useEffect(() => {
    if (!open) return;
    setForm(emptyForm);
    setError(null);
    getAllDepartments()
      .then(setDepartments)
      .catch(() => setDepartments([])); // non-fatal - dialog still works without a department list
  }, [open]);

  function updateField<K extends keyof typeof form>(field: K, value: (typeof form)[K]) {
    setForm((prev) => ({ ...prev, [field]: value }));
  }

  async function handleSubmit() {
    setError(null);
    setIsSubmitting(true);

    try {
      await createUser({
        ...form,
        departmentId: form.departmentId === "" ? null : form.departmentId,
      });
      onCreated();
      onClose();
    } catch (err: any) {
      // Your GlobalExceptionHandler puts a human-readable message in
      // response.data.message for both validation errors (400) and
      // duplicate username/email errors (400) - so this covers both.
      setError(err?.response?.data?.message ?? "Something went wrong creating the user.");
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle sx={{ fontWeight: 600 }}>Add user</DialogTitle>

      <DialogContent>
        <Box sx={{ display: "flex", flexDirection: "column", gap: 2, pt: 1 }}>
          <Box sx={{ display: "flex", gap: 2 }}>
            <TextField
              label="First name"
              fullWidth
              value={form.firstName}
              onChange={(e) => updateField("firstName", e.target.value)}
              required
            />
            <TextField
              label="Last name"
              fullWidth
              value={form.lastName}
              onChange={(e) => updateField("lastName", e.target.value)}
              required
            />
          </Box>

          <TextField
            label="Username"
            fullWidth
            value={form.username}
            onChange={(e) => updateField("username", e.target.value)}
            required
          />

          <TextField
            label="Email"
            type="email"
            fullWidth
            value={form.email}
            onChange={(e) => updateField("email", e.target.value)}
            required
          />

          <TextField
            label="Password"
            type="password"
            fullWidth
            value={form.password}
            onChange={(e) => updateField("password", e.target.value)}
            required
          />

          <FormControl fullWidth>
            <InputLabel id="role-label">Role</InputLabel>
            <Select
              labelId="role-label"
              label="Role"
              value={form.role}
              onChange={(e) => updateField("role", e.target.value as Role)}
            >
              <MenuItem value="EMPLOYEE">Employee</MenuItem>
              <MenuItem value="MANAGER">Manager</MenuItem>
              <MenuItem value="ADMIN">Admin</MenuItem>
            </Select>
          </FormControl>

          <FormControl fullWidth>
            <InputLabel id="department-label">Department (optional)</InputLabel>
            <Select
              labelId="department-label"
              label="Department (optional)"
              value={form.departmentId}
              onChange={(e) => updateField("departmentId", e.target.value as number | "")}
            >
              <MenuItem value="">
                <em>None</em>
              </MenuItem>
              {departments.map((d) => (
                <MenuItem key={d.id} value={d.id}>
                  {d.name}
                </MenuItem>
              ))}
            </Select>
          </FormControl>

          {error && <Alert severity="error">{error}</Alert>}
        </Box>
      </DialogContent>

      <DialogActions sx={{ px: 3, pb: 2.5 }}>
        <Button onClick={onClose} disabled={isSubmitting}>
          Cancel
        </Button>
        <Button
          variant="contained"
          onClick={handleSubmit}
          disabled={isSubmitting}
          sx={{ textTransform: "none", fontWeight: 600 }}
        >
          {isSubmitting ? <CircularProgress size={20} sx={{ color: "#fff" }} /> : "Create user"}
        </Button>
      </DialogActions>
    </Dialog>
  );
}
