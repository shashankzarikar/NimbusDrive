# NimbusDrive

> A production-grade cloud storage application built from scratch — secure file upload, sharing, two-factor authentication, and storage quota enforcement, deployed live on render with Aivon.io for database.

![Java](https://img.shields.io/badge/Java-23-orange?style=flat-square)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.3-brightgreen?style=flat-square)
![AWS S3](https://img.shields.io/badge/AWS-S3-yellow?style=flat-square)
![MySQL](https://img.shields.io/badge/MySQL-8.0-blue?style=flat-square)

**Live demo:** [nimbusdrive.com](https://nimbusdrive-5377.onrender.com/login.html) &nbsp;|&nbsp; **GitHub:** [github.com/shashankzarikar/NimbusDrive](https://github.com/shashankzarikar/NimbusDrive)

---

## Features

- **JWT authentication** — stateless, BCrypt password hashing, username or email login
- **File operations** — upload, download, inline preview, delete with full ownership enforcement
- **File sharing** — unique UUID share tokens, optional expiry (1h / 24h / 7d / 30d / never), revocable at any time, public viewer page requires no account
- **Two-Factor Authentication** — email OTP via Gmail SMTP, 10-minute expiry, per-user toggle, cryptographically secure via `SecureRandom`
- **Storage quota** — 1 GB per user, hard block before S3 call, progress bar with amber / red thresholds
- **Change password** — confirmed via current password before update
- **Pagination** — all file list responses paginated, never unbounded
- **Input validation** — `@NotBlank`, `@Email`, `@Size` on all request DTOs
- **File restrictions** — MIME type whitelist, 10 MB size limit, both enforced before S3 call
- **Global exception handler** — every error returns `{ success: false, message: "..." }`
- **Multi-user isolation** — verified with Postman; users cannot access each other's files even with a valid token
- **Deployed live** — Deployed on render with environment variables configured and MySql configuration with Aivon.io cloud, accessible globally at `https://nimbusdrive-5377.onrender.com/login.html`

---

## Screenshots

### Authentication

<table>
<tr>
<td width="50%"><img src="screenshots/login.png" alt="Login page" /></td>
<td width="50%"><img src="screenshots/register.png" alt="Register page" /></td>
</tr>
<tr>
<td align="center"><sub>Login — split-panel design with inline 2FA OTP flow</sub></td>
<td align="center"><sub>Register — account creation with live validation</sub></td>
</tr>
</table>

### Dashboard

![Dashboard](screenshots/dashboard.png)

File grid with inline preview, kebab menu (download / share / delete), and pagination.

### Uploading a File

![Upload page](screenshots/upload.png)

Drag-and-drop or click-to-browse upload with live file size and type feedback.

### File Sharing

![Share modal](screenshots/share-modal.png)

Generate a public, revocable share link with configurable expiry directly from the dashboard.

![Shared files management](screenshots/shared-files.png)

A dedicated page to track, copy, and revoke every active share link in one place.

### Settings — Security & Storage

![Settings page](screenshots/settings.png)

Two-Factor Authentication toggle, password change, and a live storage quota bar with amber/red thresholds.

---

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        Browser                              │
│   login.html  register.html  dashboard.html  settings.html  │
│         share.html (public)   shared.html   upload.html     │
└──────────────────────┬──────────────────────────────────────┘
                       │ HTTPS — fetch() + JWT Bearer token
                       ▼
┌─────────────────────────────────────────────────────────────┐
│                  Spring Boot Application                    │
│                                                             │
│  JwtAuthFilter ──► SecurityConfig                           │
│                                                             │
│  AuthController       FileController     UserSettings       │
│  FileShareController  PublicShareController                 │
│          │                   │                              │
│          ▼                   ▼                              │
│  AuthService         FileService ◄──── FileShareService     │
│  TwoFactorService    UserService                            │
│          │                   │                              │
│          ▼                   ▼                              │
│  UserRepository      FileRepository   FileShareRepository   │
│  OtpTokenRepository                                         │
└──────┬───────────────────────┬────────────────────────────-─┘
       │                       │
       ▼                       ▼
┌─────────────┐      ┌──────────────────┐
│  MySQL 8.0  │      │    AWS S3        │
│             │      │  ap-south-1      │
│             │      │  Mumbai region   │
│  users      │      │                  │
│  files      │      │  username/       │
│  file_shares│      │  uuid_filename   │
│  otp_tokens │      │                  │
└─────────────┘      └──────────────────┘
```

**Request flow for a file upload:**
1. Browser sends `POST /api/files/upload` with `Authorization: Bearer <token>`
2. `JwtAuthFilter` validates the token — rejects with 401 if invalid
3. `FileController` receives the request, calls `FileService`
4. `FileService` validates MIME type and size, checks storage quota — rejects with 400 if exceeded
5. `S3Service` uploads to AWS S3
6. `FileRepository` saves metadata to MySQL, `storageUsed` incremented on the `User` record

---

## Tech Stack

| Layer | Technology                  |
|---|-----------------------------|
| Language | Java 23                     |
| Framework | Spring Boot 4.0.3           |
| Security | Spring Security 7.0.3 + jjwt 0.12.3 |
| Validation | Jakarta Bean Validation     |
| Database | MySQL 8.0 (Aivon.io cloud for production) |
| ORM | Hibernate 7.2.4 / JPA       |
| Cloud Storage | AWS S3 — ap-south-1 (Mumbai) |
| AWS SDK | software.amazon.awssdk 2.25.6 |
| Password Hashing | BCrypt                      |
| Build Tool | Maven                       |
| Frontend | HTML + CSS + Vanilla JS     |
| Email | Spring Boot Mail + Gmail SMTP |
| Deployment | Render                      |

---

## Security Model

```
Layer 1 — Frontend JS
  └── Checks localStorage for JWT on page load
  └── Bypassable — this is UX only, not a security guarantee

Layer 2 — Spring Security + JWT Filter  [server-enforced]
  └── JwtAuthFilter intercepts every API request
  └── Requests without a valid JWT are rejected at filter level
  └── Runs before any controller code executes

Layer 3 — File Ownership Check  [server-enforced]
  └── FileService.getFileEntity() verifies logged-in username
      matches file.uploadedBy on every download, preview, delete
  └── Returns 403 FORBIDDEN if they do not match
  └── A valid JWT from another user cannot access your files
```

**Additional security measures:**
- BCrypt one-way password hashing — original password unrecoverable even if DB is compromised
- Generic `"Invalid credentials!"` message for all login failures — prevents username enumeration
- Zero secrets hardcoded — all sensitive values in OS environment variables with `NIMBUSDRIVE_` prefix
- Dedicated IAM user (`nimbusdrive-s3-user`) with S3 permissions only — principle of least privilege
- Dedicated MySQL user — not root
- DTO pattern on all API responses — JPA entities never serialized directly
- File type MIME whitelist — enforced on backend, frontend check is UX only
- HTTP 410 Gone for expired/revoked share links — semantically correct, signals no action is possible

---

## API Reference

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/api/auth/register` | Public | Register new user |
| POST | `/api/auth/login` | Public | Login — returns JWT or `requires2FA: true` |
| POST | `/api/auth/verify-otp` | Public | Verify OTP and issue JWT after 2FA login |
| POST | `/api/files/upload` | JWT | Upload file — validates type, size, quota before S3 |
| GET | `/api/files?page=0&size=10` | JWT | List files — paginated |
| GET | `/api/files/download/{id}` | JWT | Download file from S3 — forced attachment |
| GET | `/api/files/preview/{id}` | JWT | Preview file inline — PDF, images, plain text |
| DELETE | `/api/files/{id}` | JWT | Delete from S3 and MySQL, decrements quota |
| POST | `/api/files/{id}/share` | JWT | Create share link with expiry |
| GET | `/api/files/{id}/share` | JWT | Get active share link for a file |
| DELETE | `/api/files/{id}/share` | JWT | Revoke share link |
| GET | `/api/files/shared` | JWT | List all active share links |
| GET | `/api/share/{token}/info` | Public | Get file info for public viewer — no JWT |
| GET | `/api/share/{token}/preview` | Public | Serve file inline to viewer |
| GET | `/api/share/{token}/download` | Public | Serve file as download to viewer |
| GET | `/api/user/2fa/status` | JWT | Get 2FA enabled status |
| POST | `/api/user/2fa/enable` | JWT | Enable 2FA — two-step: password then OTP |
| POST | `/api/user/2fa/disable` | JWT | Disable 2FA with password confirmation |
| GET | `/api/user/storage` | JWT | Get storage used and limit |
| POST | `/api/user/change-password` | JWT | Change password with current password confirmation |

All error responses follow a consistent format:
```json
{ "success": false, "message": "Descriptive error message" }
```

---

## Database Schema

Tables are auto-created by Hibernate (`ddl-auto=update`) on first startup.

### users
| Column | Type | Notes |
|---|---|---|
| id | BIGINT | Primary key, auto-increment |
| username | VARCHAR | Unique, not null |
| email | VARCHAR | Unique, not null |
| password | VARCHAR | BCrypt hashed, never plain text |
| role | ENUM | `USER` or `ADMIN`, stored as string |
| storage_limit | BIGINT | Default 1073741824 (1 GB in bytes) |
| storage_used | BIGINT | Default 0 |
| is_2fa_enabled | BOOLEAN | Default false |
| is_active | BOOLEAN | Default true |
| created_at | DATETIME | Set on creation |

### files
| Column | Type | Notes |
|---|---|---|
| id | BIGINT | Primary key, auto-increment |
| file_name | VARCHAR | Original filename |
| s3_key | VARCHAR | Full S3 path — `username/uuid_filename` |
| file_type | VARCHAR | MIME type |
| file_size | BIGINT | Size in bytes |
| uploaded_by | BIGINT | Foreign key → users.id, not null |
| uploaded_at | DATETIME | Set on upload |
| is_public | BOOLEAN | Default false |

### file_shares
| Column | Type | Notes |
|---|---|---|
| id | BIGINT | Primary key, auto-increment |
| file_id | BIGINT | Foreign key → files.id |
| created_by | BIGINT | Foreign key → users.id |
| share_token | VARCHAR(36) | UUID v4, unique — the public identifier |
| created_at | DATETIME | Set on creation |
| expires_at | DATETIME | Nullable — null means never expires |
| is_active | BOOLEAN | Default true — set to false on revoke, row kept for history |

### otp_tokens
| Column | Type | Notes |
|---|---|---|
| id | BIGINT | Primary key, auto-increment |
| user_id | BIGINT | Foreign key → users.id |
| otp_code | VARCHAR(6) | 6-digit code generated via SecureRandom |
| created_at | DATETIME | Set on creation |
| expires_at | DATETIME | 10 minutes after creation |
| is_used | BOOLEAN | Default false — marked true after verification, row kept for audit |

---

## Project Structure

```
com.nimbusdrive
├── config
│   ├── SecurityConfig.java          ← Spring Security rules + JWT filter setup
│   └── S3Config.java                ← AWS S3 client bean
├── controller
│   ├── AuthController.java          ← Register, Login, Verify OTP
│   ├── FileController.java          ← Upload, List, Download, Preview, Delete
│   ├── FileShareController.java     ← Share create, get, revoke, list (JWT)
│   ├── PublicShareController.java   ← Public share info, preview, download (no JWT)
│   └── UserSettingsController.java  ← 2FA, storage, change password
├── dto
│   ├── RegisterRequest.java         ← Registration input
│   ├── LoginRequest.java            ← Login input
│   ├── LoginResponse.java           ← token + requires2FA + message
│   ├── FileResponse.java            ← API output — no internal fields exposed
│   ├── FileDownloadResult.java      ← fileName + contentType + bytes
│   ├── FilePageResponse.java        ← Paginated file list + metadata
│   ├── FileShareResponse.java       ← Owner-facing share link info
│   ├── ShareInfoResponse.java       ← Public viewer info — no s3Key, no fileId
│   └── StorageResponse.java         ← storageUsed + storageLimit
├── exception
│   └── GlobalExceptionHandler.java  ← @RestControllerAdvice — all errors
├── filter
│   └── JwtAuthFilter.java           ← Validates JWT on every request
├── model
│   ├── User.java
│   ├── FileEntity.java              ← ManyToOne → User
│   ├── FileShare.java               ← ManyToOne → FileEntity, User
│   └── OtpToken.java                ← ManyToOne → User
├── repository
│   ├── UserRepository.java
│   ├── FileRepository.java
│   ├── FileShareRepository.java
│   └── OtpTokenRepository.java
├── service
│   ├── AuthService.java             ← Register + Login
│   ├── FileService.java             ← Coordinates S3Service + FileRepository
│   ├── FileShareService.java        ← Share create, revoke, validate, serve
│   ├── TwoFactorService.java        ← OTP generate, verify, enable, disable
│   ├── EmailService.java            ← Gmail SMTP — sends OTP emails
│   ├── UserService.java             ← Storage info
│   └── S3Service.java               ← Direct AWS S3 operations
└── util
    └── JwtUtil.java                 ← Generate, validate, extract JWT

src/main/resources/static/
├── login.html       ← JWT login + inline OTP modal for 2FA
├── register.html    ← User registration
├── dashboard.html   ← File list, kebab menu, share modal, pagination
├── upload.html      ← Drag & drop file upload
├── shared.html      ← Active share links — copy and revoke
└── settings.html    ← 2FA toggle, storage quota bar, change password
```

---

## Running Locally

**Prerequisites:** Java 23, Maven, MySQL 8.0, AWS account with S3 bucket

**1. Set environment variables**

```bash
DATABASE_URL                  = jdbc:mysql://localhost:3306/nimbusdrive
NIMBUSDRIVE_DB_USERNAME       = your_mysql_username
NIMBUSDRIVE_DB_PASSWORD       = your_mysql_password
NIMBUSDRIVE_JWT_SECRET_KEY    = your_jwt_secret_min_32_chars
NIMBUSDRIVE_AWS_ACCESS_KEY    = your_aws_access_key
NIMBUSDRIVE_AWS_SECRET_KEY    = your_aws_secret_key
NIMBUSDRIVE_AWS_BUCKET        = your_s3_bucket_name
NIMBUSDRIVE_AWS_REGION        = ap-south-1
NIMBUSDRIVE_MAIL_USERNAME     = your_gmail_address
NIMBUSDRIVE_MAIL_PASSWORD     = your_gmail_app_password
```

**2. Create the database**

```sql
CREATE DATABASE nimbusdrive;
```

**3. Run**

```bash
cd nimbusdrive
mvn spring-boot:run
```

**4. Open** `http://localhost:8080/login.html`

> Hibernate will auto-create all tables on first startup via `ddl-auto=update`.

---

## Known Limitations

| Limitation | Current Behavior | Better Approach |
|---|---|---|
| JWT in localStorage | Vulnerable to XSS | HttpOnly cookies |
| Registration enumeration | Specific messages on register | Email confirmation flow |
| Backend streams file bytes | App server is bottleneck at scale | S3 presigned URLs |
| No rate limiting on login | Brute force possible | Spring Rate Limiter |
| No refresh token | JWT expiry not handled gracefully | Access + refresh token pair |
| `ddl-auto=update` in production | Risky for schema changes | Flyway migrations |
| No logging | No visibility into system behavior | SLF4J + structured logs |
| Single file upload only | One file at a time via drag-and-drop or browse | Multi-file upload with progress per file |

---

## Future Roadmap

- [ ] Zero Knowledge Encryption — AES-256-GCM, client-side key derivation (PBKDF2), Web Crypto API
- [ ] Multi-file upload
- [ ] Infinite scroll on dashboard (replacing pagination)
- [ ] File versioning — max 10 versions per file, restore capability
- [ ] Admin panel
- [ ] S3 presigned URLs — direct client-to-S3 upload

---
