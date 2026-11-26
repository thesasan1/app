# Development Roadmap

The roadmap is an implementation **sequence**, not a reduction in scope (`AGENTS.md` §14). Each phase is delivered as several focused Pull Requests; each PR is one coherent, testable unit. Status values: `Not started`, `In progress`, `In review (PR #n)`, `Merged`.

| Phase | Name | Status |
|---|---|---|
| 0 | Product and Architecture Foundation | Merged (PR #1) |
| 1 | Engineering Foundation | In progress |
| 2 | Core Productivity | Not started |
| 3 | Smart Capture | Not started |
| 4 | Personal Growth | Not started |
| 5 | AI Assistant | Not started |
| 6 | Backend and Cloud | Not started |
| 7 | Google Integrations | Not started |
| 8 | Advanced Modules | Not started |
| 9 | Production Hardening | Not started |

Backend foundation and security design may start earlier when architecture depends on it (e.g., AI endpoints in Phase 5 need the backend skeleton from Phase 1 and minimal auth).

---

## Phase 0 — Product and Architecture Foundation

PR: #1 (`docs/phase-0-foundation`)

- [x] Review all requirements in `AGENTS.md`
- [x] PRD with requirement IDs (`docs/PRD.md`)
- [x] Architecture, module boundaries (`docs/ARCHITECTURE.md`)
- [x] Initial database model (`docs/DATABASE.md`)
- [x] API contracts outline (`docs/API_SPECIFICATION.md`)
- [x] Design system foundation (`docs/DESIGN_SYSTEM.md`)
- [x] Testing strategy, security model
- [x] Risks, dependencies, decisions log (`docs/DECISIONS.md`, `docs/KNOWN_ISSUES.md`)
- [x] GitHub workflow: branch/PR conventions, PR template, issue templates, `CONTRIBUTING.md`
- [x] Merged 2026-11-24 (owner delegated merging, ADR-012). D-01/D-02 adopted provisionally (ADR-013); D-03…D-09 still pending

**Exit:** PR merged by owner; blocking decisions for Phase 1 (D-01, D-02) answered.

## Phase 1 — Engineering Foundation (planned PRs)

1. `feature/android-skeleton` — Gradle (version catalog, convention plugins in `build-logic`), `:app`, `:core:common`, `:core:designsystem` (base theme), `:feature:dashboard` shell, Hilt, type-safe Navigation, JUnit 5; **includes `android-ci.yml`** (pulled forward from PR 3 so the skeleton is verified by real builds). — Merged (PR #2)
2. `feature/backend-skeleton` — NestJS app, zod-validated config, `/v1` health endpoints, pino logging with redaction and request ids, RFC 9457 filter, global validation, helmet, 1 MB body limit, OpenAPI generation + drift check, Jest unit + supertest e2e, **`backend-ci.yml`**. Worker process and docker-compose (Postgres, Redis, MinIO) move to the PR that introduces the database (PR 5b). — Merged (PR #3)
3. `feature/ci` — dependency review, Android static analysis (detekt/ktlint via Spotless). (`android-ci.yml` landed in PR 1, `backend-ci.yml` in PR 2.) Also adds Dependabot. — Merged (PR #5)
4. `feature/design-system-foundation` — tokens, typography (Inter + Vazirmatn), theme (light/dark, accents, RTL), first components (`AppButton`, `AppCard`, `EmptyState`), Roborazzi screenshot rendering in CI. — Merged (PR #4)
5. `feature/local-database` — Room setup, base entities (projects, tags, categories), migration test harness, outbox table. — Merged (PR #14)
5b. `feature/backend-database` — PostgreSQL via Prisma (ADR-007), migrations, docker-compose for local infra, readiness check, worker process skeleton. — Waiting for owner approval of ADR-007
6. `feature/error-logging-foundation` — `AppError`, `Result`, logger abstraction, dispatchers, clock. (`AppError`, `Outcome`, dispatchers and clock landed with PR #2; this PR adds the redacting logger and error codes.) — Merged (PR #19)
7. `chore/toolchain-upgrade` — AGP 9.5, Kotlin 2.4, compileSdk 37, targetSdk 36, JDK 21 (ADR-019; added to unblock Dependabot and KI-005). — Merged (PR #20)
8. `chore/nestjs-12` — NestJS 12 on Node.js 26 LTS (ADR-020; unblocks Dependabot #12). — Merged (PR #21)
9. `ci/debug-apk-prerelease` — `debug-latest` pre-release for testers (ADR-021). — Merged (PR #22)

**Exit:** green CI on both stacks; documented commands in README.

## Phase 2 — Core Productivity

Notes (NOTE-01…08 offline parts) → Tasks (TASK-01…08) → Reminder engine + UI (REM-01, 02, 06, 07) → Habits core (HAB-01, 02, 06, 07) → Dashboard v1 (DASH-01…03) → Search (PLT-07) → Notifications.
Progress: notes data layer (Room v2 + FTS, ADR-022) — Merged (PR #23); notes repository and document format (ADR-009) — Merged (PR #24); notes list and editor with autosave (NOTE-01) — Merged (PR #27); archive and trash screens (NOTE-03, NOTE-04) — Merged (PR #28); tasks data layer (TASK-01) — Merged (PR #29); tasks repository with history (TASK-05, TASK-08) — Merged (PR #30); tasks list and quick add (TASK-01, TASK-06) — Merged (PR #31); task detail and editor (TASK-02, TASK-03) — Merged (PR #32); completed, archive and trash screens (TASK-07) — Merged (PR #33); reminder storage, Room v4 (ADR-024) — Merged (PR #34); reminder scheduler (ADR-025) — Merged (PR #35); reminder notification actions (ADR-026) — Merged (PR #36); reminder picker on tasks (ADR-027) — Merged (PR #37); clearing stale reminder notifications — Merged (PR #38); custom reminder times and 1-hour snooze — Merged (PR #39); habits data layer, Room v5 (ADR-028) — Merged (PR #40); habits repository and check-in rules (ADR-029) — Merged (PR #41); habits screen — Merged (PR #42); habit streaks and freezes (ADR-030) — Merged (PR #43); habit archive shelf — Merged (PR #44); habit edit (ADR-031) — Merged (PR #45); habit reminder engine (ADR-032) — Merged (PR #46); modern app shell (ADR-033) — Merged (PR #47); habit row tidy-up — In review.

**Exit:** functional offline app for notes, tasks, reminders, habits.

## Phase 3 — Smart Capture

Capture inbox + quick text sheet (CAP-12, CAP-06) → in-app FAB + overlay FAB (CAP-01/02) → Share target (CAP-05) → QS Tile (CAP-04) → Widget (CAP-03) → Voice capture + STT EN/FA (CAP-08/09) → deterministic capture processing EN/FA (CAP-10/11 offline part) → lock-screen shortcuts (CAP-07).

## Phase 4 — Personal Growth

Habit analytics, streaks, heatmap, charts (HAB-03/04) → Achievements/XP (HAB-05, GAM-01) → Goals + milestones (GOAL-01) → Morning planning / evening review (PLAN-01…03) → Focus timer (FOC-01) → Progress dashboard.

## Phase 5 — AI Assistant

Backend AI orchestrator, provider adapter (after D-03), tool registry, validation, usage metering → `/ai/capture/interpret` → conversational assistant UI → context permissions (AI-05) → reports (AI-04) → habit analysis (HAB-08) → smart reminder suggestions (REM-05).
Requires minimal auth from Phase 6 (pulled forward).

## Phase 6 — Backend and Cloud

Auth (D-06) → users/settings → sync push/pull + conflict resolution → Android sync engine → files/attachments → export + account deletion → backup/recovery → monitoring (D-05).

## Phase 7 — Google Integrations

OAuth (server-side, PKCE) → Google Calendar (read events into dashboard/calendar preview, two-way where API allows) → Google Tasks → integration settings/status.

## Phase 8 — Advanced Modules

Journal (private entries need app lock) → Mood → Vision board → Reading & learning → Workflow automation → advanced analytics → extra personalization.

## Phase 9 — Production Hardening

Security review, performance (Baseline Profiles, startup, memory), battery, accessibility audit, UI polish, device/API-level compatibility matrix, migration validation, sync stress tests, backup/restore drills, release pipeline (`release.yml`, signing via secrets), store listing materials, production docs.

---

## Dependencies Between Phases

```
P0 ─► P1 ─► P2 ─► P3 ─► P4
        │     └──────────► P5 (needs P2 entities + backend skeleton + minimal auth)
        └─► backend skeleton ─► P6 ─► P7
P2..P7 ─► P8 ─► P9
```
