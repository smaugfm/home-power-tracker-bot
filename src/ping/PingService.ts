import { pingHost } from "./ping";
import { tcpPingHost } from "./ping";
import { PowerIspState } from "../types";
import { clearInterval } from "timers";
import { log } from "../log/log";
import EventEmitter from "events";
import TypedEmitter from "typed-emitter";
import { Config } from "../config/Config";

export type PingServiceEvents = {
  ping: (configuration: Config, state: PowerIspState) => Promise<void>;
};

export class PingService extends (EventEmitter as new () => TypedEmitter<PingServiceEvents>) {
  private readonly intervalMs: number;
  private readonly configs: Config[];
  private timer: NodeJS.Timer | undefined;
  private enabled = false;

  constructor(configs: Config[], interval: number) {
    super();
    this.intervalMs = interval * 1000;
    this.configs = configs;
  }

  private ping() {
    return Promise.all(
      this.configs.map(async config => {
        try {
          if (config.host.port) {
            const [isp, power] = await Promise.all([
              pingHost(config.host.host),
              tcpPingHost(config.host.host, config.host.port),
            ]);
            this.emit("ping", config, {
              power,
              isp: isp !== undefined,
            });
          } else {
            const power = await pingHost(config.host.host);
            this.emit("ping", config, {
              power: power != undefined,
              isp: undefined,
            });
          }
        } catch (e) {
          log.error(`Error processing host ${config.host}`, e);
        }
      }),
    );
  }

  async start() {
    this.enabled = true;
    log.info("Started ping monitoring")
    await this.ping();
    this.timer = setInterval(() => {
      if (this.enabled) this.ping();
    }, this.intervalMs);
  }

  stop() {
    this.enabled = false;
    log.info("Stopped ping monitoring")
    if (this.timer) clearInterval(this.timer);
  }
}
