import dotenv from "dotenv";
import { Storage } from "./storage/Storage";
import { MonitoringService } from "./monitoring/MonitoringService";
import { log } from "./log/log";
import { notifyIsp, notifyPower } from "./telegraf/notification";
import { Telegraf } from "telegraf";

dotenv.config();

const bot = new Telegraf(process.env["BOT_TOKEN"]!);

bot.use(ctx => {
  log.info("Received message: ", ctx.message);
});

const storage = new Storage();
const monitoring = new MonitoringService(
  storage.hosts,
  { interval: 10 },
  async (host, power, isp) => {
    const current = storage.getPowerIspState(host.host);

    if (current.power !== power) {
      await Promise.all(
        storage
          .getTelegramChatIds(host.host)
          .telegramChatIds.map(chatId => notifyPower(bot.telegram, power, chatId))
      );
    }
    if (current.isp !== isp) {
      await Promise.all(
        storage
          .getTelegramChatIds(host.host)
          .telegramChatIds.map(chatId => notifyIsp(bot.telegram, power, chatId))
      );
    }

    await storage.setPowerIspState(host.host, power, isp);
  }
);

monitoring.start();
log.info("Started monitoring. Version " + __VERSION__);
await bot.launch();

