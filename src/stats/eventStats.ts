import { Stats } from "../types";
import { Config } from "../config/Config";
import { getSingleEventStats } from "./singleEventStats";
import { getSummaryStats } from "./summaryStats";
import { Event } from "../events/Event";

export function getStatsForEvent(config: Config, event: Event): Stats[] {
  return [getSingleEventStats(event, config), ...getSummaryStats(event, config)];
}
