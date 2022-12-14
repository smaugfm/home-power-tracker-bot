const path = require("path");
const {CleanWebpackPlugin} = require("clean-webpack-plugin");

const isProduction = process.env.NODE_ENV === "production";

const config = {
  entry: "./index.ts",
  output: {
    path: path.resolve(__dirname, "dist"),
  },
  target: "node",
  devtool: "source-map",
  plugins: [
    new CleanWebpackPlugin(),
  ],
  module: {
    rules: [
      {
        test: /\.(ts|tsx)$/i,
        loader: "ts-loader",
        options: {
          compilerOptions: {
            noEmit: false,
          },
        },
        exclude: ["/node_modules/"],
      },
      {
        test: /\.(eot|svg|ttf|woff|woff2|png|jpg|gif)$/i,
        type: "asset",
      },
    ],
  },
  resolve: {
    extensions: [".tsx", ".ts", ".jsx", ".js", "..."],
  },
};

module.exports = () => {
  if (isProduction) {
    config.mode = "production";
  } else {
    config.mode = "development";
  }
  return config;
};
