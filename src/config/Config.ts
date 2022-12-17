import {
  ConfigurationData,
  Event,
  MonitorableHost,
  NotificationSettings,
  PowerIspState,
} from "../types";
import { Storage } from "./Storage";

export class Config {
  private readonly data: ConfigurationData;
  private readonly storage: Storage;

  constructor(data: ConfigurationData, storage: Storage) {
    this.data = data;
    this.storage = storage;
  }

  get host(): MonitorableHost {
    return this.data.host;
  }

  get notificationSettings(): NotificationSettings {
    if (!this.data.notificationSettings)
      this.data.notificationSettings = { power: true, isp: true };

    return this.data.notificationSettings;
  }

  set notificationSettings(value: NotificationSettings) {
    this.data.notificationSettings = value;

    this.storage.persist();
  }

  get state(): PowerIspState {
    return this.data.state;
  }

  set state(value: PowerIspState) {
    this.data.state = value;
    this.storage.persist();
  }

  get telegramChatIds(): number[] {
    return this.data.telegramChatIds;
  }

  addEvents(events: Event[]): void {
    if (!this.data.events) this.data.events = [];

    this.data.events.push(...events);
    this.storage.persist();
  }
}
