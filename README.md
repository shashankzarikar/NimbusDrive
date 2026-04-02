# NimbusDrive

A cloud-based file storage application built from scratch using Java Spring Boot and AWS S3. Authenticated users can upload, download, list, and delete files. Each user's files are isolated.Access is enforced at the server level — users cannot access files belonging to others, even with a valid token.

**Live:** [nimbusdrive.com](https://nimbusdrive-production.up.railway.app/login.html) — deployed on Railway.app  
**GitHub:** [github.com/shashankzarikar/NimbusDrive](https://github.com/shashankzarikar/NimbusDrive)

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 23 |
| Framework | Spring Boot 4.0.3 |
| Security | Spring Security 7.0.3 + jjwt 0.12.3 |
| Database | MySQL 8.0 (Railway cloud in production) |
| ORM | Hibernate 7.2.4 / JPA |
| Cloud Storage | AWS S3 — ap-south-1 (Mumbai) |
| AWS SDK | software.amazon.awssdk 2.25.6 |
| Password Hashing | BCrypt |
| Build Tool | Maven |
| Frontend | HTML + CSS + Vanilla JS |
| Deployment | Railway.app |

---

## Design Decisions

**Why JWT over sessions?**  
JWT is stateless — the server does not store session data. Each request carries authentication information, making the API scalable and suitable for distributed systems. Token expiry is configured in JwtUtil (currently 24 hours). No refresh token mechanism is implemented yet.

**Why AWS S3 over local disk storage?**  
Local disk storage is not reliable in ephemeral cloud environments like Railway. S3 is durable, scalable, and decoupled from the application server. Files persist independently of deployments.

**Why UUID-based S3 keys?**  
Files are stored as `username/uuid_originalfilename` in S3. The UUID prevents conflicts between environments — both local MySQL and Railway MySQL auto-increment from ID=1, but they point to completely different S3 objects because each key contains a random UUID.

**Why DTO pattern?**  
JPA entities contain internal fields like hashed passwords, S3 keys, and database IDs that should never be exposed through the API. `FileResponse` DTO is the explicit API contract — only fields we choose to expose are serialized.

**Why dedicated IAM user and MySQL user?**  
Principle of least privilege. The app uses a dedicated `nimbusdrive-s3-user` IAM account with S3 permissions only — not the root AWS account. Same for MySQL. If credentials are compromised, the blast radius is limited.

**Why backend-handled uploads initially?**
Uploading via backend simplifies implementation and keeps control within the server. However, this creates a scalability bottleneck. A presigned URL approach (direct client → S3 upload) is planned as an improvement.
---

## API Endpoints

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/api/auth/register` | Public | Register new user |
| POST | `/api/auth/login` | Public | Login, returns JWT token |
| POST | `/api/files/upload` | JWT | Upload file to AWS S3 |
| GET | `/api/files` | JWT | List files for logged-in user |
| GET | `/api/files/download/{id}` | JWT | Download file from AWS S3 |
| DELETE | `/api/files/{id}` | JWT | Delete from S3 and MySQL |

### Request / Response Examples

**POST /api/auth/login**
```
Request body:
{
  "usernameOrEmail": "your username or Email",
  "password": "yourpassword"
}

Response 200 OK — plain JWT string:
eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJzaGFzaGFuayJ9...

Response 400 — plain string:
"User not found!"
"Invalid password!"
```

**Note on Authentication Response Format**
The login API currently returns a raw JWT string as the response body,
This was kept simple for initial implementation. A structured JSON response (e.g., `{ token, type, expiresIn }`) is planned for better extensibility and alignment with common API design practices.

**Note on Error Messages**
The current implementation differentiates between "user not found" and "invalid password". This will be unified into a generic "Invalid credentials" response to prevent user enumeration attacks.


**GET /api/files**
```
Response 200 OK:
[
  {
    "id": 1,
    "fileName": "report.pdf",
    "fileSize": 204800,
    "fileType": "application/pdf",
    "isPublic": false,
    "uploadedAt": "2026-04-01T10:30:00",
    "uploadedBy": "shashank"
  }
]
```

**DELETE /api/files/{id}**
```
204 No Content  — deleted successfully
403 Forbidden   — file belongs to another user
404 Not Found   — file does not exist
```

---

## Security Model

Three layers working together:

**Layer 1 — Frontend JS (bypassable)**  
Protected pages check for a JWT token in `localStorage` on load. No token → redirect to login. This layer can be bypassed by disabling JavaScript or calling APIs directly. It is a UX protection, not a security guarantee.

**Layer 2 — Spring Security + JWT Filter (server-side enforced)**  
`JwtAuthFilter` intercepts every API request before it reaches any controller. Requests without a valid JWT are rejected at the filter level. JWT tokens expire after 24 hours.

**Layer 3 — File Ownership Check (server-side enforced)**  
Even a valid JWT token is not enough to access a file. `FileService.getFileEntity()` verifies that the logged-in username matches `file.uploadedBy` before every download or delete. Returns `403 FORBIDDEN` if they do not match.

**Note on localStorage:** JWT is stored in `localStorage`, which is accessible to JavaScript and vulnerable to XSS attacks. The more secure approach is `HttpOnly` cookies which JavaScript cannot read. This is a known tradeoff and a planned improvement.

**Note on CORS:** CORS is currently disabled in `SecurityConfig`. This is acceptable for a same-origin setup but would need proper configuration if a separate frontend domain were used.

---

## Database Schema

Tables are auto-created by Hibernate (`ddl-auto=update`) on first startup.

**users**

| Column | Type | Notes |
|---|---|---|
| id | BIGINT | Primary key, auto-increment |
| username | VARCHAR | Unique, not null |
| email | VARCHAR | Unique, not null |
| password | VARCHAR | BCrypt hashed, not null |
| role | ENUM('USER','ADMIN') | Default USER, stored as string |
| storage_limit | BIGINT | Default 1073741824 (1GB in bytes) |
| storage_used | BIGINT | Default 0 |
| is_active | BOOLEAN | Default true |
| created_at | DATETIME | Set on creation |

**files**

| Column | Type | Notes |
|---|---|---|
| id | BIGINT | Primary key, auto-increment |
| file_name | VARCHAR | Original filename, not null |
| s3_key | VARCHAR | Full S3 path — username/uuid_filename |
| file_type | VARCHAR | MIME type |
| file_size | BIGINT | Size in bytes |
| uploaded_by | BIGINT | Foreign key → users.id, not null |
| uploaded_at | DATETIME | Set on upload |
| is_public | BOOLEAN | Default false |

**Note on cascade behavior:** The `uploaded_by` foreign key has no cascade rule defined. If a user deletion is attempted while they have files, MySQL will reject it with a foreign key constraint error. User deletion is not currently implemented — this is a known limitation.

---

## Project Structure

```
com.nimbusdrive
├── config
│   ├── SecurityConfig.java       ← Spring Security rules, JWT filter registration
│   └── S3Config.java             ← AWS S3 client bean
├── controller
│   ├── AuthController.java       ← /api/auth/** endpoints
│   ├── FileController.java       ← /api/files/** endpoints
│   └── TestController.java       ← /api/test protected endpoint
├── dto
│   ├── RegisterRequest.java      ← Registration input
│   ├── LoginRequest.java         ← Login input (username or email field)
│   ├── FileResponse.java         ← API output — no internal entity fields exposed
│   └── FileDownloadResult.java   ← Carries filename + contentType + bytes together
├── filter
│   └── JwtAuthFilter.java        ← Validates JWT on every request
├── model
│   ├── User.java                 ← users table
│   └── FileEntity.java           ← files table, ManyToOne → User
├── repository
│   ├── UserRepository.java       ← findByUsername, findByEmail
│   └── FileRepository.java       ← findByUploadedBy(User)
├── service
│   ├── AuthService.java          ← Register + Login logic
│   ├── FileService.java          ← Coordinates S3Service + FileRepository
│   └── S3Service.java            ← Direct AWS S3 operations
└── util
    └── JwtUtil.java              ← Generate, validate, extract JWT

src/main/resources/static/
├── login.html
├── register.html
├── dashboard.html
└── upload.html
```

---

## Running Locally

**Prerequisites:** Java 23, Maven, MySQL 8.0, AWS account with S3 bucket

Set these environment variables:

```
DATABASE_URL               = jdbc:mysql://localhost:3306/nimbusdrive
NIMBUSDRIVE_DB_USERNAME    = your_mysql_username
NIMBUSDRIVE_DB_PASSWORD    = your_mysql_password
NIMBUSDRIVE_JWT_SECRET_KEY = your_jwt_secret_min_32_chars
NIMBUSDRIVE_AWS_ACCESS_KEY = your_aws_access_key
NIMBUSDRIVE_AWS_SECRET_KEY = your_aws_secret_key
NIMBUSDRIVE_AWS_BUCKET     = your_s3_bucket_name
NIMBUSDRIVE_AWS_REGION     = ap-south-1
```

```bash
cd nimbusdrive
mvn spring-boot:run
```

Open `http://localhost:8080/login.html`

---

## Known Limitations

| Limitation | Current Behavior | Better Approach |
|---|---|---|
| No input validation | Empty fields accepted at API level | `@Valid` + `@NotBlank` on request DTOs |
| No file type restrictions | Any file can be uploaded | MIME type whitelist in FileService |
| No file size limit | Unlimited file size | Max size check before S3 upload |
| No global exception handler | Spring default error responses | `@ControllerAdvice` for standardized errors |
| JWT in localStorage | XSS vulnerable | `HttpOnly` cookies |
| No pagination | Returns all files at once | `Pageable` on GET /api/files |
| Backend streams file bytes | App server is bottleneck at scale | Presigned URLs — client uploads directly to S3 |
| No user deletion | MySQL rejects due to FK constraint | Cascade delete with S3 cleanup, or soft delete |
| No rate limiting | Login brute force possible | Spring rate limiter or API gateway |
| No cross-service transaction handling | S3 and database operations can become inconsistent on failure | Use compensating actions or distributed transaction patterns |

---
## Future Architecture Improvement

### Current Approach
Backend handles file upload and download.

### Planned Improvement
- Generate presigned URLs from backend
- Client uploads directly to S3
- Backend stores only metadata

### Benefits
- Reduced server load
- Improved scalability
- Industry-standard architecture

## Roadmap

**High Priority**
- 🔜 Global exception handler (`@ControllerAdvice`)
- 🔜 Input validation (`@Valid`, `@NotBlank`)
- 🔜 File type and size restrictions
- 🔜 Pagination on file list API

**Planned Features**
- 🔜 File sharing — unique token, optional expiry, revocable
- 🔜 Two-Factor Authentication — email OTP, per-user toggle
- 🔜 File versioning — max 10 versions, restore capability
- 🔜 Server-side encryption (AES-256)
- 🔜 Storage quota UI — 1GB per user
- 🔜 Admin panel

---

*Built by Shashank Zarikar — 6th Semester, Computer Science*