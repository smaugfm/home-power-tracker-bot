import { Event, MonitorableHost, PowerIspState } from "../types";
import _ from "lodash";
import { NotificationsService } from "./NotificationsService";
import { ConfigurationService } from "../config/ConfigurationService";

export class EventsService {
  private readonly config: ConfigurationService;
  private notifications: NotificationsService;

  constructor(config: ConfigurationService, notifications: NotificationsService) {
    this.config = config;
    this.notifications = notifications;
  }

  public async onState(host: MonitorableHost, state: PowerIspState) {
    const current = this.config.getCurrentState(host.host);
    if (!_.isEqual(current, state)) {
      const events = this.computeEvents(current, state);

      if (events.length > 0) {
        await Promise.all([
          ...events.map(e => this.notifications.notify(host.host, e)),
          this.config.setCurrentState(host.host, state),
          this.config.addEvents(host.host, events),
        ]);
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
