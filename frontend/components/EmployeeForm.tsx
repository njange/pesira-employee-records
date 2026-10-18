import { useEffect, useRef, useState, type ChangeEvent, type FormEvent } from "react";
import { ApiError, createEmployee, errorMessage, getEmployee, updateEmployee } from "@/lib/api";
import { STATUS_LABELS, toIsoDate } from "@/lib/format";
import { EMPLOYEE_STATUSES, type Employee } from "@/lib/types";
import {
  EMPLOYEE_FIELDS,
  EMPTY_EMPLOYEE,
  toEmployeeRequest,
  toFormValues,
  validateEmployee,
  type EmployeeErrors,
  type EmployeeField,
  type EmployeeFormValues,
} from "@/lib/validation";
import Button from "./Button";
import SelectField from "./SelectField";
import TextField from "./TextField";
import styles from "./EmployeeForm.module.css";

interface EmployeeFormProps {
  // null adds a new employee.
  employee: Employee | null;
  onSaved: (employee: Employee) => void;
  onClose: () => void;
}

function isEmployeeField(field: string): field is EmployeeField {
  return EMPLOYEE_FIELDS.some((known) => known === field);
}

export default function EmployeeForm({ employee, onSaved, onClose }: EmployeeFormProps) {
  const dialogRef = useRef<HTMLDialogElement>(null);
  const formRef = useRef<HTMLFormElement>(null);
  const [values, setValues] = useState(employee ? toFormValues(employee) : EMPTY_EMPLOYEE);
  const [version, setVersion] = useState(employee?.version ?? null);
  const [errors, setErrors] = useState<EmployeeErrors>({});
  const [edited, setEdited] = useState<Partial<Record<EmployeeField, boolean>>>({});
  const [formError, setFormError] = useState("");
  const [stale, setStale] = useState(false);
  const [saving, setSaving] = useState(false);
  const [today] = useState(() => toIsoDate(new Date()));

  useEffect(() => {
    dialogRef.current?.showModal();
  }, []);

  function update<K extends EmployeeField>(field: K, value: EmployeeFormValues[K]) {
    const next = { ...values, [field]: value };
    setValues(next);
    setEdited({ ...edited, [field]: true });
    if (errors[field]) setErrors({ ...errors, [field]: validateEmployee(next)[field] });
  }

  // Only fields the user has changed: tabbing past an empty field shouldn't flag it yet.
  function validateOnBlur(field: EmployeeField) {
    if (edited[field]) setErrors({ ...errors, [field]: validateEmployee(values)[field] });
  }

  function textField(field: Exclude<EmployeeField, "status">) {
    return {
      id: `employee-${field}`,
      name: field,
      value: values[field],
      error: errors[field],
      onChange: (event: ChangeEvent<HTMLInputElement>) => update(field, event.target.value),
      onBlur: () => validateOnBlur(field),
    };
  }

  function focusField(field: EmployeeField) {
    const element = formRef.current?.elements.namedItem(field);
    if (element instanceof HTMLElement) element.focus();
  }

  async function showServerError(error: unknown) {
    if (!(error instanceof ApiError)) {
      setFormError(errorMessage(error));
      return;
    }
    const known = error.fieldErrors.filter((fieldError) => isEmployeeField(fieldError.field));
    if (known.length > 0) {
      setErrors(Object.fromEntries(known.map((fieldError) => [fieldError.field, fieldError.message])));
      if (known.length < error.fieldErrors.length) setFormError(error.detail);
      return;
    }
    if (error.status !== 409) {
      setFormError(error.detail);
      return;
    }
    // A 409 is a duplicate email or, on edit, a stale version: stale if the stored version moved on.
    const latest = employee ? await getEmployee(employee.id).catch(() => null) : null;
    if (employee && latest?.version !== version) {
      setFormError(error.detail);
      setStale(true);
    } else {
      setErrors({ email: error.detail });
    }
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const found = validateEmployee(values);
    setErrors(found);
    setFormError("");
    setStale(false);
    const firstInvalid = EMPLOYEE_FIELDS.find((field) => found[field]);
    if (firstInvalid || values.status === "") {
      focusField(firstInvalid ?? "status");
      return;
    }

    setSaving(true);
    const request = toEmployeeRequest({ ...values, status: values.status }, version);
    try {
      const saved = employee ? await updateEmployee(employee.id, request) : await createEmployee(request);
      onSaved(saved);
      dialogRef.current?.close();
    } catch (error) {
      await showServerError(error);
      setSaving(false);
    }
  }

  async function reload() {
    if (!employee) return;
    try {
      const latest = await getEmployee(employee.id);
      setValues(toFormValues(latest));
      setVersion(latest.version);
      setErrors({});
      setEdited({});
      setFormError("");
      setStale(false);
    } catch (error) {
      setFormError(errorMessage(error));
    }
  }

  const title = employee ? `Edit ${employee.firstName} ${employee.lastName}` : "Add employee";

  return (
    <dialog ref={dialogRef} className={styles.dialog} aria-labelledby="employee-form-title" onClose={onClose}>
      <form ref={formRef} className={styles.form} onSubmit={handleSubmit} noValidate>
        <h2 id="employee-form-title">{title}</h2>
        {formError && (
          <div className={styles.formError} role="alert">
            <p>{formError}</p>
            {stale && (
              <Button type="button" size="small" onClick={reload}>
                Reload record
              </Button>
            )}
          </div>
        )}
        <div className={styles.fields}>
          <TextField label="First name" autoComplete="off" {...textField("firstName")} />
          <TextField label="Last name" autoComplete="off" {...textField("lastName")} />
          <TextField label="Email" type="email" autoComplete="off" {...textField("email")} />
          <TextField label="Department" autoComplete="off" {...textField("department")} />
          <TextField label="Job title" autoComplete="off" {...textField("jobTitle")} />
          <TextField label="Salary" inputMode="decimal" placeholder="85000.00" {...textField("salary")} />
          <TextField label="Hire date" type="date" max={today} {...textField("hireDate")} />
          <SelectField
            id="employee-status"
            name="status"
            label="Status"
            value={values.status}
            error={errors.status}
            onChange={(event) =>
              update("status", EMPLOYEE_STATUSES.find((status) => status === event.target.value) ?? "")
            }
            onBlur={() => validateOnBlur("status")}
          >
            <option value="">Choose a status</option>
            {EMPLOYEE_STATUSES.map((status) => (
              <option key={status} value={status}>
                {STATUS_LABELS[status]}
              </option>
            ))}
          </SelectField>
        </div>
        <div className={styles.actions}>
          <Button type="button" onClick={() => dialogRef.current?.close()}>
            Cancel
          </Button>
          <Button type="submit" variant="primary" disabled={saving}>
            {saving ? "Saving…" : employee ? "Save changes" : "Add employee"}
          </Button>
        </div>
      </form>
    </dialog>
  );
}
