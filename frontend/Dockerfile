# =====================================================================
# Stage 1: Build optimized Vite React production bundle
# =====================================================================
FROM node:20-alpine AS builder
WORKDIR /build

ARG VITE_API_BASE_URL=/api/v1
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}

COPY package.json package-lock.json ./
RUN npm ci

COPY index.html postcss.config.js tailwind.config.js vite.config.js ./
COPY public ./public
COPY src ./src

RUN npm run build

# =====================================================================
# Stage 2: Serve static SPA + reverse-proxy API via Nginx
# =====================================================================
FROM nginx:1.27-alpine AS runtime

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /build/dist /usr/share/nginx/html

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD wget -qO- http://127.0.0.1:80/ >/dev/null 2>&1 || exit 1

CMD ["nginx", "-g", "daemon off;"]

