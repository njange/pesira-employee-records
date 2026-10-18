import { toIsoDate } from "./format";
import type { Employee, EmployeeRequest, EmployeeStatus } from "./types";

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const TEXT_MAX = 100;
const EMAIL_MAX = 255;
// Up to 10 digits before the point and 2 after, like the server's @Digits(integer = 10, fraction = 2).
const SALARY_PATTERN = /^\d{1,10}(\.\d{1,2})?$/;

export function isEmail(value: string): boolean {
  return EMAIL_PATTERN.test(value.trim());
}

export type LoginErrors = Partial<Record<"email" | "password", string>>;

export function validateLogin(email: string, password: string): LoginErrors {
  const errors: LoginErrors = {};
  if (!email.trim()) {
    errors.email = "Enter your email.";
  } else if (!isEmail(email)) {
    errors.email = "Enter a valid email address.";
  }
  if (!password) {
    errors.password = "Enter your password.";
  }
  return errors;
}

// Form inputs hold strings; they become an EmployeeRequest only after validation.
export interface EmployeeFormValues {
  firstName: string;
  lastName: string;
  email: string;
  department: string;
  jobTitle: string;
  salary: string;
  hireDate: string;
  status: EmployeeStatus | "";
}

export type EmployeeField = keyof EmployeeFormValues;
export type EmployeeErrors = Partial<Record<EmployeeField, string>>;

// In the order the form shows them.
export const EMPLOYEE_FIELDS: EmployeeField[] = [
  "firstName",
  "lastName",
  "email",
  "department",
  "jobTitle",
  "salary",
  "hireDate",
  "status",
];

export const EMPTY_EMPLOYEE: EmployeeFormValues = {
  firstName: "",
  lastName: "",
  email: "",
  department: "",
  jobTitle: "",
  salary: "",
  hireDate: "",
  status: "",
};

const TEXT_FIELDS = [
  ["firstName", "First name"],
  ["lastName", "Last name"],
  ["department", "Department"],
  ["jobTitle", "Job title"],
] as const;

export function validateEmployee(values: EmployeeFormValues, today = toIsoDate(new Date())): EmployeeErrors {
  const errors: EmployeeErrors = {};

  for (const [field, label] of TEXT_FIELDS) {
    const value = values[field].trim();
    if (!value) {
      errors[field] = `${label} is required.`;
    } else if (value.length > TEXT_MAX) {
      errors[field] = `${label} must be at most ${TEXT_MAX} characters.`;
    }
  }

  const email = values.email.trim();
  if (!email) {
    errors.email = "Email is required.";
  } else if (email.length > EMAIL_MAX) {
    errors.email = `Email must be at most ${EMAIL_MAX} characters.`;
  } else if (!isEmail(email)) {
    errors.email = "Enter a valid email address.";
  }

  const salary = values.salary.trim();
  if (!salary) {
    errors.salary = "Salary is required.";
  } else if (!SALARY_PATTERN.test(salary)) {
    errors.salary = "Enter a number with at most 2 decimals, like 85000.50.";
  } else if (Number(salary) <= 0) {
    errors.salary = "Salary must be greater than 0.";
  }

  if (!values.hireDate) {
    errors.hireDate = "Hire date is required.";
  } else if (values.hireDate > today) {
    errors.hireDate = "Hire date can't be in the future.";
  }

  if (!values.status) {
    errors.status = "Status is required.";
  }

  return errors;
}

export function toFormValues(employee: Employee): EmployeeFormValues {
  return {
    firstName: employee.firstName,
    lastName: employee.lastName,
    email: employee.email,
    department: employee.department,
    jobTitle: employee.jobTitle,
    salary: employee.salary.toFixed(2),
    hireDate: employee.hireDate,
    status: employee.status,
  };
}

export function toEmployeeRequest(
  values: EmployeeFormValues & { status: EmployeeStatus },
  version: number | null,
): EmployeeRequest {
  return {
    firstName: values.firstName.trim(),
    lastName: values.lastName.trim(),
    email: values.email.trim(),
    department: values.department.trim(),
    jobTitle: values.jobTitle.trim(),
    salary: Number(values.salary.trim()),
    hireDate: values.hireDate,
    status: values.status,
    version,
  };
}
