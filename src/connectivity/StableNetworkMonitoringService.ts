import isOnline from "is-online";
import TypedEmitter from "typed-emitter";
import EventEmitter from "events";
import { log } from "../log/log";
import { pingHost } from "../ping/ping";

export type ConnectivityServiceEvents = {
  status: (online: boolean) => Promise<void>;
};

export interface ConnectivityServiceOptions {
  interval: number;
  timeout: number;
  consecutiveTriesToConsiderOnline: number;
  pingHosts?: string[];
}

export class StableNetworkMonitoringService extends (EventEmitter as new () => TypedEmitter<ConnectivityServiceEvents>) {
  private readonly options: ConnectivityServiceOptions;
  private status = false;
  private successfulTriesAfterDrop = 0;

  constructor(options: ConnectivityServiceOptions) {
    super();
    this.options = options;
  }

  public async start() {
    log.info("Started network stability monitoring");
    await this.check();
    setInterval(() => this.check(), this.options.interval);
  }

  private async isOnlineComposite(): Promise<boolean> {
    const onlineCheck = isOnline({
      timeout: this.options.timeout,
    });
    const pingChecks = (this.options.pingHosts ?? [])
      .map(h => pingHost(h, this.options.timeout / 1000, 500))
      .map(r => r !== undefined);

    return (await Promise.all([onlineCheck, ...pingChecks])).every(x => x);
  }

  private async check() {
    const online = await this.isOnlineComposite();
    if (!online) {
      if (this.successfulTriesAfterDrop > 1) {
        log.info(`Network still has issues. Ping attempt #${this.successfulTriesAfterDrop} failed`);
      }
      this.successfulTriesAfterDrop = 0;
    }
    if (online != this.status) {
      if (online) {
        if (++this.successfulTriesAfterDrop >= this.options.consecutiveTriesToConsiderOnline) {
          this.status = true;
          this.successfulTriesAfterDrop = 0;
          log.info(`Network is STABLE again`);
          this.emit("status", true);
        }
      } else {
        this.status = false;
        log.error("Network ISSUES detected");
        this.emit("status", false);
      }
    }
  }
}