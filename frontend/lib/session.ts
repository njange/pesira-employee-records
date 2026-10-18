import type { Session } from "./types";

const STORAGE_KEY = "pesira.session";

let cachedRaw: string | null = null;
let cachedSession: Session | null = null;

export function isExpired(session: Session, now = Date.now()): boolean {
  return Date.parse(session.expiresAt) <= now;
}

// Returns the same object while storage is unchanged, so React can use it as a store snapshot.
export function getSession(): Session | null {
  const raw = sessionStorage.getItem(STORAGE_KEY);
  if (raw !== cachedRaw) {
    cachedRaw = raw;
    cachedSession = raw === null ? null : (JSON.parse(raw) as Session);
  }
  return cachedSession !== null && !isExpired(cachedSession) ? cachedSession : null;
}

export function saveSession(session: Session): void {
  sessionStorage.setItem(STORAGE_KEY, JSON.stringify(session));
}

export function clearSession(): void {
  sessionStorage.removeItem(STORAGE_KEY);
}
