# ====================
# Stage 1: Build TypeScript
# ====================
FROM node:20-alpine AS builder

WORKDIR /app

COPY apps/chatbot-gateway/package.json apps/chatbot-gateway/package-lock.json* ./
RUN npm ci --frozen-lockfile

COPY apps/chatbot-gateway/ .
RUN npm run build

# ====================
# Stage 2: Production Runtime
# ====================
FROM node:20-alpine AS production

WORKDIR /app

COPY --from=builder /app/dist ./dist
COPY --from=builder /app/node_modules ./node_modules
COPY apps/chatbot-gateway/package.json .

EXPOSE 3000
CMD ["node", "dist/index.js"]
