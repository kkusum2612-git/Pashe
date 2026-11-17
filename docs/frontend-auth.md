# Authentication frontend

Run `npm.cmd install`, then `npm.cmd run dev` in `frontend`. Open the home page and choose Sign in. The existing landing page and health check remain available at `#/`.

Routes: `#/auth/login`, `register`, `verify`, `forgot`, `reset`, `profile`, `admin`, `ambulance`, `pharmacy` (all under `#/auth/`). Verification and reset emails must link to `/#/auth/verify?token=…` and `/#/auth/reset?token=…`. Tokens are removed from browser history on entry and kept only in memory. Reloading requires reopening the email link.

## Backend integration

The backend now implements this contract with JWT access cookies and rotating refresh cookies. See `backend-auth.md` for startup, test credentials, security details and operational constraints. The frontend retries protected 401 responses once after `/auth/refresh`, with a shared in-flight refresh promise.

All endpoints below are prefixed `/api/v1`. User JSON is `{ id, fullName, email, phone, emailVerified, roles }`. Roles are `USER`, `ADMIN`, `AMBULANCE_OPERATOR`, `PHARMACY_STAFF`.

| Method | Endpoint | Body / response |
| --- | --- | --- |
| GET | `/auth/csrf` | `{ token }`; initialize anonymous session if needed |
| GET | `/auth/session` | User JSON, or 401 for anonymous/expired session |
| POST | `/auth/register` | `{ fullName, email, phone, password }`; 204 |
| POST | `/auth/login` | `{ email, password }`; User JSON and session cookie |
| POST | `/auth/logout` | Invalidate session; 204 |
| POST | `/auth/refresh` | Rotate refresh cookie; User JSON and new access cookie |
| POST | `/auth/verify-email` | `{ token }`; 204 |
| POST | `/auth/resend-verification` | `{ email }`; generic 204 |
| POST | `/auth/forgot-password` | `{ email }`; generic 204 |
| POST | `/auth/reset-password` | `{ token, password }`; invalidate sessions; 204 |
| PATCH | `/users/me` | `{ fullName, phone }`; updated User JSON |

Mutations first retrieve CSRF and send `X-CSRF-TOKEN`. The server uses HttpOnly, Secure (production), SameSite JWT/refresh cookies and invalidates sessions on logout/reset. It enforces verified-email and role authorization; frontend gates are only UI controls. Role assignments cannot be changed through registration/profile. Server validation, rate limits, single-use expiring hashed tokens and generic email-request responses are implemented. Passwords require at least 8 characters and at most 72 UTF-8 bytes (BCrypt limit). Phone validation accepts Bangladesh mobile numbers.

Errors: 400/410 invalid or expired input/link, 401 anonymous or invalid credentials, 403 forbidden/verification required, 409 account conflict, 429 throttled; other failures show service unavailable. No server exception details are exposed. Profile allows name and phone edits; email changes need a separate verified-email-change backend flow.

## PWA and validation

`npm.cmd test` runs the API transport and rendered authentication-screen tests. The UI tests cover registration through the verification prompt and login through profile and role-gated navigation; API calls are mocked there. `npm.cmd run build` type-checks and builds the frontend, and `npm.cmd run lint` checks it. Install frontend dependencies with `npm.cmd ci`.

From the repository root, `powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/Test-Auth.ps1` runs frontend tests, build and lint, then starts the local PostgreSQL/Mailpit services and runs backend authentication integration tests. It requires Docker Desktop; `Start-Local.ps1` creates `.env` only if it is missing, as described in `backend-auth.md`. `scripts/Test-LocalAuth.ps1` remains the live HTTP/SMTP smoke test and requires the backend to already be running.

Preview production with `npm.cmd run preview`; production must reverse-proxy `/api` to the backend. HTTPS or localhost is required for service workers. The generated manifest includes 192/512 PNG icons and a vector icon. Update prompts avoid reloading forms without consent. Supported browsers offer install; on iOS use Share → Add to Home Screen.

Only the static app shell is precached. API navigation is excluded, no account responses are cached, and no offline mutation queue exists. Offline account actions are disabled. No passwords, tokens, profiles or roles are persisted in browser storage. Leaflet/OpenStreetMap is reserved for future location workflows; no map is needed in authentication.

Manual acceptance with an implementing backend: register → receive email → verify → login → reload and restore session → edit profile → logout → protected route denied; request/reset password with valid and expired tokens; check generic unknown-email response; deny wrong roles/direct role URLs; expire server session and refocus; disconnect network; install production PWA and test update prompt. Check keyboard navigation and 320px mobile layout in both languages. These end-to-end checks require the backend contract above.
