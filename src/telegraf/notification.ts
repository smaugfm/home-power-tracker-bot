import { Telegraf, Telegram } from "telegraf";
import { log } from "../log/log";

export async function notifyPower(bot: Telegram, power: boolean, chatId: number) {
  try {
    return await bot.sendMessage(
      chatId,
      `${power ? "💡" : "🕯"} Електропостачання ${power ? "відновлено" : "відсутнє"}`,
    );
  } catch (e) {
    log.error(`Error sending message to ${chatId}: `, e);
    return;
  }
}

export async function notifyIsp(bot: Telegram, isp: boolean, chatId: number) {
  try {
    return await bot.sendMessage(
      chatId,
      `${isp ? "🌐" : "🚫"} Інтернет ${isp ? "відновлено" : "відсутній"}`,
    );
  } catch (e) {
    log.error(`Error sending message to ${chatId}: `, e);
    return;
  }
}
