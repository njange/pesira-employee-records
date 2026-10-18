import type { EmployeeStatus } from "./types";

const LOCALE = "en-GB";

const moneyFormat = new Intl.NumberFormat(LOCALE, {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});

const countFormat = new Intl.NumberFormat(LOCALE);

// API dates are calendar dates (yyyy-mm-dd); formatting them in UTC stops them shifting a day.
const dateFormat = new Intl.DateTimeFormat(LOCALE, {
  day: "numeric",
  month: "short",
  year: "numeric",
  timeZone: "UTC",
});

export const STATUS_LABELS: Record<EmployeeStatus, string> = {
  ACTIVE: "Active",
  ON_LEAVE: "On leave",
  TERMINATED: "Terminated",
};

export function formatMoney(amount: number): string {
  return moneyFormat.format(amount);
}

export function formatDate(isoDate: string): string {
  return dateFormat.format(new Date(`${isoDate}T00:00:00Z`));
}

// The local calendar date as yyyy-mm-dd, the format the API and <input type="date"> use.
export function toIsoDate(date: Date): string {
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${date.getFullYear()}-${month}-${day}`;
}

export function formatRange(page: number, size: number, total: number): string {
  if (total === 0) return "No results";
  const first = (page - 1) * size + 1;
  const last = Math.min(page * size, total);
  return `Showing ${countFormat.format(first)}–${countFormat.format(last)} of ${countFormat.format(total)}`;
}
