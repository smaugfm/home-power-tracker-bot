import { Event, MonitorableHost, PowerIspState } from "../types";
import { Storage } from "../storage/Storage";
import _ from "lodash";
import { NotificationsService } from "./NotificationsService";

export class EventsService {
  private readonly storage: Storage;
  private notifications: NotificationsService;

  constructor(storage: Storage, notifications: NotificationsService) {
    this.storage = storage;
    this.notifications = notifications;
  }

  public async onState(host: MonitorableHost, state: PowerIspState) {
    const current = this.storage.getCurrentState(host.host);
    if (!_.isEqual(current, state)) {
      const events = this.computeEvents(current, state);

      if (events.length > 0) {
        await Promise.all([
          ...events.map(e => this.notifications.notify(host.host, e)),
          this.storage.setCurrentState(host.host, state),
          this.storage.addEvents(host.host, events),
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
