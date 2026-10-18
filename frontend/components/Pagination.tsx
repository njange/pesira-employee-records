import { formatRange } from "@/lib/format";
import { PAGE_SIZES } from "@/lib/query";
import Button from "./Button";
import styles from "./Pagination.module.css";

interface PaginationProps {
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  onPageChange: (page: number) => void;
  onSizeChange: (size: number) => void;
}

export default function Pagination({
  page,
  size,
  totalElements,
  totalPages,
  onPageChange,
  onSizeChange,
}: PaginationProps) {
  const lastPage = Math.max(totalPages, 1);
  return (
    <nav className={styles.pagination} aria-label="Pagination">
      <p className={styles.range}>{formatRange(page, size, totalElements)}</p>
      <div className={styles.size}>
        <label htmlFor="page-size">Rows per page</label>
        <select
          id="page-size"
          className={styles.select}
          value={size}
          onChange={(event) => onSizeChange(Number(event.target.value))}
        >
          {PAGE_SIZES.map((option) => (
            <option key={option} value={option}>
              {option}
            </option>
          ))}
        </select>
      </div>
      <div className={styles.buttons}>
        <Button type="button" size="small" disabled={page <= 1} onClick={() => onPageChange(1)}>
          First
        </Button>
        <Button type="button" size="small" disabled={page <= 1} onClick={() => onPageChange(page - 1)}>
          Previous
        </Button>
        <span className={styles.current}>
          Page {page} of {lastPage}
        </span>
        <Button type="button" size="small" disabled={page >= lastPage} onClick={() => onPageChange(page + 1)}>
          Next
        </Button>
        <Button type="button" size="small" disabled={page >= lastPage} onClick={() => onPageChange(lastPage)}>
          Last
        </Button>
      </div>
    </nav>
  );
}
