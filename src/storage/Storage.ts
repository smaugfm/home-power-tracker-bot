import fsSync, { promises as fs } from "fs";
import { Event, MonitorableHost, NotificationSettings, PowerIspState } from "../types";

interface DbSchema {
  [k: string]: {
    host: MonitorableHost;
    state: PowerIspState;
    notificationSettings: NotificationSettings;
    telegramChatIds: number[];
    events: Event[];
  };
}

export class Storage {
  private readonly db: DbSchema;
  private readonly filename: string;

  constructor(filename = "db.json") {
    this.db = this.read(filename);
    this.filename = filename;
  }

  getTelegramChatIds(host: string): number[] {
    const monitorable = this.monitorable(host);

    return monitorable.telegramChatIds;
  }

  get hosts(): MonitorableHost[] {
    return Object.values(this.db).map(x => x.host);
  }

  getCurrentState(host: string): PowerIspState {
    const monitorable = this.monitorable(host);

    return monitorable.state;
  }

  async setCurrentState(host: string, state: PowerIspState): Promise<void> {
    const monitorable = this.monitorable(host);

    monitorable.state = state;
    await this.write();
  }

  async addEvents(host: string, events: Event[]): Promise<void> {
    const value = this.monitorable(host);

    if (!value.events) value.events = [];

    value.events.push(...events);
    await this.write();
  }

  private monitorable(host: string) {
    const value = this.db[host];
    if (!value) throw new Error("Missing host: " + host);

    return value;
  }

  private read(filename: string): DbSchema {
    let read: DbSchema = {};
    if (fsSync.existsSync(filename)) {
      read = JSON.parse(fsSync.readFileSync(filename, "utf-8"));
    }

    return Object.assign({}, read);
  }

  private write() {
    return fs.writeFile(this.filename, JSON.stringify(this.db, undefined, 2));
  }
}
