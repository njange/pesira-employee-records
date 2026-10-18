import {
  EMPLOYEE_STATUSES,
  SORT_FIELDS,
  type EmployeeFilters,
  type EmployeeListParams,
  type EmployeeStatus,
  type SortDirection,
  type SortField,
} from "./types";

export interface Sort {
  field: SortField;
  direction: SortDirection;
}

// What the employees URL holds. `page` is 1-based here and 0-based in the API.
export interface TableState {
  search: string;
  status: EmployeeStatus | null;
  sort: Sort;
  page: number;
  size: number;
}

export const PAGE_SIZES = [10, 20, 50];
const DEFAULT_SIZE = 20;
const DEFAULT_SORT: Sort = { field: "lastName", direction: "asc" };

function isStatus(value: string | null): value is EmployeeStatus {
  return EMPLOYEE_STATUSES.some((status) => status === value);
}

function isSortField(value: string): value is SortField {
  return SORT_FIELDS.some((field) => field === value);
}

function parseSort(value: string | null): Sort {
  const [field = "", direction] = (value ?? "").split(",");
  if (isSortField(field) && (direction === "asc" || direction === "desc")) {
    return { field, direction };
  }
  return DEFAULT_SORT;
}

// Hand-edited URLs fall back to defaults instead of turning into API errors.
export function parseTableState(params: URLSearchParams): TableState {
  const status = params.get("status");
  const page = Number(params.get("page"));
  const size = Number(params.get("size"));
  return {
    search: params.get("search") ?? "",
    status: isStatus(status) ? status : null,
    sort: parseSort(params.get("sort")),
    page: Number.isInteger(page) && page >= 1 ? page : 1,
    size: PAGE_SIZES.includes(size) ? size : DEFAULT_SIZE,
  };
}

export function toUrlSearch(state: TableState): string {
  const params = new URLSearchParams();
  if (state.search) params.set("search", state.search);
  if (state.status) params.set("status", state.status);
  if (state.page !== 1) params.set("page", String(state.page));
  if (state.size !== DEFAULT_SIZE) params.set("size", String(state.size));
  if (state.sort.field !== DEFAULT_SORT.field || state.sort.direction !== DEFAULT_SORT.direction) {
    params.set("sort", `${state.sort.field},${state.sort.direction}`);
  }
  // Commas are valid in a query string; leaving them unescaped keeps `sort=salary,desc` readable.
  const query = params.toString().replaceAll("%2C", ",");
  return query ? `?${query}` : "";
}

// Any change other than the page itself starts again from page 1.
export function changeTableState(state: TableState, change: Partial<TableState>): TableState {
  return { ...state, page: 1, ...change };
}

export function nextSort(current: Sort, field: SortField): Sort {
  if (current.field !== field) {
    return { field, direction: "asc" };
  }
  return { field, direction: current.direction === "asc" ? "desc" : "asc" };
}

export function toApiFilters(state: TableState): EmployeeFilters {
  return {
    search: state.search.trim() || undefined,
    status: state.status ?? undefined,
    sort: `${state.sort.field},${state.sort.direction}`,
  };
}

export function toApiParams(state: TableState): EmployeeListParams {
  return { ...toApiFilters(state), page: state.page - 1, size: state.size };
}
