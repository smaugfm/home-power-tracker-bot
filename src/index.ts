import dotenv from "dotenv";
import { log } from "./log/log";
import { Telegraf } from "telegraf";
import { PingService } from "./ping/PingService";
import { ConfigurationService } from "./config/ConfigurationService";
import { NotificationsService } from "./core/NotificationsService";
import { EventsService } from "./core/EventsService";

dotenv.config();

const bot = new Telegraf(process.env["BOT_TOKEN"]!);

bot.use(ctx => {
  log.info("Received message: ", ctx.message);
});

const config = new ConfigurationService();
const ping = new PingService(config, 10);
const notifications = new NotificationsService(config, bot.telegram);
const events = new EventsService(config, notifications);

ping.on("ping", (host, state) => events.onState(host, state));

await ping.start();

log.info("Started monitoring. Version " + __VERSION__);
await bot.launch();
