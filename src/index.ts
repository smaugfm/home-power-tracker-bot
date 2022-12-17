import dotenv from "dotenv";
import { Storage } from "./storage/Storage";
import { log } from "./log/log";
import { Telegraf } from "telegraf";
import { PingService } from "./service/PingService";
import { EventsService } from "./service/EventsService";
import { NotificationsService } from "./service/NotificationsService";
import { stat } from "fs";

dotenv.config();

const bot = new Telegraf(process.env["BOT_TOKEN"]!);

bot.use(ctx => {
  log.info("Received message: ", ctx.message);
});

const storage = new Storage();
const ping = new PingService(storage, 10);
const notifications = new NotificationsService(storage, bot.telegram);
const events = new EventsService(storage, notifications);

ping.on("ping", (host, state) => events.onState(host, state));

await ping.start();

log.info("Started monitoring. Version " + __VERSION__);
await bot.launch();
