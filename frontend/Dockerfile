FROM node:24-alpine AS build
WORKDIR /workspace

ARG VITE_API_BASE_URL=http://localhost:8080
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
ENV COREPACK_ENABLE_DOWNLOAD_PROMPT=0

RUN corepack enable && corepack prepare pnpm@10.15.0 --activate
COPY pnpm-lock.yaml pnpm-workspace.yaml ./
COPY frontend/package.json frontend/package.json
RUN pnpm install --frozen-lockfile --filter academic-monitor-frontend...

COPY frontend frontend
RUN pnpm --filter academic-monitor-frontend build

FROM nginx:1.27-alpine
COPY frontend/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /workspace/frontend/dist /usr/share/nginx/html

EXPOSE 80
