import { SummaryStats } from "../types";
import { Config } from "../config/Config";
import { Event } from "../events/Event";

export function getSummaryStats(event: Event, config: Config): SummaryStats[] {
  const prev = event.previousOfSameType(config);
  const stats: SummaryStats[] = [];
  if (!prev) return stats;

  return stats;
}

function getDailyStats(): SummaryStats {
  return { type: "empty" };
}

function getMonthlyStats(): SummaryStats {
  return { type: "empty" };
}

function getWeeklyStats(): SummaryStats {
  return { type: "empty" };
}
