import winston from "winston";

export const log = winston.createLogger({
  level: process.env["NODE_ENV"] !== "production" ? "debug" : "info",
  format: winston.format.combine(winston.format.colorize(), winston.format.simple()),
  transports: [new winston.transports.Console()],
});
