export async function main() {
  console.log("main()");
  await delay(100);
  console.log("wait 100ms");
}

export const sum = (...a: number[]) => a.reduce((acc, val) => acc + val, 0);

export function delay(ms: number) {
  return new Promise(resolve => setTimeout(resolve, ms));
}
