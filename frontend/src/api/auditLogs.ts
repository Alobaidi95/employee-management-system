import apiClient from "./client";
import type { PageResponse } from "../types/pagination";
import type { AuditLog } from "../types/auditLog";

// Matches AuditLogController's actual GET /api/audit-logs mapping,
// enforced ADMIN-only server-side via @PreAuthorize on the service method
export async function getAllLogs(page: number, size: number): Promise<PageResponse<AuditLog>> {
  const response = await apiClient.get<PageResponse<AuditLog>>("/audit-logs", {
    params: { page, size },
  });
  return response.data;
}