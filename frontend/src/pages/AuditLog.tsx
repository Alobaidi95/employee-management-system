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
  CircularProgress,
  Alert,
} from "@mui/material";
import { getAllLogs } from "../api/auditLogs";
import type { AuditLog } from "../types/auditLog";
import { colors } from "../theme/theme";

export default function AuditLogPage() {
  const [logs, setLogs] = useState<AuditLog[]>([]);
  const [totalElements, setTotalElements] = useState(0);
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(20);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    async function loadLogs() {
      setIsLoading(true);
      setError(null);
      try {
        const data = await getAllLogs(page, rowsPerPage);
        if (!cancelled) {
          setLogs(data.content);
          setTotalElements(data.totalElements);
        }
      } catch {
        if (!cancelled) setError("Couldn't load the audit log. Try refreshing the page.");
      } finally {
        if (!cancelled) setIsLoading(false);
      }
    }

    loadLogs();
    return () => {
      cancelled = true;
    };
  }, [page, rowsPerPage]);

  return (
    <Box sx={{ p: 4 }}>
      <Typography
        sx={{
          fontFamily: '"IBM Plex Mono", monospace',
          fontSize: 11,
          letterSpacing: "0.14em",
          textTransform: "uppercase",
          color: colors.textFaint,
          mb: 0.5,
        }}
      >
        Record
      </Typography>
      <Typography variant="h5" sx={{ fontWeight: 600, color: colors.textDark, mb: 3 }}>
        Audit Log
      </Typography>

      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

      <Box sx={{ bgcolor: "#fff", border: `1px solid ${colors.border}`, borderRadius: 2, overflow: "hidden" }}>
        <TableContainer>
          <Table size="small">
            <TableHead>
              <TableRow sx={{ bgcolor: "#FAFAFC" }}>
                <TableCell sx={{ fontWeight: 600, fontSize: 12.5, color: colors.textMuted }}>Timestamp</TableCell>
                <TableCell sx={{ fontWeight: 600, fontSize: 12.5, color: colors.textMuted }}>Action</TableCell>
                <TableCell sx={{ fontWeight: 600, fontSize: 12.5, color: colors.textMuted }}>Performed by</TableCell>
                <TableCell sx={{ fontWeight: 600, fontSize: 12.5, color: colors.textMuted }}>Target</TableCell>
                <TableCell sx={{ fontWeight: 600, fontSize: 12.5, color: colors.textMuted }}>Details</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {isLoading ? (
                <TableRow>
                  <TableCell colSpan={5} align="center" sx={{ py: 6 }}>
                    <CircularProgress size={28} />
                  </TableCell>
                </TableRow>
              ) : logs.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={5} align="center" sx={{ py: 6, color: colors.textMuted }}>
                    No audit log entries yet.
                  </TableCell>
                </TableRow>
              ) : (
                logs.map((log) => (
                  <TableRow key={log.id} hover>
                    <TableCell sx={{ fontSize: 12.5, fontFamily: '"IBM Plex Mono", monospace', color: colors.textMuted, whiteSpace: "nowrap" }}>
                      {new Date(log.timestamp).toLocaleString()}
                    </TableCell>
                    <TableCell>
                      <Chip
                        label={log.action}
                        size="small"
                        sx={{
                          fontFamily: '"IBM Plex Mono", monospace',
                          fontSize: 10.5,
                          fontWeight: 600,
                          bgcolor: "rgba(74,95,232,0.1)",
                          color: colors.primary,
                        }}
                      />
                    </TableCell>
                    <TableCell sx={{ fontSize: 13 }}>{log.performedByUsername ?? "—"}</TableCell>
                    <TableCell sx={{ fontSize: 13, color: colors.textMuted }}>
                      {log.targetType} #{log.targetId}
                    </TableCell>
                    <TableCell sx={{ fontSize: 13, color: colors.textMuted, maxWidth: 320 }}>{log.details ?? "—"}</TableCell>
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
          rowsPerPageOptions={[20, 50, 100]}
        />
      </Box>
    </Box>
  );
}
