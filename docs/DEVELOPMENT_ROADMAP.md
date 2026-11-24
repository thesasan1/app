# Development Roadmap

The roadmap is an implementation **sequence**, not a reduction in scope (`AGENTS.md` §14). Each phase is delivered as several focused Pull Requests; each PR is one coherent, testable unit. Status values: `Not started`, `In progress`, `In review (PR #n)`, `Merged`.

| Phase | Name | Status |
|---|---|---|
| 0 | Product and Architecture Foundation | In review (PR #1) |
| 1 | Engineering Foundation | Not started |
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
- [ ] Project-owner approval of pending decisions D-01 … D-09

**Exit:** PR merged by owner; blocking decisions for Phase 1 (D-01, D-02) answered.

## Phase 1 — Engineering Foundation (planned PRs)

1. `feature/android-skeleton` — Gradle (version catalog, convention plugins in `build-logic`), `:app` + core modules, Kotlin/Compose, Hilt, Navigation, empty Dashboard screen; `./gradlew assembleDebug test lint` passing.
2. `feature/backend-skeleton` — NestJS app + worker, config validation, health endpoints, pino logging, Problem Details filter, docker-compose (Postgres, Redis, MinIO), Jest + e2e harness.
3. `feature/ci` — `.github/workflows/android-ci.yml`, `backend-ci.yml` (build, test, lint, type-check, dependency review).
4. `feature/design-system-foundation` — tokens, typography (Inter + Vazirmatn), theme (light/dark, accents, RTL), first components, screenshot tests.
5. `feature/local-database` — Room setup, base entities (projects, tags, categories), migration test harness, outbox table.
6. `feature/error-logging-foundation` — `AppError`, `Result`, logger abstraction, dispatchers, clock.

**Exit:** green CI on both stacks; documented commands in README.

## Phase 2 — Core Productivity

Notes (NOTE-01…08 offline parts) → Tasks (TASK-01…08) → Reminder engine + UI (REM-01, 02, 06, 07) → Habits core (HAB-01, 02, 06, 07) → Dashboard v1 (DASH-01…03) → Search (PLT-07) → Notifications.
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
