import { clearSession, getSession } from "./session";
import type {
  Employee,
  EmployeeFilters,
  EmployeeListParams,
  EmployeeRequest,
  ExportFormat,
  FieldError,
  PageResponse,
  ProblemDetail,
  Session,
} from "./types";

const LOGIN_PATH = "/auth/login";
const GENERIC_ERROR = "Something went wrong. Please try again.";

export class ApiError extends Error {
  readonly status: number;
  readonly title: string;
  readonly detail: string;
  readonly fieldErrors: FieldError[];

  constructor(status: number, title: string, detail: string, fieldErrors: FieldError[] = []) {
    super(detail);
    this.name = "ApiError";
    this.status = status;
    this.title = title;
    this.detail = detail;
    this.fieldErrors = fieldErrors;
  }
}

async function toApiError(response: Response): Promise<ApiError> {
  if (response.status === 403) {
    return new ApiError(403, "Forbidden", "You don't have permission to do that.");
  }
  const problem: Partial<ProblemDetail> | null = await response.json().catch(() => null);
  return new ApiError(
    response.status,
    problem?.title ?? response.statusText,
    problem?.detail ?? GENERIC_ERROR,
    problem?.fieldErrors,
  );
}

export function errorMessage(error: unknown): string {
  return error instanceof ApiError ? error.detail : GENERIC_ERROR;
}

async function apiFetch(path: string, init: RequestInit = {}): Promise<Response> {
  const headers = new Headers(init.headers);
  headers.set("Accept", "application/json, application/problem+json");
  if (init.body) headers.set("Content-Type", "application/json");
  const session = getSession();
  if (session) headers.set("Authorization", `Bearer ${session.token}`);

  let response: Response;
  try {
    response = await fetch(`/api${path}`, { ...init, headers });
  } catch (error) {
    if (error instanceof DOMException && error.name === "AbortError") throw error;
    throw new ApiError(0, "Network error", "Could not reach the server. Check your connection and try again.");
  }
  if (response.ok) return response;

  // A failed login means a wrong password, not an expired session.
  if (response.status === 401 && path !== LOGIN_PATH) {
    clearSession();
    window.location.replace("/login");
  }
  throw await toApiError(response);
}

function toQueryString(params: Record<string, string | number | undefined>): string {
  const query = new URLSearchParams();
  for (const [name, value] of Object.entries(params)) {
    if (value !== undefined) query.set(name, String(value));
  }
  return `?${query}`;
}

async function json<T>(response: Response): Promise<T> {
  return (await response.json()) as T;
}

export async function login(email: string, password: string): Promise<Session> {
  const response = await apiFetch(LOGIN_PATH, {
    method: "POST",
    body: JSON.stringify({ email, password }),
  });
  return json<Session>(response);
}

export async function listEmployees(
  params: EmployeeListParams,
  signal?: AbortSignal,
): Promise<PageResponse<Employee>> {
  const response = await apiFetch(`/employees${toQueryString(params)}`, { signal });
  return json<PageResponse<Employee>>(response);
}

export async function getEmployee(id: number): Promise<Employee> {
  return json<Employee>(await apiFetch(`/employees/${id}`));
}

export async function createEmployee(request: EmployeeRequest): Promise<Employee> {
  const response = await apiFetch("/employees", { method: "POST", body: JSON.stringify(request) });
  return json<Employee>(response);
}

export async function updateEmployee(id: number, request: EmployeeRequest): Promise<Employee> {
  const response = await apiFetch(`/employees/${id}`, { method: "PUT", body: JSON.stringify(request) });
  return json<Employee>(response);
}

export async function deleteEmployee(id: number): Promise<void> {
  await apiFetch(`/employees/${id}`, { method: "DELETE" });
}

export async function exportEmployees(format: ExportFormat, filters: EmployeeFilters): Promise<Blob> {
  const response = await apiFetch(`/employees/export${toQueryString({ ...filters, format })}`);
  return response.blob();
}
