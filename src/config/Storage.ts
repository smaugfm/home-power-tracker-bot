import fsSync from "fs";
import { ConfigurationData } from "../types";
import { Config } from "./Config";

export class Storage {
  private readonly db: ConfigurationData[];
  private readonly filename: string;

  constructor(filename = "db.json") {
    this.db = this.populate(filename);
    this.filename = filename;
  }

  getConfigs(): Config[] {
    return Object.values(this.db).map(data => new Config(data, this));
  }

  public persist() {
    return fsSync.writeFileSync(this.filename, JSON.stringify(this.db, undefined, 2));
  }

  private populate(filename: string): ConfigurationData[] {
    let read: ConfigurationData[] = [];
    if (fsSync.existsSync(filename)) {
      read = JSON.parse(fsSync.readFileSync(filename, "utf-8"));
    }

    return Object.assign({}, read);
  }
}
