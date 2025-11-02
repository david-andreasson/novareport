# Build
FROM node:20-alpine AS build
WORKDIR /app
ARG VITE_ACCOUNTS_API_BASE="http://localhost:8080"
ARG VITE_SUBS_API_BASE="http://localhost:8081"
ENV VITE_ACCOUNTS_API_BASE=${VITE_ACCOUNTS_API_BASE}
ENV VITE_SUBS_API_BASE=${VITE_SUBS_API_BASE}
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

# Serve
FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
