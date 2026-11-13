# syntax=docker/dockerfile:1

# ============================================================
# Stage 1: build the React app into plain HTML, CSS and JS files
# ============================================================
FROM node:22-alpine AS build
WORKDIR /app

# Install dependencies first so Docker can cache this layer until package*.json changes.
# "npm ci" installs exactly what package-lock.json says (repeatable builds).
COPY package.json package-lock.json ./
RUN npm ci

COPY . .

# Vite reads VITE_* settings at BUILD time and writes them into the JavaScript files, so they are
# build arguments here (docker-compose passes them in), not run-time environment variables.
ARG VITE_GOOGLE_CLIENT_ID=""
ARG VITE_CURRENCY="INR"
ARG VITE_LOCALE="en-IN"
ENV VITE_GOOGLE_CLIENT_ID=$VITE_GOOGLE_CLIENT_ID \
    VITE_CURRENCY=$VITE_CURRENCY \
    VITE_LOCALE=$VITE_LOCALE
RUN npm run build

# ============================================================
# Stage 2: serve the built files with nginx (the Node tools are left behind)
# ============================================================
FROM nginx:stable-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
