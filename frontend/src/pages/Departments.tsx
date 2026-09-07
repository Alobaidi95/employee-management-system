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
} from "@mui/material";
import AddIcon from "@mui/icons-material/Add";
import MoreVertIcon from "@mui/icons-material/MoreVert";
import { getDepartments, deleteDepartment, removeManager } from "../api/departments";
import type { Department } from "../types/department";
import { useAuth } from "../context/AuthContext";
import { colors } from "../theme/theme";
import CreateDepartmentDialog from "../components/CreateDepartmentDialog";
import EditDepartmentDialog from "../components/EditDepartmentDialog";
import AssignManagerDialog from "../components/AssignManagerDialog";
import ConfirmDialog from "../components/ConfirmDialog";

export default function Departments() {
  const { user } = useAuth();
  const [departments, setDepartments] = useState<Department[]>([]);
  const [totalElements, setTotalElements] = useState(0);
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(10);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [refreshKey, setRefreshKey] = useState(0);

  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [menuAnchor, setMenuAnchor] = useState<HTMLElement | null>(null);
  const [selectedDept, setSelectedDept] = useState<Department | null>(null);

  const [editOpen, setEditOpen] = useState(false);
  const [assignManagerOpen, setAssignManagerOpen] = useState(false);
  const [deleteOpen, setDeleteOpen] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState<string | null>(null);
  const [removeManagerOpen, setRemoveManagerOpen] = useState(false);
  const [isRemovingManager, setIsRemovingManager] = useState(false);
  const [removeManagerError, setRemoveManagerError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    async function loadDepartments() {
      setIsLoading(true);
      setError(null);
      try {
        const data = await getDepartments(page, rowsPerPage);
        if (!cancelled) {
          setDepartments(data.content);
          setTotalElements(data.totalElements);
        }
      } catch {
        if (!cancelled) setError("Couldn't load departments. Try refreshing the page.");
      } finally {
        if (!cancelled) setIsLoading(false);
      }
    }

    loadDepartments();
    return () => {
      cancelled = true;
    };
  }, [page, rowsPerPage, refreshKey]);

  const isAdmin = user?.role === "ADMIN";

  function openMenu(e: React.MouseEvent<HTMLElement>, d: Department) {
    setMenuAnchor(e.currentTarget);
    setSelectedDept(d);
  }
  function closeMenu() {
    setMenuAnchor(null);
  }

  async function handleDelete() {
    if (!selectedDept) return;
    setDeleteError(null);
    setIsDeleting(true);
    try {
      await deleteDepartment(selectedDept.id);
      setDeleteOpen(false);
      setRefreshKey((k) => k + 1);
    } catch (err: any) {
      // e.g. "Cannot delete department 'Engineering': it still has 3
      // employee(s) assigned. Reassign them first."
      setDeleteError(err?.response?.data?.message ?? "Couldn't delete this department.");
    } finally {
      setIsDeleting(false);
    }
  }

  async function handleRemoveManager() {
    if (!selectedDept) return;
    setRemoveManagerError(null);
    setIsRemovingManager(true);
    try {
      await removeManager(selectedDept.id);
      setRemoveManagerOpen(false);
      setRefreshKey((k) => k + 1);
    } catch (err: any) {
      setRemoveManagerError(err?.response?.data?.message ?? "Couldn't remove the manager.");
    } finally {
      setIsRemovingManager(false);
    }
  }

  return (
    <Box sx={{ p: 4 }}>
      <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "flex-end", mb: 3 }}>
        <Box>
          <Typography sx={{ fontFamily: '"IBM Plex Mono", monospace', fontSize: 11, letterSpacing: "0.14em", textTransform: "uppercase", color: colors.textFaint, mb: 0.5 }}>
            Structure
          </Typography>
          <Typography variant="h5" sx={{ fontWeight: 600, color: colors.textDark }}>
            Departments
          </Typography>
        </Box>

        {isAdmin && (
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={() => setIsCreateOpen(true)}
            sx={{ bgcolor: colors.primary, textTransform: "none", fontWeight: 600, "&:hover": { bgcolor: colors.primaryHover } }}
          >
            Add department
          </Button>
        )}
      </Box>

      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

      <Box sx={{ bgcolor: "#fff", border: `1px solid ${colors.border}`, borderRadius: 2, overflow: "hidden" }}>
        <TableContainer>
          <Table>
            <TableHead>
              <TableRow sx={{ bgcolor: "#FAFAFC" }}>
                <TableCell sx={{ fontWeight: 600, fontSize: 12.5, color: colors.textMuted }}>Name</TableCell>
                <TableCell sx={{ fontWeight: 600, fontSize: 12.5, color: colors.textMuted }}>Manager</TableCell>
                <TableCell sx={{ fontWeight: 600, fontSize: 12.5, color: colors.textMuted }}>Employees</TableCell>
                {isAdmin && <TableCell align="right" sx={{ fontWeight: 600, fontSize: 12.5, color: colors.textMuted }}>Actions</TableCell>}
              </TableRow>
            </TableHead>
            <TableBody>
              {isLoading ? (
                <TableRow>
                  <TableCell colSpan={isAdmin ? 4 : 3} align="center" sx={{ py: 6 }}>
                    <CircularProgress size={28} />
                  </TableCell>
                </TableRow>
              ) : departments.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={isAdmin ? 4 : 3} align="center" sx={{ py: 6, color: colors.textMuted }}>
                    No departments found.
                  </TableCell>
                </TableRow>
              ) : (
                departments.map((d) => (
                  <TableRow key={d.id} hover>
                    <TableCell sx={{ fontSize: 13.5, fontWeight: 500 }}>{d.name}</TableCell>
                    <TableCell sx={{ fontSize: 13.5, color: d.managerUsername ? colors.textDark : colors.textFaint }}>
                      {d.managerUsername ?? "No manager assigned"}
                    </TableCell>
                    <TableCell>
                      <Chip label={d.employeeCount} size="small" sx={{ fontFamily: '"IBM Plex Mono", monospace', fontSize: 11, fontWeight: 600, bgcolor: "rgba(232,179,74,0.15)", color: "#9C7412" }} />
                    </TableCell>
                    {isAdmin && (
                      <TableCell align="right">
                        <IconButton size="small" onClick={(e) => openMenu(e, d)}>
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

      <Menu anchorEl={menuAnchor} open={!!menuAnchor} onClose={closeMenu}>
        <MenuItem onClick={() => { setEditOpen(true); closeMenu(); }}>Rename</MenuItem>
        <MenuItem onClick={() => { setAssignManagerOpen(true); closeMenu(); }}>Assign manager</MenuItem>
        {selectedDept?.managerUsername && (
          <MenuItem onClick={() => { setRemoveManagerError(null); setRemoveManagerOpen(true); closeMenu(); }}>
            Remove manager
          </MenuItem>
        )}
        <MenuItem onClick={() => { setDeleteError(null); setDeleteOpen(true); closeMenu(); }} sx={{ color: "error.main" }}>
          Delete department
        </MenuItem>
      </Menu>

      <CreateDepartmentDialog open={isCreateOpen} onClose={() => setIsCreateOpen(false)} onCreated={() => setRefreshKey((k) => k + 1)} />

      <EditDepartmentDialog open={editOpen} department={selectedDept} onClose={() => setEditOpen(false)} onUpdated={() => setRefreshKey((k) => k + 1)} />

      <AssignManagerDialog open={assignManagerOpen} department={selectedDept} onClose={() => setAssignManagerOpen(false)} onUpdated={() => setRefreshKey((k) => k + 1)} />

      <ConfirmDialog
        open={deleteOpen}
        title="Delete department"
        message={`Delete ${selectedDept?.name ?? "this department"}? This is blocked if any employees are still assigned to it.`}
        confirmLabel="Delete"
        confirmColor="error"
        isSubmitting={isDeleting}
        error={deleteError}
        onConfirm={handleDelete}
        onClose={() => setDeleteOpen(false)}
      />

      <ConfirmDialog
        open={removeManagerOpen}
        title="Remove manager"
        message={`Remove ${selectedDept?.managerUsername ?? "the current manager"} as manager of ${selectedDept?.name ?? "this department"}?`}
        confirmLabel="Remove"
        confirmColor="primary"
        isSubmitting={isRemovingManager}
        error={removeManagerError}
        onConfirm={handleRemoveManager}
        onClose={() => setRemoveManagerOpen(false)}
      />
    </Box>
  );
}
