import { Telegram } from "telegraf";
import DurationUnitFormat from "../intl-unofficial-duration-unit-format-3.1.0";
import { log } from "../log/log";
import { Event, Stats } from "../types";
import { Config } from "../config/Config";
import { getStatsForEvent } from "../events/event-stats";

const durationFormat = new DurationUnitFormat("uk-UA", {
  style: "long",
  format: "{days} {hours} {minutes}",
});

export class NotificationsService {
  private readonly bot: Telegram;

  constructor(bot: Telegram) {
    this.bot = bot;
  }

  async notify(config: Config, event: Event) {
    if (!config.notificationSettings[event.type]) return Promise.resolve();

    return Promise.all(
      config.telegramChatIds.map(chatId =>
        this.sendTelegramMessage(chatId, this.getMessage(config, event)),
      ),
    );
  }

  private getMessage(config: Config, event: Event) {
    let msg = "";
    switch (event.type) {
      case "isp":
        msg = `${event.state ? "🟩" : "🟥"} ${
          event.state ? "Інтернет з'явився" : "Інтернет пропав"
        }`;
        break;
      case "power":
        msg = `${event.state ? "🟢" : "🔴"} Світло ${event.state ? "відновлено" : "пропало"}`;
        break;
      default:
        throw new Error("Unknown event type: " + event.type);
    }
    const stats = getStatsForEvent(config, event);
    if (!stats || stats.type === "empty") return msg;

    return `${msg}.\n\n${this.getStatsMessage(stats)}`;
  }

  private async sendTelegramMessage(chatId: number, message: string) {
    try {
      return await this.bot.sendMessage(chatId, message);
    } catch (e) {
      log.error(`Error sending message to ${chatId}: `, e);
      return;
    }
  }

  private format(duration: Temporal.Duration) {
    return durationFormat.format(duration.total("seconds"));
  }

  private getStatsMessage(stats: Stats): string {
    switch (stats.type) {
      case "empty":
        return "";
      case "ispUp":
        return `Його не було ${this.format(stats.lastInverse)}`;
      case "ispDown":
        return `Він тримався ${this.format(stats.lastInverse)}${
          stats.lastPowerUp ? `. Акумулятори заряджались ${this.format(stats.lastPowerUp)}` : ""
        }`;
      case "powerUp":
        return `Його не було ${this.format(stats.lastInverse)}`;
      case "powerDown":
        return `Воно було ${this.format(stats.lastInverse)}`;
    }
  }
}
