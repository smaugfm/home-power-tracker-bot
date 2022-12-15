import fsSync, { promises as fs } from "fs";
import { MonitorableHost, PowerIspState, TelegramChatIds } from "../types";
import _ from "lodash";

type DbSchema = Record<string, MonitorableHost & PowerIspState & TelegramChatIds>;

export class Storage {
  private readonly db: DbSchema;
  private readonly filename: string;

  constructor(filename = "db.json") {
    this.db = this.read(filename);
    this.filename = filename;
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

  getTelegramChatIds(host: string): TelegramChatIds {
    const value = this.db[host];
    if (!value) throw new Error("Missing host: " + host);

    return _.pick(value, "telegramChatIds");
  }

  get hosts(): MonitorableHost[] {
    return Object.values(this.db);
  }

  getPowerIspState(host: string): Partial<PowerIspState> {
    const value = this.db[host];
    if (!value) throw new Error("Missing host: " + host);

    return _.pick(value, "power", "isp");
  }

  async setPowerIspState(host: string, power: boolean, isp?: boolean): Promise<void> {
    const value = this.db[host];
    if (!value) throw new Error("Missing host: " + host);

    this.db[host] = {
      ...value,
      power,
      isp,
    };
    await this.write();
  }
}
