# Pashe

## Care Circle and SOS presentation

This branch adds consent-based Care Circle invitations and a self-SOS button with
a cancellable ten-second countdown, guardian-visible incident status and optional
location sharing. Open `http://localhost:5173/#/guardian` after signing in.

Use `scripts/Start-Sos-Demo.ps1` to start the backend in its dedicated presentation
schema, then run `npm.cmd run dev` in `frontend`. Existing database schemas are
preserved. See the [setup and presentation guide](docs/care-circle-sos-demo.md).

Run `scripts/Test-Guardian.ps1` for frontend/build/lint and isolated backend checks.

## PostgreSQL database

PostgreSQL 17 runs locally through Docker Compose. Database settings are read from
environment variables; create a local `.env` from the checked-in example before
starting the database:

```powershell
Copy-Item .env.example .env
# Edit .env and replace DB_PASSWORD with a local-only password.
docker compose up -d postgres
```

Compose binds PostgreSQL to `127.0.0.1:5433` by default, mapping host port
`DB_HOST_PORT` to the container's port `5432`. This avoids conflicting with a
local PostgreSQL service already using host port `5432`. It waits for the
`pg_isready` health check and stores data in the persistent
`pashe_postgres_data` volume.
Inspect container health and logs with:

```powershell
docker compose ps
docker compose logs postgres
```

Connect using the configured container credentials:

```powershell
docker compose exec postgres sh -c 'exec psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"'
```

Stop PostgreSQL without removing its data:

```powershell
docker compose stop postgres
```

Start it again with `docker compose start postgres`, or use
`docker compose down` to stop and remove the container while retaining the
named volume. Do not use `docker compose down -v` unless you intend to delete
the database volume and all data in it.

For an already-initialized database, changing `DB_PASSWORD` in `.env` does not
change the password stored in PostgreSQL. Keep the existing role password or
connect to the database and use `\password pashe` to update it; do not remove
the volume to resolve a credential mismatch.

The backend reads `DB_USERNAME` and `DB_PASSWORD` from the process environment.
For local development, set them in the shell before starting Spring Boot:

```powershell
$env:DB_NAME = "pashe_db"
$env:DB_USERNAME = "pashe"
$env:DB_PASSWORD = "the-value-from-your-local-.env"
$env:DB_HOST_PORT = "5433"
Set-Location backend
.\mvnw.cmd spring-boot:run
```

The same `DB_HOST_PORT` value must be used by Compose and the locally launched
backend. Its default is `5433`; set it to another free host port if needed.

Flyway owns schema creation and applies versioned SQL migrations from
`backend/src/main/resources/db/migration/`. See
[docs/database-schema.md](docs/database-schema.md) for the schema, ER diagram,
and validation status. For the complete setup and live-health guide, see
[DB_GUIDE.md](DB_GUIDE.md). For a codebase overview aimed at external coding
agents, see [Agent_Context.md](Agent_Context.md).

## Authentication tests

Install frontend dependencies with `npm.cmd ci` from `frontend`, then run the
frontend and backend authentication test suite from the repository root:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/Test-Auth.ps1
```

This runs frontend auth UI/API tests, the production build and lint, then
backend authentication integration tests against the local PostgreSQL service.
It requires Docker Desktop; the setup helper creates `.env` only if it is
missing. For a live
HTTP/SMTP check against a running backend, see `scripts/Test-LocalAuth.ps1` and
[docs/backend-auth.md](docs/backend-auth.md).
