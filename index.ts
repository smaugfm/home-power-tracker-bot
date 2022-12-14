import {main} from "./src/main";

(async () => {
  try {
    await main();
    console.log("finish");
  } catch (e) {
    console.error(e);
    process.exit(1);
  }
})();
