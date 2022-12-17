import { Telegram } from "telegraf";
import { log } from "../log/log";
import { Event } from "../types";
import { ConfigurationService } from "../config/ConfigurationService";

export class NotificationsService {
  private readonly config: ConfigurationService;
  private readonly bot: Telegram;

  constructor(config: ConfigurationService, bot: Telegram) {
    this.config = config;
    this.bot = bot;
  }

  async notify(host: string, event: Event) {
    if (!this.config.getNotificationSettings(host)[event.type]) return Promise.resolve();

    return Promise.all(
      this.config
        .getTelegramChatIds(host)
        .map(chatId =>
          event.type === "power"
            ? this.notifyPower(event.state, chatId)
            : this.notifyIsp(event.state, chatId),
        ),
    );
  }

  private async notifyIsp(isp: boolean, chatId: number) {
    return this.sendTelegramMessage(
      chatId,
      `${isp ? "🟩" : "🟥"} Інтернет ${isp ? "відновлено" : "відсутній"}`,
    );
  }

  private notifyPower(power: boolean, chatId: number) {
    return this.sendTelegramMessage(
      chatId,
      `${power ? "🟢" : "🔴"} Електропостачання ${power ? "відновлено" : "відсутнє"}`,
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
}
