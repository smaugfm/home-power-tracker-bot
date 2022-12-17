import { Event } from "../types";

export function between(later: Event, earlier: Event): Temporal.Duration {
  const earlierTime = Temporal.ZonedDateTime.from(earlier.time);
  const laterTime = Temporal.ZonedDateTime.from(later.time);

  return laterTime.toInstant().since(earlierTime.toInstant());
}
