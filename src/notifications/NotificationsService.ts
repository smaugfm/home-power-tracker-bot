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

  private async sendTelegramMessage(chatId: number, message: string) {
    try {
      return await this.bot.sendMessage(chatId, message);
    } catch (e) {
      log.error(`Error sending message to ${chatId}: `, e);
      return;
    }
  }

  private getMessage(config: Config, event: Event) {
    let msg = "";
    switch (event.type) {
      case "isp":
        msg = `${event.state ? "🟩" : "🟥"} ${
          event.state ? "Інтернет з'явився" : "Інтернет зник"
        }`;
        break;
      case "power":
        msg = `${event.state ? "🟢" : "🔴"} Світло ${event.state ? "відновлено" : "зникло"}`;
        break;
      default:
        throw new Error("Unknown event type: " + event.type);
    }
    const stats = getStatsForEvent(config, event);
    if (!stats || stats.type === "empty") return msg;

    return `${msg}.\n\n${this.getStatsMessage(stats)}`;
  }

  private getStatsMessage(stats: Stats): string {
    switch (stats.type) {
      case "empty":
        return "";
      case "ispUp":
        return "Скільки не було: " + this.humanize(stats.lastInverse);
      case "ispDown":
        return `Скільки тримався: ${this.humanize(stats.lastInverse)}${
          stats.lastPowerUp
            ? `. Тривалість останньої зарядки акумуляторів: ${this.humanize(stats.lastPowerUp)}`
            : ""
        }`;
      case "powerUp":
        return `Скільки не було: ${this.humanize(stats.lastInverse)}`;
      case "powerDown":
        return `Скільки трималось: ${this.humanize(stats.lastInverse)}`;
    }
  }

  private humanize(duration: Temporal.Duration) {
    return durationFormat.format(duration.total("seconds"));
  }
}
