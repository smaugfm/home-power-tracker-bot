import { pingHost } from "../ping/ping";
import { tcpPingHost } from "../ping/ping";
import { MonitorableHost, PowerIspState } from "../types";
import { clearInterval } from "timers";
import { log } from "../log/log";
import { Storage } from "../storage/Storage";
import EventEmitter from "events";
import TypedEmitter from "typed-emitter";

export type PingServiceEvents = {
  ping: (host: MonitorableHost, state: PowerIspState) => Promise<void>;
};

export class PingService extends (EventEmitter as new () => TypedEmitter<PingServiceEvents>) {
  private readonly intervalMs: number;
  private readonly hosts: MonitorableHost[];
  private timer: NodeJS.Timer | undefined;

  constructor(storage: Storage, interval: number) {
    super();
    this.intervalMs = interval * 1000;
    this.hosts = storage.hosts;
  }

  private ping() {
    return Promise.all(
      this.hosts.map(async host => {
        try {
          if (host.port) {
            const [isp, power] = await Promise.all([
              pingHost(host.host),
              tcpPingHost(host.host, host.port),
            ]);
            this.emit("ping", host, {
              power,
              isp: isp !== undefined,
            });
          } else {
            const power = await pingHost(host.host);
            this.emit("ping", host, {
              power: power != undefined,
              isp: undefined,
            });
          }
        } catch (e) {
          log.error(`Error processing host ${host}`, e);
        }
      }),
    );
  }

  async start() {
    await this.ping();
    this.timer = setInterval(() => this.ping(), this.intervalMs);
  }

  stop() {
    if (this.timer) clearInterval(this.timer);
  }
}
