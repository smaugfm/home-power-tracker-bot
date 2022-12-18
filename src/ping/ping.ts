import tcpp from "tcp-ping";
import ping from "ping";

export async function pingHost(
  host: string,
  deadline = 5,
  timeout = 1,
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

export async function tcpPingHost(host: string, port: number): Promise<boolean> {
  try {
    return new Promise<boolean>((resolve, reject) => {
      tcpp.probe(host, port, (err, res) => {
        if (err) reject(err);
        else resolve(res);
      });
    });
  } catch (err) {
    return Promise.resolve(false);
  }
}
