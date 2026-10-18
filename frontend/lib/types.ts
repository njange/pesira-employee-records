export type Role = "ADMIN" | "USER";

export interface User {
  id: number;
  email: string;
  fullName: string;
  role: Role;
}

export interface Session {
  token: string;
  expiresAt: string;
  user: User;
}

export const EMPLOYEE_STATUSES = ["ACTIVE", "ON_LEAVE", "TERMINATED"] as const;
export type EmployeeStatus = (typeof EMPLOYEE_STATUSES)[number];

export interface Employee {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  department: string;
  jobTitle: string;
  salary: number;
  hireDate: string;
  status: EmployeeStatus;
  version: number;
  createdAt: string;
  updatedAt: string;
}

export interface EmployeeRequest {
  firstName: string;
  lastName: string;
  email: string;
  department: string;
  jobTitle: string;
  salary: number;
  hireDate: string;
  status: EmployeeStatus;
  // Required on update, ignored on create.
  version: number | null;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface FieldError {
  field: string;
  message: string;
}

export interface ProblemDetail {
  type?: string;
  title: string;
  status: number;
  detail?: string;
  instance?: string;
  fieldErrors?: FieldError[];
}

export const SORT_FIELDS = [
  "firstName",
  "lastName",
  "email",
  "department",
  "jobTitle",
  "salary",
  "hireDate",
  "status",
] as const;
export type SortField = (typeof SORT_FIELDS)[number];
export type SortDirection = "asc" | "desc";

export type EmployeeFilters = {
  search?: string;
  status?: EmployeeStatus;
  sort: `${SortField},${SortDirection}`;
};

export type EmployeeListParams = EmployeeFilters & {
  page: number;
  size: number;
};

export type ExportFormat = "csv" | "json";
