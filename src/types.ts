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
