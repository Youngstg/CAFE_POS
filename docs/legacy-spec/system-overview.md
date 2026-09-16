# System Overview — CoffeeOS

## Gambaran Umum

CoffeeOS adalah platform POS (Point-of-Sale) multi-tenant untuk kafe, dibangun dengan arsitektur monorepo yang terdiri dari tiga aplikasi utama:

| Aplikasi | Teknologi | Tujuan |
|----------|-----------|--------|
| `apps/backend` | Laravel 11 + PHP 8.3 | REST API utama — business logic |
| `apps/web` | React + Vite + JSX | Dashboard owner/kasir + e-menu pelanggan |
| `apps/chatbot-gateway` | Node.js + TypeScript | Bot WhatsApp & Telegram dengan LLM |

---

## Arsitektur High-Level

```
┌─────────────────────────────────────────────────────────┐
│                     CLIENT LAYER                         │
│                                                         │
│  ┌──────────────┐    ┌──────────────┐                   │
│  │  Web Browser │    │  WhatsApp /  │                   │
│  │  (React App) │    │  Telegram    │                   │
│  └──────┬───────┘    └──────┬───────┘                   │
└─────────┼────────────────────┼──────────────────────────┘
          │                    │
          │                    ▼
          │         ┌────────────────────┐
          │         │  chatbot-gateway   │
          │         │  (Node.js/TS)      │
          │         │  - WhatsApp WH     │
          │         │  - Telegram WH     │
          │         │  - LLM Orchestr.  │
          │         │  - RAG Retriever  │
          │         └────────┬───────────┘
          │                  │ HTTP (internal)
          ▼                  ▼
┌─────────────────────────────────────────┐
│              BACKEND API                │
│           (Laravel 11 REST)             │
│                                         │
│  ┌─────────────┐  ┌──────────────────┐  │
│  │ POS APIs    │  │ Chatbot Tools API│  │
│  │ /menu       │  │ /chatbot/menu    │  │
│  │ /orders     │  │ /chatbot/orders  │  │
│  │ /stock      │  │ /chatbot/stock   │  │
│  │ /shifts     │  │ /chatbot/rag     │  │
│  │ /finance    │  │                  │  │
│  └─────────────┘  └──────────────────┘  │
└──────────────┬──────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────┐
│              DATABASE LAYER             │
│                                         │
│  MySQL (dev)     PostgreSQL (prod)      │
│  + pgvector (untuk RAG embeddings)      │
└─────────────────────────────────────────┘
```

---

## Role & Hak Akses (RBAC)

| Role | Akses |
|------|-------|
| `owner` | Full access: laporan, pengaturan, staff, menu, stok |
| `cashier` | Order, transaksi, shift, menu (read) |
| `kitchen` | Kitchen display, update status order |
| `customer` | E-menu, buat order via QR/chatbot |
| `guest` | Hanya bisa melihat menu publik |

---

## Multi-Tenancy

Setiap outlet/kafe adalah **Tenant** yang terisolasi. Semua data (menu, order, stok, staff) di-scope per tenant menggunakan `BelongsToTenant` trait di Laravel.

```
Tenant (kafe)
 ├── Outlets
 ├── Users (owner, kasir, dapur)
 ├── Menus & Categories
 ├── Orders & OrderItems
 ├── Shifts & Transactions
 └── KnowledgeBase (chatbot RAG)
```

---

## Tech Stack Lengkap

### Backend (`apps/backend`)
- **Framework**: Laravel 11
- **Auth**: Laravel Sanctum (token-based)
- **Database**: MySQL (dev) / PostgreSQL (prod)
- **Queue**: Laravel Queue (database driver)
- **Testing**: PHPUnit + Laravel Test helpers

### Web (`apps/web`)
- **Framework**: React 18 + Vite
- **State**: React Context + hooks
- **HTTP**: Axios
- **Routing**: React Router v6
- **UI**: Custom CSS (no framework)

### Chatbot Gateway (`apps/chatbot-gateway`)
- **Runtime**: Node.js 20 + TypeScript
- **Channels**: WhatsApp Business API, Telegram Bot API
- **LLM**: Google Gemini / OpenAI (function calling)
- **RAG**: pgvector via backend API
- **Framework**: Express.js

### Infrastructure
- **Containerization**: Docker + Docker Compose
- **Reverse Proxy**: Nginx
- **CI/CD**: GitHub Actions (path-filtered per service)
