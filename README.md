# ☕ CoffeeOS — Cafe POS Monorepo

> Platform Point-of-Sale lengkap untuk kafe, dengan integrasi chatbot WhatsApp/Telegram bertenaga LLM.

[![Backend CI](https://github.com/Youngstg/CAFE_POS/actions/workflows/backend-ci.yml/badge.svg)](https://github.com/Youngstg/CAFE_POS/actions/workflows/backend-ci.yml)
[![Web CI](https://github.com/Youngstg/CAFE_POS/actions/workflows/web-ci.yml/badge.svg)](https://github.com/Youngstg/CAFE_POS/actions/workflows/web-ci.yml)
[![Chatbot CI](https://github.com/Youngstg/CAFE_POS/actions/workflows/chatbot-gateway-ci.yml/badge.svg)](https://github.com/Youngstg/CAFE_POS/actions/workflows/chatbot-gateway-ci.yml)

---

## 📁 Struktur Monorepo

```
coffeeos/
├── apps/
│   ├── backend/           # Laravel 11 API — business logic utama
│   ├── web/               # React + Vite — dashboard & customer-facing
│   └── chatbot-gateway/   # Node.js TypeScript — WhatsApp & Telegram bot
├── infra/
│   ├── docker/            # Dockerfiles per service
│   ├── nginx/             # Reverse proxy config
│   ├── docker-compose.dev.yml   # Dev: MySQL
│   └── docker-compose.prod.yml  # Prod: PostgreSQL
├── docs/
│   ├── architecture/      # System overview & ERD
│   └── api/               # Postman collection
└── .github/
    └── workflows/         # CI/CD per service (path-filtered)
```

---

## 🚀 Cara Menjalankan (Development)

### Prasyarat
- Docker & Docker Compose
- PHP 8.3 + Composer (untuk backend lokal)
- Node.js 20+ (untuk web & chatbot-gateway lokal)

### 1. Clone & Setup

```bash
git clone https://github.com/Youngstg/CAFE_POS.git
cd CAFE_POS
```

### 2. Setup Backend (Laravel)

```bash
cd apps/backend
cp .env.example .env
composer install
php artisan key:generate
php artisan migrate --seed
php artisan serve
```

### 3. Setup Web (React)

```bash
cd apps/web
cp .env.example .env
npm install
npm run dev
```

### 4. Setup Chatbot Gateway

```bash
cd apps/chatbot-gateway
cp .env.example .env
npm install
npm run dev
```

### 5. Atau jalankan semua via Docker

```bash
docker compose -f infra/docker-compose.dev.yml up -d
```

---

## 🏗️ Arsitektur

Lihat [docs/architecture/system-overview.md](docs/architecture/system-overview.md) untuk detail.

```
Customer / Staff
     │
     ├── Web Browser ──────────────► apps/web (React)
     │                                    │
     │                                    ▼
     └── WhatsApp / Telegram ──► apps/chatbot-gateway (Node.js)
                                          │
                                          ▼
                               apps/backend (Laravel API)
                                          │
                                          ▼
                               MySQL (dev) / PostgreSQL (prod)
```

---

## 🔑 Fitur Utama

| Fitur | Status |
|-------|--------|
| Multi-tenant POS | ✅ |
| Manajemen Menu & Stok | ✅ |
| Order & Kitchen Display | ✅ |
| Laporan Keuangan | ✅ |
| Manajemen Shift | ✅ |
| Chatbot WhatsApp | 🚧 |
| Chatbot Telegram | 🚧 |
| RAG Knowledge Base | 🚧 |
| RBAC (Owner/Cashier/Kitchen/Customer) | ✅ |

---

## 📚 Dokumentasi

- [System Overview](docs/architecture/system-overview.md)
- [Business Flow](docs/architecture/business-flow.md)
- [API Collection](docs/api/postman_collection.json)

---

## 🤝 Kontribusi

1. Fork repository ini
2. Buat branch fitur: `git checkout -b feature/nama-fitur`
3. Commit: `git commit -m "feat: tambah fitur X"`
4. Push: `git push origin feature/nama-fitur`
5. Buat Pull Request ke `main`

---

## 📄 Lisensi

MIT License — lihat [LICENSE](LICENSE) untuk detail.
