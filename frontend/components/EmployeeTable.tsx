import { STATUS_LABELS, formatDate, formatMoney } from "@/lib/format";
import type { Sort } from "@/lib/query";
import type { Employee, SortField } from "@/lib/types";
import Button from "./Button";
import styles from "./EmployeeTable.module.css";

interface Column {
  field: SortField;
  label: string;
  numeric?: boolean;
}

const COLUMNS: Column[] = [
  { field: "lastName", label: "Name" },
  { field: "email", label: "Email" },
  { field: "department", label: "Department" },
  { field: "jobTitle", label: "Job title" },
  { field: "salary", label: "Salary", numeric: true },
  { field: "hireDate", label: "Hire date" },
  { field: "status", label: "Status" },
];

interface SortableHeaderProps {
  column: Column;
  sort: Sort;
  onSort: (field: SortField) => void;
}

function SortableHeader({ column, sort, onSort }: SortableHeaderProps) {
  const active = sort.field === column.field;
  const ascending = sort.direction === "asc";
  return (
    <th
      scope="col"
      aria-sort={active ? (ascending ? "ascending" : "descending") : undefined}
      className={column.numeric ? styles.numeric : undefined}
    >
      <button type="button" className={styles.sortButton} onClick={() => onSort(column.field)}>
        {column.label}
        <span aria-hidden="true" className={active ? styles.arrowActive : styles.arrow}>
          {active ? (ascending ? "▲" : "▼") : "↕"}
        </span>
      </button>
    </th>
  );
}

interface EmployeeTableProps {
  // null until the first page has loaded.
  employees: Employee[] | null;
  placeholderRows: number;
  loading: boolean;
  sort: Sort;
  onSort: (field: SortField) => void;
  // Only passed for admins; without it the Actions column is hidden.
  onEdit?: (employee: Employee) => void;
}

export default function EmployeeTable({
  employees,
  placeholderRows,
  loading,
  sort,
  onSort,
  onEdit,
}: EmployeeTableProps) {
  const columnCount = COLUMNS.length + (onEdit ? 1 : 0);
  return (
    <div className={styles.scroller} role="region" aria-label="Employees table" tabIndex={0}>
      <table className={loading ? `${styles.table} ${styles.loading}` : styles.table} aria-busy={loading}>
        <thead>
          <tr>
            {COLUMNS.map((column) => (
              <SortableHeader key={column.field} column={column} sort={sort} onSort={onSort} />
            ))}
            {onEdit && (
              <th scope="col" className={styles.actionsHeader}>
                Actions
              </th>
            )}
          </tr>
        </thead>
        <tbody>
          {employees === null
            ? Array.from({ length: placeholderRows }, (_, index) => (
                <tr key={index}>
                  <td colSpan={columnCount}>
                    <span className={styles.skeleton} />
                  </td>
                </tr>
              ))
            : employees.map((employee) => (
                <tr key={employee.id}>
                  <td className={styles.name}>
                    {employee.firstName} {employee.lastName}
                  </td>
                  <td>{employee.email}</td>
                  <td>{employee.department}</td>
                  <td>{employee.jobTitle}</td>
                  <td className={styles.numeric}>{formatMoney(employee.salary)}</td>
                  <td>{formatDate(employee.hireDate)}</td>
                  <td>
                    <span className={`${styles.badge} ${styles[employee.status]}`}>
                      {STATUS_LABELS[employee.status]}
                    </span>
                  </td>
                  {onEdit && (
                    <td>
                      <Button
                        type="button"
                        size="small"
                        aria-label={`Edit ${employee.firstName} ${employee.lastName}`}
                        onClick={() => onEdit(employee)}
                      >
                        Edit
                      </Button>
                    </td>
                  )}
                </tr>
              ))}
        </tbody>
      </table>
    </div>
  );
}
