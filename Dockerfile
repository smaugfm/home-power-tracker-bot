FROM node:16

WORKDIR /opt/app

COPY package.json yarn.lock ./
RUN --mount=type=cache,target=/root/.yarn YARN_CACHE_FOLDER=/root/.yarn yarn install

COPY . .
RUN --mount=type=cache,target=./node_modules/.cache/webpack yarn build

CMD ["node", "dist/index.cjs"]
