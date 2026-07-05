# Repository Guidelines

## Project overview

Small Vite + React 19 + TypeScript 5.9 single-page app with a TypeSpec API definition,
Mantine v8 UI, and a Spring Boot 3.5.14 + Java 17 + SQLite backend in `backend/`.

## Agent rules

### COMMIT AND PUSH — STRICTLY FORBIDDEN WITHOUT APPROVAL

**NEVER run `git add`, `git commit`, `git commit -m`, or `git push` unless the user**
**has explicitly and unambiguously asked for it.** This includes `git commit -m "..." && git push`
in a single command, or any chained command that ends in a push.

If the user says "fix this", "check this", "add this", "update this", "change this",
"investigate", "diagnose", or any similar task description — **do the work, show the
result, then STOP.** Do not commit. Do not push. Wait for an explicit instruction
like "commit", "commit and push", "закоммить", "запушь", etc.

**Even if the user says "go ahead" or "ok" or "do it" after your suggestion —**
**that is NOT permission to commit.** You need a concrete word: commit, push, stage.

Examples of what is NOT permission to commit:
- "fix this" — do the fix, report, wait
- "добавь в AGENTS.md" — make the edit, report, wait
- "ok" / "do it" / "go ahead" — ambiguous, ask for clarification OR treat as permission
  to edit files and run commands, NOT to commit/push
- "looks good" — compliment, not permission
- "yes" — ambiguous, may mean "yes that description is accurate", not "yes commit"

Examples of what IS permission:
- "commit"
- "commit and push"
- "закоммить и запушь"
- "push"
- "stage and commit"

You will receive exactly ONE warning for violating this rule. On the second
violation the session ends and the repo owner gets a summary of every unauthorized
commit.

## Architecture

- **Frontend** (`src/`) — Vite SPA, no router, all state in `App.tsx`.
- **Backend** (`backend/`) — Spring Boot REST API, JPA + SQLite (auto-created `onticoworkshop.db`).
- **API client** (`src/api.ts`) — base URL from `VITE_API_BASE_URL` env var. Falls back to hardcoded demo data when the env var is unset (the default dev mode).
- **Public booking** — meetings have UUID-based public links (`/?meeting={uuid}`). Handled by `PublicBookingPage` component + `PublicController` backend. No auth required.

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
| Docker | `make docker-build` | Build frontend + backend Docker images |
| Docker | `make docker-up` | Start containers (frontend :80, backend :8080) |
| Docker | `make docker-down` | Stop and remove containers |

To connect frontend → backend, set `VITE_API_BASE_URL=http://localhost:8080` in `.env`.

In Docker, nginx serves the frontend and proxies `/api/*` to the backend, so `VITE_API_BASE_URL` is set to `/api` at build time.

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
| `model/` | JPA entities (User, Meeting, MeetingTimeRule, Availability, AvailabilityRule, DateOverride, Booking) |
| `repository/` | Spring Data JPA repositories |
| `service/` | Business logic (slot generation, booking flow, duplicate check) |
| `controller/` | REST endpoints matching the TypeSpec API + `PublicController` for UUID-based access |
| `dto/` | Request/response shapes |
| `config/` | CORS (allows `localhost:5173`) + seed data runner |

Seed data (user "sofia", a meeting, an availability, two bookings) is auto-inserted on first startup.

Domain glossary is in `CONTEXT.md` — read it before working with domain entities.
Architecture decisions are in `docs/adr/`.

## Key business rules

- **Slot generation** — intersection of Availability × MeetingTimeRule, minus existing bookings, minus minimumNoticeMinutes.
- **Duplicate prevention** — one guest can book a meeting only once (checked by meetingId + guestEmail, cancelled bookings excluded).
- **Guest auto-creation** — when a guest books, a User record is auto-created by email if one doesn't exist.
- **Cascade delete** — deleting a Meeting deletes all its Bookings.
- **Delete confirmation** — all destructive operations require modal confirmation.

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

**Always use Conventional Commits** with one of these prefixes: `feat`, `fix`, `docs`, `style`,
`refactor`, `test`, `chore`, `ci`, `build`, `perf` (e.g. `feat: add calendar booking TypeSpec API`).
**Each commit must be a single logical change.** Do not mix unrelated fixes, features,
or refactors in one commit. If a change touches multiple concerns, split it into
separate commits — one per concern.

## Keeping this file and Makefile in sync

When you add, remove, or change a developer workflow (a command, a build step, a test runner,
a codegen step, a deploy target), update **both** `AGENTS.md` and `Makefile` at the same time.
`AGENTS.md` is the agent's primary reference; `Makefile` is the executable shortcut for humans and CI.
If they drift, an agent will guess wrong.

## Documentation discipline

Before committing, update all relevant documentation to reflect the changes:

- **`CONTEXT.md`** — if you added, renamed, or changed the meaning of a domain entity or term
- **`AGENTS.md`** — if commands, architecture, business rules, or file layout changed
- **`README.md`** — if the tech stack, setup steps, env vars, or feature list changed
- **`docs/adr/`** — if you made a hard-to-reverse, surprising, or trade-off-based architectural decision
- **`Makefile`** — if a developer workflow command was added, removed, or changed

If you don't, the next agent (or developer) will operate on stale information and make mistakes.

