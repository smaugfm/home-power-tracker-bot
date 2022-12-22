import { IspDownStats, SingleEventStats } from "../types";
import { Config } from "../config/Config";
import { log } from "../log/log";
import { Event } from "../events/Event";

export function getSingleEventStats(event: Event, config: Config): SingleEventStats {
  try {
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
  } catch (e) {
    log.error(e);
    return {
      type: "empty",
    };
  }
}

function getLastInverseStats(config: Config, event: Event): SingleEventStats {
  const lastInverseEvent = event.previousOfSameType(config, (e: Event) => e.state !== event.state);
  if (!lastInverseEvent)
    return {
      type: "empty",
    };

  const type = (event.type + (event.state ? "Up" : "Down")) as SingleEventStats["type"];

  return {
    type,
    lastInverse: event.since(lastInverseEvent),
  };
}

function getIspDownStats(config: Config, event: Event): IspDownStats {
  const result = getLastInverseStats(config, event) as IspDownStats;

  if (!config.state.power) {
    const lastPowerDown = config.previousEvent("power", (e: Event) => !e.state);
    const lastPowerUp = config.previousEvent("power", (e: Event) => e.state);

    if (lastPowerDown) {
      result.sinceLastPowerDown = event.since(lastPowerDown);
      if (lastPowerUp) result.lastPowerUp = lastPowerDown.since(lastPowerUp);
    }
  }

  return result;
}
