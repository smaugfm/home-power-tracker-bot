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

export type EventType = "power" | "isp";

export type EventObject = {
  type: EventType;
  state: boolean;
  time: string;
};

export interface ConfigurationData {
  host: MonitorableHost;
  state: PowerIspState;
  notificationSettings: NotificationSettings;
  telegramChatIds: number[];
  events: EventObject[];
}

export type Stats = SingleEventStats | SummaryStats;

export type SingleEventStats = IspUpStats | IspDownStats | PowerUpStats | PowerDownStats | EmptyStats;

export type SummaryStats = LastPeriodSummaryStats & {
  type: EventType;
} | EmptyStats;

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
  sinceLastPowerDown?: Temporal.Duration;
};

export interface LastPeriodSummaryStats {
  summaryType: "day" | "week" | "month";
  upTotal: Temporal.Duration;
  downTotal: Temporal.Duration;
  upPercent: number;
}
