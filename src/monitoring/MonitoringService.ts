import { pingHost, tcpPingHost } from "../ping/ping";
import { MonitorableHost } from "../types";
import { clearInterval } from "timers";
import { log } from "../log/log";

export class MonitoringService {
  private readonly intervalMs: number;
  private readonly hosts: MonitorableHost[];
  private notification: (
    host: MonitorableHost,
    hasPower: boolean,
    hasIsp?: boolean
  ) => Promise<void>;
  private timer: NodeJS.Timer | undefined;

  constructor(
    monitorable: MonitorableHost[],
    options: {
      interval: number;
    },
    notification: (host: MonitorableHost, hasPower: boolean, hasIsp?: boolean) => Promise<void>
  ) {
    this.notification = notification;
    this.intervalMs = options.interval * 1000;
    this.hosts = monitorable;
  }

  start() {
    this.timer = setInterval(async () => {
      await Promise.all(
        this.hosts.map(async host => {
          try {
            if (host.port) {
              const [isp, power] = await Promise.all([
                pingHost(host.host),
                tcpPingHost(host.host, host.port)
              ]);
              await this.notification(host, power, isp != undefined);
            } else {
              const power = await pingHost(host.host);
              await this.notification(host, power != undefined);
            }
          } catch (e) {
            log.error(`Error processing host ${host}`, e);
          }
        })
      );
    }, this.intervalMs);
  }

  stop() {
    if (this.timer) clearInterval(this.timer);
  }
}
