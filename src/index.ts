import "temporal-polyfill/global";
import dotenv from "dotenv";
import { log } from "./log/log";
import { Telegraf } from "telegraf";
import { PingService } from "./ping/PingService";
import { NotificationsService } from "./notifications/NotificationsService";
import { Storage } from "./config/Storage";
import { EventsService } from "./events/EventsService";
import { StableNetworkMonitoringService } from "./connectivity/StableNetworkMonitoringService";

dotenv.config();

const bot = new Telegraf(process.env["BOT_TOKEN"]!);

bot.use(ctx => {
  log.info("Received message: ", ctx.message);
});

const storage = new Storage();

const connectivity = new StableNetworkMonitoringService({
  interval: 600,
  timeout: 1000,
  consecutiveTriesToConsiderOnline: 20,
  pingHosts: ["188.190.254.254", "8.8.8.8", "1.1.1.1"],
});
const ping = new PingService(storage.getConfigs(), 10);
const notifications = new NotificationsService(bot.telegram);
const events = new EventsService(notifications);

ping.on("ping", (host, state) => events.onState(host, state));
connectivity.on("status", async online => {
  if (online) await ping.start();
  else ping.stop();
});

await connectivity.start();

log.info("Current time: " + Temporal.Now.zonedDateTimeISO().toString());
await bot.launch();
