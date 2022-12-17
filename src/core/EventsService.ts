import { Event, PowerIspState } from "../types";
import _ from "lodash";
import { NotificationsService } from "./NotificationsService";
import { Config } from "../config/Config";

export class EventsService {
  private notifications: NotificationsService;

  constructor(notifications: NotificationsService) {
    this.notifications = notifications;
  }

  public async onState(configuration: Config, state: PowerIspState) {
    const current = configuration.state;
    if (!_.isEqual(current, state)) {
      const events = this.computeEvents(current, state);

      if (events.length > 0) {
        await Promise.all([events.map(e => this.notifications.notify(configuration, e))]);

        configuration.state = state;
        configuration.addEvents(events);
      }
    }
  }

  private computeEvents(current: PowerIspState, state: PowerIspState): Event[] {
    const events: Event[] = [];
    if (current.power !== state.power) {
      const event: Event = {
        type: "power",
        state: state.power,
        time: new Date().toISOString(),
      };
      events.push(event);
    }
    if (state.isp !== undefined && current.isp !== state.isp) {
      const event: Event = {
        type: "isp",
        state: state.isp,
        time: new Date().toISOString(),
      };
      events.push(event);
    }

    return events;
  }
}
