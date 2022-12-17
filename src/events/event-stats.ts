import { Event, IspDownStats, Stats } from "../types";
import { Config } from "../config/Config";
import _ from "lodash";
import { between } from "../util/temporal";

export function getStatsForEvent(config: Config, event: Event): Stats {
  switch (event.type) {
    case "isp":
      if (event.state) {
        return getLastInverseStats(config, event);
      } else {
        return getIspDownStats(config, event);
      }
    case "power":
      if (event.state) {
        return getLastInverseStats(config, event);
      } else {
        return getLastInverseStats(config, event);
      }
    default:
      throw new Error("Unknown event type: " + event.type);
  }
}

function getLastInverseStats(config: Config, event: Event): Stats {
  const lastInverseEvent = getLastEvent(config, event.type, (e: Event) => e.state !== event.state);

  const type = (event.type + (event.state ? "Up" : "Down")) as Stats["type"];

  return {
    type,
    lastInverse: between(event, lastInverseEvent),
  };
}

function getIspDownStats(config: Config, event: Event): Stats {
  const result = getLastInverseStats(config, event) as IspDownStats;

  if (!config.state.power) {
    const lastPowerDown = getLastEvent(config, "power", (e: Event) => !e.state);
    const lastPowerUp = getLastEvent(config, "power", (e: Event) => e.state);

    result.lastPowerUp = between(lastPowerDown, lastPowerUp);
  }

  return result;
}

function getLastEvent(
  config: Config,
  type: Event["type"],
  predicate: (e: Event) => boolean,
): Event {
  const lastIspUpEvent = _.findLast(config.events, e => e.type === type && predicate(e));
  if (!lastIspUpEvent) {
    throw new Error("Could not find event");
  }
  return lastIspUpEvent;
}
