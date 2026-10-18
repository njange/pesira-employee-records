import { useSyncExternalStore } from "react";
import { getSession } from "./session";
import type { Session } from "./types";

// The session only changes on login and logout, and both navigate to another page.
function subscribe(): () => void {
  return () => {};
}

// undefined while rendering on the server (no sessionStorage there), null when logged out.
export function useSession(): Session | null | undefined {
  return useSyncExternalStore(subscribe, getSession, () => undefined);
}
