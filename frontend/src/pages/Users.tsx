import { useEffect, useState } from "react";
import {
  Box,
  Typography,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TablePagination,
  Chip,
  Button,
  CircularProgress,
  Alert,
  IconButton,
  Menu,
  MenuItem,
  TextField,
  InputAdornment,
} from "@mui/material";
import AddIcon from "@mui/icons-material/Add";
import MoreVertIcon from "@mui/icons-material/MoreVert";
import SearchIcon from "@mui/icons-material/Search";
import { getUsers, deleteUser } from "../api/users";
import type { User } from "../types/user";
import { useAuth } from "../context/AuthContext";
import { colors } from "../theme/theme";
import CreateUserDialog from "../components/CreateUserDialog";
import EditUserDialog from "../components/EditUserDialog";
import ChangeRoleDialog from "../components/ChangeRoleDialog";
import AssignUserDepartmentDialog from "../components/AssignUserDepartmentDialog";
import ConfirmDialog from "../components/ConfirmDialog";
import AdminResetPasswordDialog from "../components/AdminResetPasswordDialog";

export default function Users() {
  const { user: currentUser } = useAuth();
  // Fetches a larger batch once and filters/paginates entirely client-side.
  // CAVEAT: only searches within the first 100 users - fine at current
  // scale, but past that this silently stops covering the rest. A real
  // fix at scale means a backend ?search= query param, not a bigger
  // number here.
  const [allUsers, setAllUsers] = useState<User[]>([]);
  const [searchTerm, setSearchTerm] = useState("");
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(10);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [refreshKey, setRefreshKey] = useState(0);

  const [isCreateOpen, setIsCreateOpen] = useState(false);

  // Which user the actions menu / dialogs are currently targeting
  const [menuAnchor, setMenuAnchor] = useState<HTMLElement | null>(null);
  const [selectedUser, setSelectedUser] = useState<User | null>(null);

  const [editOpen, setEditOpen] = useState(false);
  const [roleOpen, setRoleOpen] = useState(false);
  const [deptOpen, setDeptOpen] = useState(false);
  const [resetPasswordOpen, setResetPasswordOpen] = useState(false);
  const [deleteOpen, setDeleteOpen] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    async function loadUsers() {
      setIsLoading(true);
      setError(null);
      try {
        const data = await getUsers(0, 100);
        if (!cancelled) setAllUsers(data.content);
      } catch {
        if (!cancelled) setError("Couldn't load users. Try refreshing the page.");
      } finally {
        if (!cancelled) setIsLoading(false);
      }
    }

    loadUsers();
    return () => {
      cancelled = true;
    };
  }, [refreshKey]);

  const filteredUsers = allUsers.filter((u) => {
    const term = searchTerm.trim().toLowerCase();
    if (!term) return true;
    return (
      u.username.toLowerCase().includes(term) ||
      u.firstName.toLowerCase().includes(term) ||
      u.lastName.toLowerCase().includes(term) ||
      u.email.toLowerCase().includes(term)
    );
  });

  const users = filteredUsers.slice(page * rowsPerPage, page * rowsPerPage + rowsPerPage);
  const totalElements = filteredUsers.length;

  useEffect(() => setPage(0), [searchTerm]);

  const isAdmin = currentUser?.role === "ADMIN";
  // Managers only see the actions column to edit their own department's
  // employees - the backend already scopes GET /users to their own
  // department for them, so every row here already qualifies.
  const canManageUsers = isAdmin || currentUser?.role === "MANAGER";

  function openMenu(e: React.MouseEvent<HTMLElement>, u: User) {
    setMenuAnchor(e.currentTarget);
    setSelectedUser(u);
  }

  function closeMenu() {
    setMenuAnchor(null);
  }

  async function handleDelete() {
    if (!selectedUser) return;
    setDeleteError(null);
    setIsDeleting(true);
    try {
      await deleteUser(selectedUser.id);
      setDeleteOpen(false);
      setRefreshKey((k) => k + 1);
    } catch (err: any) {
      // e.g. "Cannot delete user: they are currently heading department
      // 'Engineering'. Remove them as manager first." - a real backend
      // rule, worth showing verbatim rather than a generic message
      setDeleteError(err?.response?.data?.message ?? "Couldn't delete this user.");
    } finally {
      setIsDeleting(false);
    }
  }

  return (
    <Box sx={{ p: 4 }}>
      <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "flex-end", mb: 3 }}>
        <Box>
          <Typography sx={{ fontFamily: '"IBM Plex Mono", monospace', fontSize: 11, letterSpacing: "0.14em", textTransform: "uppercase", color: colors.textFaint, mb: 0.5 }}>
            Directory
          </Typography>
          <Typography variant="h5" sx={{ fontWeight: 600, color: colors.textDark }}>
            Users
          </Typography>
        </Box>

        {isAdmin && (
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={() => setIsCreateOpen(true)}
            sx={{ bgcolor: colors.primary, textTransform: "none", fontWeight: 600, "&:hover": { bgcolor: colors.primaryHover } }}
          >
            Add user
          </Button>
        )}
      </Box>

      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

      <TextField
        placeholder="Search by username, name, or email"
        size="small"
        value={searchTerm}
        onChange={(e) => setSearchTerm(e.target.value)}
        sx={{ mb: 2, width: 340, bgcolor: "#fff" }}
        slotProps={{
          input: {
            startAdornment: (
              <InputAdornment position="start">
                <SearchIcon fontSize="small" sx={{ color: colors.textFaint }} />
              </InputAdornment>
            ),
          },
        }}
      />

      <Box sx={{ bgcolor: "#fff", border: `1px solid ${colors.border}`, borderRadius: 2, overflow: "hidden" }}>
        <TableContainer>
          <Table>
            <TableHead>
              <TableRow sx={{ bgcolor: "#FAFAFC" }}>
                <TableCell sx={{ fontWeight: 600, fontSize: 12.5, color: colors.textMuted }}>Username</TableCell>
                <TableCell sx={{ fontWeight: 600, fontSize: 12.5, color: colors.textMuted }}>Name</TableCell>
                <TableCell sx={{ fontWeight: 600, fontSize: 12.5, color: colors.textMuted }}>Email</TableCell>
                <TableCell sx={{ fontWeight: 600, fontSize: 12.5, color: colors.textMuted }}>Role</TableCell>
                <TableCell sx={{ fontWeight: 600, fontSize: 12.5, color: colors.textMuted }}>Department</TableCell>
                {canManageUsers && <TableCell align="right" sx={{ fontWeight: 600, fontSize: 12.5, color: colors.textMuted }}>Actions</TableCell>}
              </TableRow>
            </TableHead>
            <TableBody>
              {isLoading ? (
                <TableRow>
                  <TableCell colSpan={canManageUsers ? 6 : 5} align="center" sx={{ py: 6 }}>
                    <CircularProgress size={28} />
                  </TableCell>
                </TableRow>
              ) : users.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={canManageUsers ? 6 : 5} align="center" sx={{ py: 6, color: colors.textMuted }}>
                    {searchTerm ? "No users match your search." : "No users found."}
                  </TableCell>
                </TableRow>
              ) : (
                users.map((u) => (
                  <TableRow key={u.id} hover>
                    <TableCell sx={{ fontSize: 13.5 }}>{u.username}</TableCell>
                    <TableCell sx={{ fontSize: 13.5 }}>{u.firstName} {u.lastName}</TableCell>
                    <TableCell sx={{ fontSize: 13.5 }}>{u.email}</TableCell>
                    <TableCell>
                      <Chip label={u.role} size="small" sx={{ fontFamily: '"IBM Plex Mono", monospace', fontSize: 10.5, fontWeight: 600, bgcolor: "rgba(74,95,232,0.1)", color: colors.primary }} />
                    </TableCell>
                    <TableCell sx={{ fontSize: 13.5, color: u.departmentName ? colors.textDark : colors.textFaint }}>
                      {u.departmentName ?? "Unassigned"}
                    </TableCell>
                    {canManageUsers && (
                      <TableCell align="right">
                        <IconButton size="small" onClick={(e) => openMenu(e, u)}>
                          <MoreVertIcon fontSize="small" />
                        </IconButton>
                      </TableCell>
                    )}
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </TableContainer>

        <TablePagination
          component="div"
          count={totalElements}
          page={page}
          onPageChange={(_, newPage) => setPage(newPage)}
          rowsPerPage={rowsPerPage}
          onRowsPerPageChange={(e) => { setRowsPerPage(parseInt(e.target.value, 10)); setPage(0); }}
          rowsPerPageOptions={[10, 20, 50]}
        />
      </Box>

      {/* Per-row actions menu */}
      <Menu anchorEl={menuAnchor} open={!!menuAnchor} onClose={closeMenu}>
        <MenuItem onClick={() => { setEditOpen(true); closeMenu(); }}>Edit details</MenuItem>
        {isAdmin && <MenuItem onClick={() => { setRoleOpen(true); closeMenu(); }}>Change role</MenuItem>}
        {isAdmin && <MenuItem onClick={() => { setDeptOpen(true); closeMenu(); }}>Assign department</MenuItem>}
        {isAdmin && <MenuItem onClick={() => { setResetPasswordOpen(true); closeMenu(); }}>Reset password</MenuItem>}
        {isAdmin && (
          <MenuItem onClick={() => { setDeleteError(null); setDeleteOpen(true); closeMenu(); }} sx={{ color: "error.main" }}>
            Delete user
          </MenuItem>
        )}
      </Menu>

      <CreateUserDialog open={isCreateOpen} onClose={() => setIsCreateOpen(false)} onCreated={() => setRefreshKey((k) => k + 1)} />

      <EditUserDialog open={editOpen} user={selectedUser} onClose={() => setEditOpen(false)} onUpdated={() => setRefreshKey((k) => k + 1)} />

      <ChangeRoleDialog open={roleOpen} user={selectedUser} onClose={() => setRoleOpen(false)} onUpdated={() => setRefreshKey((k) => k + 1)} />

      <AssignUserDepartmentDialog open={deptOpen} user={selectedUser} onClose={() => setDeptOpen(false)} onUpdated={() => setRefreshKey((k) => k + 1)} />

      <AdminResetPasswordDialog open={resetPasswordOpen} user={selectedUser} onClose={() => setResetPasswordOpen(false)} />

      <ConfirmDialog
        open={deleteOpen}
        title="Delete user"
        message={`Delete ${selectedUser?.username ?? "this user"}? This also removes every task assigned to or by them. This cannot be undone.`}
        confirmLabel="Delete"
        confirmColor="error"
        isSubmitting={isDeleting}
        error={deleteError}
        onConfirm={handleDelete}
        onClose={() => setDeleteOpen(false)}
      />
    </Box>
  );
}
