# ==========================================
# Stage 1: Build Frontend Assets
# ==========================================
FROM node:22-alpine AS builder

WORKDIR /app

# Copy dependency definitions
COPY package*.json ./

# Clean install dependencies
RUN npm ci

# Copy frontend source code
COPY . .

# Build production bundle with Vite & Tailwind CSS v4
RUN npm run build

# ==========================================
# Stage 2: Serve with Nginx
# ==========================================
FROM nginx:stable-alpine

# Remove default static content
RUN rm -rf /usr/share/nginx/html/*

# Copy built assets from builder
COPY --from=builder /app/dist /usr/share/nginx/html

# Copy custom Nginx configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
