import path from "path";
import { fileURLToPath } from "url";
import { CleanWebpackPlugin } from "clean-webpack-plugin";
import { createRequire } from "module";
import pkg from "webpack";

const { DefinePlugin } = pkg;

const require = createRequire(import.meta.url);

const isProduction = process.env.NODE_ENV === "production";

const config = {
  entry: "./src/index.ts",
  output: {
    path: path.resolve(path.dirname(fileURLToPath(import.meta.url)), "dist"),
    filename: "index.cjs"
  },
  target: "node",
  devtool: "source-map",
  plugins: [new CleanWebpackPlugin(), new DefinePlugin({
    __VERSION__: JSON.stringify(require("./package.json").version)
  })],
  experiments: {
    topLevelAwait: true
  },
  module: {
    rules: [
      {
        test: /\.(ts|tsx)$/i,
        loader: "ts-loader",
        options: {
          compilerOptions: {
            noEmit: false
          }
        },
        exclude: ["/node_modules/", "/src/test/"]
      },
      {
        test: /\.(eot|svg|ttf|woff|woff2|png|jpg|gif)$/i,
        type: "asset"
      }
    ]
  },
  resolve: {
    extensions: [".tsx", ".ts", ".jsx", ".js", "..."]
  }
};

export default () => {
  if (isProduction) {
    config.mode = "production";
  } else {
    config.mode = "development";
  }
  return config;
};
