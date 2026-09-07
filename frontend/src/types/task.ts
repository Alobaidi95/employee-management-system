export type Status = "ASSIGNED" | "STARTED" | "UNDER_REVIEW" | "DONE";

// Matches TaskResponse.java
export interface Task {
  id: number;
  title: string;
  description: string;
  status: Status;
  assignedToId: number;
  assignedToUsername: string;
  assignedById: number;
  assignedByUsername: string;
  startDate: string; // "YYYY-MM-DD" - LocalDate serializes as a plain date string
  endDate: string;
}

// Matches CreateTaskRequest.java
export interface CreateTaskRequest {
  title: string;
  description: string;
  assignedToId: number;
  startDate: string;
  endDate: string;
}

// Matches UpdateTaskRequest.java
export interface UpdateTaskRequest {
  title: string;
  description: string;
  startDate: string;
  endDate: string;
}