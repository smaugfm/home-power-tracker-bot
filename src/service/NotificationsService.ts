import { Storage } from "../storage/Storage";
import { Telegram } from "telegraf";
import { log } from "../log/log";
import _ from "lodash";
import { Event } from "../types";

export class NotificationsService {
  private readonly storage: Storage;
  private readonly bot: Telegram;

  constructor(storage: Storage, bot: Telegram) {
    this.storage = storage;
    this.bot = bot;
  }

  async notify(host: string, event: Event) {
    return Promise.all(
      this.storage
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
