FROM node:24-alpine AS build

WORKDIR /app

RUN npm install -g pnpm@10.34.1

COPY package.json pnpm-lock.yaml ./
RUN pnpm install --frozen-lockfile

COPY . .

ARG VITE_API_BASE_URL=/api
ARG VITE_APP_ENV=aws-dev
ARG VITE_DEMO_MODE=false
ARG VITE_PUBLIC_BASE_PATH=/
ARG VITE_ROUTER_HISTORY=web

ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}
ENV VITE_APP_ENV=${VITE_APP_ENV}
ENV VITE_DEMO_MODE=${VITE_DEMO_MODE}
ENV VITE_PUBLIC_BASE_PATH=${VITE_PUBLIC_BASE_PATH}
ENV VITE_ROUTER_HISTORY=${VITE_ROUTER_HISTORY}

RUN pnpm build

FROM nginxinc/nginx-unprivileged:1.27-alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 8080
