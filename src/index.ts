import "temporal-polyfill/global";
import dotenv from "dotenv";
import { log } from "./log/log";
import { Telegraf } from "telegraf";
import { PingService } from "./ping/PingService";
import { NotificationsService } from "./notifications/NotificationsService";
import { Storage } from "./config/Storage";
import { EventsService } from "./events/EventsService";

dotenv.config();

const bot = new Telegraf(process.env["BOT_TOKEN"]!);

bot.use(ctx => {
  log.info("Received message: ", ctx.message);
});

const storage = new Storage();

const ping = new PingService(storage.getConfigs(), 10);
const notifications = new NotificationsService(bot.telegram);
const events = new EventsService(notifications);

ping.on("ping", (host, state) => events.onState(host, state));

await ping.start();

log.info("Started. Current time: " + Temporal.Now.zonedDateTimeISO().toString());

await bot.launch();
