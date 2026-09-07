// Matches DepartmentResponse.java
export interface Department {
  id: number;
  name: string;
  managerId: number | null;
  managerUsername: string | null;
  employeeCount: number;
}

// Matches CreateDepartmentRequest.java
export interface CreateDepartmentRequest {
  name: string;
}

// Matches UpdateDepartmentRequest.java
export interface UpdateDepartmentRequest {
  name: string;
}

// Matches AssignManagerRequest.java
export interface AssignManagerRequest {
  userId: number;
}