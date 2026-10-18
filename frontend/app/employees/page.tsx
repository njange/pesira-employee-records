"use client";

import { Suspense, useCallback, useEffect, useMemo, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import AppHeader from "@/components/AppHeader";
import Button from "@/components/Button";
import EmployeeForm from "@/components/EmployeeForm";
import EmployeeTable from "@/components/EmployeeTable";
import Pagination from "@/components/Pagination";
import SearchBox from "@/components/SearchBox";
import SelectField from "@/components/SelectField";
import { errorMessage, listEmployees } from "@/lib/api";
import { STATUS_LABELS } from "@/lib/format";
import {
  changeTableState,
  nextSort,
  parseTableState,
  toApiParams,
  toUrlSearch,
  type TableState,
} from "@/lib/query";
import { EMPLOYEE_STATUSES, type Employee, type PageResponse, type Session } from "@/lib/types";
import { useSession } from "@/lib/useSession";
import styles from "./employees.module.css";

const STATUS_MESSAGE_MS = 5000;

// Results remember which request they answer, so a stale one is never shown as current.
interface RequestKey {
  table: TableState;
  reloads: number;
}

// The open form: `employee` is null when adding.
interface FormTarget {
  employee: Employee | null;
}

export default function EmployeesPage() {
  const session = useSession();
  const router = useRouter();

  useEffect(() => {
    if (session === null) router.replace("/login");
  }, [session, router]);

  if (!session) return null;
  return (
    <Suspense>
      <EmployeesView session={session} />
    </Suspense>
  );
}

function EmployeesView({ session }: { session: Session }) {
  const router = useRouter();
  const searchParams = useSearchParams();
  const table = useMemo(() => parseTableState(searchParams), [searchParams]);
  const [reloads, setReloads] = useState(0);
  const [data, setData] = useState<(RequestKey & { page: PageResponse<Employee> }) | null>(null);
  const [failure, setFailure] = useState<(RequestKey & { message: string }) | null>(null);
  const [formTarget, setFormTarget] = useState<FormTarget | null>(null);
  const [statusMessage, setStatusMessage] = useState("");
  const isAdmin = session.user.role === "ADMIN";

  useEffect(() => {
    const controller = new AbortController();
    listEmployees(toApiParams(table), controller.signal)
      .then((page) => setData({ table, reloads, page }))
      .catch((error: unknown) => {
        if (!controller.signal.aborted) setFailure({ table, reloads, message: errorMessage(error) });
      });
    return () => controller.abort();
  }, [table, reloads]);

  const isCurrent = (key: RequestKey | null) => key?.table === table && key.reloads === reloads;
  const loading = !isCurrent(data) && !isCurrent(failure);
  const error = failure && isCurrent(failure) ? failure.message : null;
  const page = data?.page ?? null;
  const lastPage = Math.max(page?.totalPages ?? 1, 1);
  const pastLastPage = isCurrent(data) && table.page > lastPage;

  const navigate = useCallback(
    (next: TableState, mode: "push" | "replace" = "push") => {
      const url = `/employees${toUrlSearch(next)}`;
      if (mode === "push") router.push(url, { scroll: false });
      else router.replace(url, { scroll: false });
    },
    [router],
  );

  // Typing replaces the history entry instead of adding one per search.
  const handleSearch = useCallback(
    (search: string) => navigate(changeTableState(table, { search }), "replace"),
    [navigate, table],
  );

  // A page that no longer exists (after a delete, or a hand-edited URL) shows the last real one.
  useEffect(() => {
    if (pastLastPage) navigate({ ...table, page: lastPage }, "replace");
  }, [pastLastPage, lastPage, table, navigate]);

  useEffect(() => {
    if (!statusMessage) return;
    const timer = setTimeout(() => setStatusMessage(""), STATUS_MESSAGE_MS);
    return () => clearTimeout(timer);
  }, [statusMessage]);

  function handleSaved(saved: Employee) {
    const action = formTarget?.employee ? "Saved" : "Added";
    setStatusMessage(`${action} ${saved.firstName} ${saved.lastName}.`);
    setReloads((count) => count + 1);
  }

  return (
    <>
      <AppHeader user={session.user} />
      <main className={styles.main}>
        <div className={styles.titleRow}>
          <h1>Employees</h1>
          {isAdmin && (
            <Button type="button" variant="primary" onClick={() => setFormTarget({ employee: null })}>
              Add employee
            </Button>
          )}
        </div>
        <p className={styles.statusMessage} role="status">
          {statusMessage}
        </p>

        <div className={styles.toolbar}>
          <div className={styles.search}>
            <SearchBox value={table.search} onSearch={handleSearch} />
          </div>
          <div className={styles.status}>
            <SelectField
              id="status-filter"
              label="Status"
              value={table.status ?? ""}
              onChange={(event) => {
                const status = EMPLOYEE_STATUSES.find((value) => value === event.target.value) ?? null;
                navigate(changeTableState(table, { status }));
              }}
            >
              <option value="">All</option>
              {EMPLOYEE_STATUSES.map((status) => (
                <option key={status} value={status}>
                  {STATUS_LABELS[status]}
                </option>
              ))}
            </SelectField>
          </div>
        </div>

        {error ? (
          <div className={styles.errorBanner} role="alert">
            <p>{error}</p>
            <Button type="button" onClick={() => setReloads((count) => count + 1)}>
              Retry
            </Button>
          </div>
        ) : page?.totalElements === 0 ? (
          <div className={styles.empty} aria-busy={loading}>
            <p>{table.search ? `No employees match '${table.search}'` : "No employees found."}</p>
            {table.search && (
              <Button type="button" onClick={() => navigate(changeTableState(table, { search: "" }))}>
                Clear search
              </Button>
            )}
          </div>
        ) : (
          <>
            <EmployeeTable
              employees={page?.content ?? null}
              placeholderRows={table.size}
              loading={loading}
              sort={table.sort}
              onSort={(field) => navigate(changeTableState(table, { sort: nextSort(table.sort, field) }))}
              onEdit={isAdmin ? (employee) => setFormTarget({ employee }) : undefined}
            />
            {page && (
              <Pagination
                page={table.page}
                size={table.size}
                totalElements={page.totalElements}
                totalPages={page.totalPages}
                onPageChange={(number) => navigate({ ...table, page: number })}
                onSizeChange={(size) => navigate(changeTableState(table, { size }))}
              />
            )}
          </>
        )}
      </main>
      {formTarget && (
        <EmployeeForm employee={formTarget.employee} onSaved={handleSaved} onClose={() => setFormTarget(null)} />
      )}
    </>
  );
}
