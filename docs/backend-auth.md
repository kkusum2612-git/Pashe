# Authentication backend

## Local start and test account

From the repository root in PowerShell:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/Start-Local.ps1
```

The script creates `.env` only if absent, generating private database and JWT secrets, starts PostgreSQL (5433) and Mailpit (SMTP 1025, inbox http://localhost:8025), then runs Spring Boot (8080). Existing `.env` is preserved; add missing properties using `.env.example`. Start Docker Desktop first. Start the frontend separately with `npm.cmd run dev` in `frontend`; open http://localhost:5173/#/auth/login.

On the first development launch with `SEED_TEST_USER=true`, the verified USER account `tester@pashe.local` is inserted. The generated local configuration uses password `Pashe-Local-Test-2026!`. This is a deliberately public local test credential, not suitable for deployment. Seeding never overwrites an existing account. Disable seeding outside local testing; the seeder only exists in the `dev` profile.

Run backend integration tests with `scripts/Start-Local.ps1 -Test`. Tests use real PostgreSQL, roll back their account data, and mock email delivery. Rate-limit counters commit independently. The persistent named database volume is retained. Never use `docker compose down -v` to reset authentication.

`scripts/Test-LocalAuth.ps1` exercises the seeded account over real HTTP and SMTP, resetting its password to the same local test value. It requires the backend already running. Validation on 2026-11-17: eight backend tests passed, eight frontend tests passed, frontend build/lint passed, Flyway V1–V5 succeeded, and this HTTP/SMTP smoke test passed. WebSocket configuration compiled and loaded in the test application; a real STOMP client exchange has not been tested yet.

## Module and authentication model

`identity` owns registration, UserService/profile, email tokens, JWT issuance, refresh sessions, authorization, rate limits, seeding and scheduled cleanup. It uses Spring JDBC with explicit transactions and PostgreSQL row locks against Flyway-owned schema. `realtime` owns the authenticated WebSocket/STOMP foundation. `shared/health` remains public. Business SOS timers/escalation workflows are future domain work; Spring Scheduling currently removes expired authentication records hourly.

Access JWT: HS256, issuer `pashe`, 15-minute expiry, subject=user UUID, `sid`=session UUID. `JWT_SECRET` must be Base64 encoding at least 32 random bytes. Browser tokens use HttpOnly, SameSite=Strict cookies; Secure defaults true and is disabled only in the dev profile. API clients can supply an access JWT in Authorization: Bearer. Mutation requests still require CSRF. No token is exposed to frontend storage.

Refresh tokens are random 256-bit opaque values stored only as SHA-256 hashes. Sessions have an absolute seven-day lifetime. Refresh rotates under a row lock; reuse revokes the whole session family. Access validation checks session revocation, current user enabled/verified flags and current roles on every request. Logout revokes the refresh cookie's session; password reset revokes all sessions. Browser clients must serialize refreshes; simultaneous refreshes from multiple tabs can trigger reuse protection and require signing in again.

Passwords use BCrypt cost 12, minimum 8 characters, maximum 72 UTF-8 bytes. All passwords pass byte-length validation before hashing. Registration grants USER only. Email is case-normalized and constrained by a case-insensitive unique index. Profile updates allow only name/phone; roles and verified email cannot be changed through profile requests.

## API

All routes use `/api/v1`. User response: `{id, fullName, email, phone, emailVerified, roles}`. Obtain GET `/auth/csrf`, retain its cookie, and send returned `token` as `X-CSRF-TOKEN` on mutations. Retain response cookies. Spring Security sends no-cache headers.

| Method | Route | Request / result |
| --- | --- | --- |
| POST | `/auth/register` | fullName, email, phone, password; 204, sends verification |
| POST | `/auth/login` | email, password; user JSON, access/refresh cookies |
| GET | `/auth/session` | current user or 401 |
| POST | `/auth/refresh` | refresh cookie; rotated cookies and user JSON |
| POST | `/auth/logout` | refresh cookie; revoke and clear cookies, 204 |
| POST | `/auth/verify-email` | token; 204 |
| POST | `/auth/resend-verification` | email; generic 204 |
| POST | `/auth/forgot-password` | email; generic 204 |
| POST | `/auth/reset-password` | token, password; 204 |
| GET/PATCH | `/users/me` | read profile / update fullName and phone |
| GET | `/admin/access` | ADMIN authorization check |
| GET | `/ambulance/access` | AMBULANCE_OPERATOR authorization check |
| GET | `/pharmacy/access` | PHARMACY_STAFF authorization check |

Verification links last 24 hours; reset links last 30 minutes. Tokens are one-use hashes, older links are invalidated on resend. Links use frontend hash routes. SMTP delivery is synchronous and database changes roll back on delivery failure. For production delivery reliability, a transactional outbox/retry worker remains an operational enhancement.

Rate limits are PostgreSQL-backed and survive process restarts: 60 account attempts per source IP per 15 minutes, 10 logins per normalized email, 3 registration/verification/reset requests per email. Rejections return 429 and Retry-After. Forwarded IP headers are not trusted; configure a trusted proxy strategy before running behind a shared proxy, otherwise users share its IP limit. Generic responses reduce account disclosure but do not guarantee identical timing across email-delivery branches.

## WebSocket/STOMP

Native WebSocket endpoint `/api/v1/ws` requires the access cookie or bearer JWT at handshake and exact configured FRONTEND_URL Origin. First obtain `/auth/csrf`; include `X-CSRF-TOKEN` in the STOMP CONNECT frame. Subscribe only to `/user/queue/account`; SEND to `/app/account/ping` returns a private status response. Other destinations are denied. Every non-disconnect command rechecks JWT expiry and the database session. Reconnect after refreshing access tokens. No sensitive broadcasts or domain events exist yet. Production deployments with multiple instances need an external broker when domain messaging is added.

## Deployment

Use HTTPS, a persistent private JWT key, production SMTP with authentication/TLS, an exact frontend origin, and a same-origin `/api` reverse proxy with WebSocket upgrade support. Do not expose Mailpit publicly, enable dev seeding, or deploy the dev profile. No permissive CORS configuration is added. Additional domain authorization must be added explicitly; unrecognized routes are denied by default.
