# DangCap — Hệ thống Quản lý Tài chính Cá nhân (Personal Finance Manager)

Ứng dụng quản lý tài chính cá nhân fullstack với khả năng cô lập dữ liệu đa người dùng, giao dịch an toàn (Pessimistic Locking + Idempotency Key), tự động hóa định kỳ, cảnh báo ngân sách, mục tiêu tiết kiệm, báo cáo trực quan (Recharts), song ngữ Tiếng Việt / Tiếng Anh và giao diện Sáng / Tối.

---

## 1. Công nghệ sử dụng

| Tầng | Công nghệ |
| :--- | :--- |
| **Backend** | Java 22, Spring Boot 3.3.5, Spring Security 6 (JWT Access/Refresh Token Rotation), Spring Data JPA / Hibernate 6, Flyway Migrations, MapStruct, Bean Validation, SpringDoc OpenAPI 3 |
| **Database & Cache** | MySQL 8.4 (InnoDB, ACID), Redis 7 (Token Blacklist, Auth Rate Limiting, Idempotency Keys, Report & Dashboard Caching) |
| **Storage** | Local Storage hoặc Cloudinary (`STORAGE_PROVIDER=cloudinary`) với kiểm tra MIME + Magic Bytes |
| **Frontend** | React 18 (JavaScript JSX), Vite 5 (Lazy Route Splitting + Vendor Chunks), Tailwind CSS 3 (`darkMode: 'class'`), Redux Toolkit, React Router v6, Axios, Recharts, Heroicons |
| **Deployment** | Docker Multi-stage builds, Docker Compose, Nginx Reverse Proxy |

---

## 2. Cấu trúc thư mục

```text
personal-finance-manager/
├── backend/
│   ├── src/main/java/com/pfm/personalfinancemanager/
│   │   ├── annotation/         # @EvictUserReports
│   │   ├── aspect/             # ReportCacheEvictionAspect (evict sau DB commit)
│   │   ├── configuration/      # Security, Redis, CORS, WebMvc, OpenAPI, Scheduling
│   │   ├── controller/         # 11 REST Controllers (/api/v1/*)
│   │   ├── dto/                # Request & Response DTOs
│   │   ├── entity/             # JPA Entities
│   │   ├── exception/          # ErrorCode & GlobalExceptionHandler
│   │   ├── mapper/             # MapStruct Mappers
│   │   ├── repository/         # JPA Repositories & Projections
│   │   ├── security/           # JwtService, TokenBlacklist, AuthRateLimitFilter
│   │   ├── service/            # Business Logic Services
│   │   └── util/               # ImageFileValidator, SecurityUtils, Calculators
│   ├── src/main/resources/
│   │   ├── application.yml     # Shared defaults, compression, pagination cap
│   │   ├── application-dev.yml # Local MySQL + Redis + SQL logs
│   │   ├── application-prod.yml# Strict JWT secret, Swagger disabled, tuned HikariCP
│   │   └── db/migration/       # Flyway V1 (schema), V2 (seed), V3 (indexes)
│   ├── Dockerfile
│   └── pom.xml
├── frontend/
│   ├── public/                 # Favicon & DangCap logo
│   ├── src/
│   │   ├── components/         # Charts, Layout, Modals, Feedback, BrandLogo
│   │   ├── features/           # Redux auth, notifications, feedback slices
│   │   ├── i18n/               # Song ngữ vi / en
│   │   ├── layouts/            # AuthLayout & MainLayout
│   │   ├── pages/              # 14 lazy-loaded pages
│   │   ├── services/           # Axios instance + auto token refresh + API modules
│   │   └── theme/              # ThemeContext (Light / Dark persistence)
│   ├── Dockerfile
│   ├── nginx.conf              # Reverse proxy /api & /uploads + SPA fallback + gzip
│   └── vite.config.js
├── docs/
│   └── API_ENDPOINTS.md        # Tài liệu chi tiết toàn bộ REST API
├── docker-compose.yml          # MySQL + Redis + Backend + Frontend (Nginx)
└── .env.example                # Mẫu biến môi trường Production / Docker
```

---

## 3. Hướng dẫn chạy Development (Local)

### Yêu cầu
- Java 22 (JDK 22), Maven 3.9+
- Node.js 20+ (`npm`)
- MySQL 8 đang chạy (mặc định `localhost:3306/pfm_db`, hoặc cấu hình qua env)
- Redis 7 đang chạy tại `localhost:6379`

### Chạy Backend
```bash
cd backend
mvn spring-boot:run
```
- Backend API: `http://localhost:8080`
- Swagger UI (chế độ `dev`): `http://localhost:8080/swagger-ui.html`
- Health Check: `http://localhost:8080/api/v1/health`

### Chạy Frontend
```bash
cd frontend
npm install
npm run dev
```
- Truy cập giao diện tại: `http://localhost:5173` (Vite tự proxy `/api` và `/uploads` về `http://localhost:8080`).

---

## 4. Triển khai Production với Docker Compose

1. **Tạo file `.env` từ mẫu:**
   ```bash
   cp .env.example .env
   ```
2. **Cập nhật mật khẩu và `JWT_SECRET` mạnh trong `.env`** (ít nhất 32 ký tự):
   ```bash
   openssl rand -base64 48
   ```
   Nếu sử dụng Cloudinary cho ảnh đại diện / minh chứng giao dịch, đặt `STORAGE_PROVIDER=cloudinary` và điền `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET`.
3. **Build và khởi chạy toàn bộ stack:**
   ```bash
   docker compose up -d --build
   ```
4. **Kiểm tra trạng thái các container:**
   ```bash
   docker compose ps
   curl http://localhost/api/v1/health
   ```
   - Nginx lắng nghe ở cổng `${FRONTEND_PORT:-80}`, phục vụ SPA tĩnh và proxy `/api/*`, `/uploads/*` nội bộ sang `backend:8080`.
   - Profile `prod` tự động tắt Swagger UI (`/swagger-ui.html`), bắt buộc biến `JWT_SECRET`, sử dụng `hibernate.ddl-auto=validate` và tự chạy Flyway migrations (`V1`–`V3`).

---

## 5. Kiểm thử & Bảo mật (Phase 12)

- **Chạy toàn bộ Unit & Integration Tests (Backend):**
  ```bash
  cd backend
  mvn test
  ```
- **Build Production Bundle (Frontend):**
  ```bash
  cd frontend
  npm run build
  ```
- **Các lớp bảo vệ chính:**
  - `AuthRateLimitFilter` + Redis fixed-window chống brute-force trên `/api/v1/auth/*` (`429 RATE_LIMIT_EXCEEDED`).
  - JWT Refresh Token Rotation + phát hiện dùng lại token (Reuse Detection thu hồi toàn bộ phiên) + Access Token Blacklist trên Redis khi Logout.
  - Cô lập tài nguyên theo `userId` trên mọi API, Pessimistic Locking (`FOR UPDATE`) chống race condition khi cập nhật số dư, `X-Idempotency-Key` chống gửi trùng giao dịch.
  - HTTP Security Headers (`X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff`, `Referrer-Policy`, `Permissions-Policy`) và giới hạn `max-page-size=100`.

Xem danh sách đầy đủ API tại [docs/API_ENDPOINTS.md](docs/API_ENDPOINTS.md).

