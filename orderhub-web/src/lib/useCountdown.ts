import { useEffect, useState } from "react";

/**
 * Seconds left until `target` (an ISO date string), updated every second while `enabled`.
 * Used to show how long an unpaid order stays reserved.
 */
export function useCountdown(target: string | null | undefined, enabled: boolean): number | null {
  const [now, setNow] = useState(() => Date.now());

  useEffect(() => {
    if (!enabled || !target) return;
    const timer = setInterval(() => setNow(Date.now()), 1000);
    return () => clearInterval(timer); // always clean up timers when the component goes away
  }, [enabled, target]);

  if (!target) return null;
  return Math.max(0, Math.floor((new Date(target).getTime() - now) / 1000));
}

export const formatCountdown = (seconds: number) =>
  `${Math.floor(seconds / 60)}:${String(seconds % 60).padStart(2, "0")}`;
