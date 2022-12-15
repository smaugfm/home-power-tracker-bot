export type MonitorableHost = {
  host: string;
  port?: number;
}

export type PowerIspState = {
  power: boolean;
  isp: boolean | undefined;
}

export type TelegramChatIds = {
  telegramChatIds: number[]
}
