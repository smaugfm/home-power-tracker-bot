export type MonitorableHost = {
  host: string;
  port?: number;
};

export type PowerIspState = {
  power: boolean;
  isp: boolean | undefined;
};

export type NotificationSettings = {
  power: boolean;
  isp: boolean;
};

export type Event = {
  type: "isp" | "power";
  state: boolean;
  time: string;
};

export interface ConfigurationData {
  host: MonitorableHost;
  state: PowerIspState;
  notificationSettings: NotificationSettings;
  telegramChatIds: number[];
  events: Event[];
}

export type Stats = IspUpStats | IspDownStats | PowerUpStats | PowerDownStats | EmptyStats;

export interface LastInverse {
  lastInverse: Temporal.Duration;
}

export type EmptyStats = {
  type: "empty";
};

export type PowerUpStats = LastInverse & {
  type: "powerUp";
};

export type PowerDownStats = LastInverse & {
  type: "powerDown";
};

export type IspUpStats = LastInverse & {
  type: "ispUp";
};

export type IspDownStats = LastInverse & {
  type: "ispDown";
  lastPowerUp?: Temporal.Duration;
};
