import { useRouter } from "next/navigation";
import { clearSession } from "@/lib/session";
import type { User } from "@/lib/types";
import Button from "./Button";
import styles from "./AppHeader.module.css";

export default function AppHeader({ user }: { user: User }) {
  const router = useRouter();

  function logout() {
    clearSession();
    router.replace("/login");
  }

  return (
    <header className={styles.header}>
      <span className={styles.brand}>Pesira Records</span>
      <div className={styles.user}>
        <span className={styles.name}>{user.fullName}</span>
        <span className={styles.role}>{user.role === "ADMIN" ? "Admin" : "User"}</span>
        <Button type="button" size="small" onClick={logout}>
          Log out
        </Button>
      </div>
    </header>
  );
}
