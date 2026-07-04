# Repository Guidelines

## Project overview

Small Vite + React 19 + TypeScript 5.9 single-page app with a TypeSpec API definition,
Mantine v8 UI, and a Spring Boot 3.5.14 + Java 17 + SQLite backend in `backend/`.

## Architecture

- **Frontend** (`src/`) — Vite SPA, no router, all state in `App.tsx`.
- **Backend** (`backend/`) — Spring Boot REST API, JPA + SQLite (auto-created `onticoworkshop.db`).
- **API client** (`src/api.ts`) — base URL from `VITE_API_BASE_URL` env var. Falls back to hardcoded demo data when the env var is unset (the default dev mode).

## Commands

| Where | Command | What it does |
|---|---|---|
| Both | `make dev` | Start backend + frontend concurrently |
| Both | `make build` or `make check` | Build both (tsc + vite + mvn) |
| Frontend | `make frontend-dev` or `npm run dev` | Vite dev server at `http://localhost:5173` |
| Frontend | `npm run build` | `tsc -b && vite build` |
| Frontend | `make api-build` or `npm run api:build` | `tsp compile .` → `tsp-output/schema/openapi.yaml` |
| Frontend | `make api-swagger` or `npm run api:swagger` | API build + Swagger UI at `http://127.0.0.1:8080` |
| Backend | `make backend-run` | Start backend at `http://localhost:8080` |
| Backend | `make backend-build` | Build backend JAR |

To connect frontend → backend, set `VITE_API_BASE_URL=http://localhost:8080` in `.env`.

There is no test framework, linter, or formatter configured for the frontend.
`npm run build` is the primary frontend validation step.
Backend uses JUnit + Spring Boot Test (via `mvn test`).

If Maven is behind a custom artifact proxy, use:
```
cd backend && mvn clean package -s /path/to/central-settings.xml
```

## Backend structure

| Package | Purpose |
|---|---|
| `model/` | JPA entities (User, OnlineCallSettings, AvailabilitySchedule, Booking, etc.) |
| `repository/` | Spring Data JPA repositories |
| `service/` | Business logic (slot generation, booking flow) |
| `controller/` | REST endpoints matching the TypeSpec API |
| `dto/` | Request/response shapes |
| `config/` | CORS (allows `localhost:5173`) + seed data runner |

Seed data (user "sofia", a schedule, a few bookings) is auto-inserted on first startup.

## TypeScript notes

- **Project references** — `tsconfig.json` references `tsconfig.app.json` (src code) and `tsconfig.node.json` (vite.config.ts). `tsc -b` compiles both.
- `strict: true`, `noUnusedLocals`, `noUnusedParameters`, `noFallthroughCasesInSwitch` are on.

## Generated / ignored files

- `tsp-output/` — TypeSpec compiler output; never edit by hand.
- `backend/target/` — Maven build output.
- `*.db` / `*.db-*` — SQLite database files.

## Style conventions

- Frontend: 2-space indent, double quotes, semicolons, trailing commas.
- Backend: standard Maven layout, Lombok `@Data` on entities/DTOs, `@RequiredArgsConstructor` for DI.
- Components in PascalCase, functions/variables in camelCase.
- Reuse types from `src/api.ts`; do not duplicate shapes.

## Commit style

Recent commits use Conventional Commits (e.g. `feat: add calendar booking TypeSpec API`).

## Keeping this file and Makefile in sync

When you add, remove, or change a developer workflow (a command, a build step, a test runner,
a codegen step, a deploy target), update **both** `AGENTS.md` and `Makefile` at the same time.
`AGENTS.md` is the agent's primary reference; `Makefile` is the executable shortcut for humans and CI.
If they drift, an agent will guess wrong.
