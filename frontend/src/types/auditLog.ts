
// Matches AuditLogResponse.java. targetType mirrors your backend's
// AuditTargetType enum (USER, DEPARTMENT, TASK).
export type TargetType = "USER" | "DEPARTMENT" | "TASK";

export interface AuditLog {
  id: number;
  action: string;
  performedById: number | null;
  performedByUsername: string | null;
  targetId: number;
  targetType: TargetType;
  details: string | null;
  timestamp: string; // ISO datetime string
}
