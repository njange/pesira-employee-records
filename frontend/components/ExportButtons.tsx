import { useState } from "react";
import { errorMessage, exportEmployees } from "@/lib/api";
import { exportFileName } from "@/lib/format";
import type { EmployeeFilters, ExportFormat } from "@/lib/types";
import Button from "./Button";
import styles from "./ExportButtons.module.css";

function saveFile(blob: Blob, fileName: string) {
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = fileName;
  document.body.append(link);
  link.click();
  link.remove();
  // Revoked on the next task so the browser has started the download first.
  setTimeout(() => URL.revokeObjectURL(url));
}

export default function ExportButtons({ filters }: { filters: EmployeeFilters }) {
  const [exporting, setExporting] = useState<ExportFormat | null>(null);
  const [error, setError] = useState("");

  async function handleExport(format: ExportFormat) {
    setExporting(format);
    setError("");
    try {
      saveFile(await exportEmployees(format, filters), exportFileName(format, new Date()));
    } catch (failure) {
      setError(errorMessage(failure));
    } finally {
      setExporting(null);
    }
  }

  return (
    <div className={styles.export}>
      <div className={styles.buttons}>
        <Button type="button" disabled={exporting !== null} onClick={() => handleExport("csv")}>
          {exporting === "csv" ? "Exporting…" : "Export CSV"}
        </Button>
        <Button type="button" disabled={exporting !== null} onClick={() => handleExport("json")}>
          {exporting === "json" ? "Exporting…" : "Export JSON"}
        </Button>
      </div>
      {error && (
        <p className={styles.error} role="alert">
          {error}
        </p>
      )}
    </div>
  );
}
