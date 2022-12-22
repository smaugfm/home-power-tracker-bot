import tcpp from "tcp-ping";
import ping from "ping";

export async function pingHost(
  host: string,
  deadline = 10,
  timeout = 3,
): Promise<string | undefined> {
  const numeric = /^((25[0-5]|(2[0-4]|1\d|[1-9]|)\d)\.?\b){4}$/.test(host);

  const result = await ping.promise.probe(host, {
    deadline: deadline,
    timeout,
    numeric,
  });

  if (result.alive) return result.numeric_host;

  return undefined;
}

export async function tcpPingHost(
  host: string,
  port: number,
  timeout = 3000,
  attempts = 3,
): Promise<boolean> {
  try {
    return new Promise<boolean>((resolve, reject) => {
      tcpp.ping(
        {
          address: host,
          port,
          timeout,
          attempts,
        },
        (err, res) => {
          if (err) reject(err);
          else resolve(res.min !== undefined);
        },
      );
    });
  } catch (err) {
    return Promise.resolve(false);
  }
}
