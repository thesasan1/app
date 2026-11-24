# Progress

The GitHub repository is the source of truth. A checkpoint is added at the end of every development unit; older checkpoints are kept.

## Current State

| Field | Value |
|---|---|
| Current phase | **Phase 1 — Engineering Foundation** |
| Status | PR #14 `feature/local-database` in review |
| Code status | Android skeleton (#2), backend skeleton (#3) design-system foundation (#4) and CI hardening (#5) merged. Local database in PR #14 |
| Build status | `android-ci` and `backend-ci` green on `main` after #5 and the Dependabot action bumps |
| Test status | Backend: 13 unit + 12 e2e (CI green). Android: unit tests, lint, screenshots run in CI |
| Blockers | None for Phase 1. D-03…D-09 pending for later phases |
| Next task | Phase 1 PR 5b — `feature/backend-database` (Prisma, docker-compose, worker) |

## Completed Features

- Phase 0 foundation documentation — merged (PR #1).
- Android engineering skeleton + `android-ci` — merged (PR #2).
- Backend skeleton + `backend-ci` — merged (PR #3).
- Design-system foundation — merged (PR #4).
- CI hardening — merged (PR #5).

## Features In Progress

- Local database (Room v1, outbox, migration harness, UUIDv7) — PR #14.

## Merged Pull Requests

- #1 Phase 0: Product and architecture foundation — merged 2026-11-24 (squash `d92d349`).
- #2 Android engineering skeleton + Android CI — merged 2026-11-24 (squash `d69a475`), `android-ci` green.
- #3 Backend skeleton + backend CI — merged 2026-11-24 (squash `bc6aafb`), `backend-ci` and `android-ci` green.
- #4 Design-system foundation — merged 2026-11-24 (squash `8e301d6`), `android-ci` green after one compile fix.
- #5 CI hardening — merged 2026-11-24 (squash `bdb5035`) after the owner enabled the dependency graph.
- Dependabot #6–#10 (GitHub Actions bumps) and #13 (backend lint/test tooling) — merged 2026-11-24 with green CI. #11 and #12 left open (KI-011).

---

## Checkpoints

### Checkpoint 0.1 — 2026-11-24 — Phase 0 foundation submitted

**Completed (pending approval):**

- Inspected repository: contained only `README.md` (title only) and `AGENTS.md`; no code, no CI, single branch `main`.
- Created PRD with stable requirement IDs, architecture (Android modules, backend modules, sync, AI, integrations), initial database model, API contract outline, design-system foundation, roadmap with planned Phase 1 PRs, testing strategy, security model, decisions log (11 ADRs, 9 pending decisions), risks and dependencies.
- Established GitHub workflow: `CONTRIBUTING.md`, PR template, issue templates, `.gitignore`, `.editorconfig`.

**Tested:** Documentation-only change. Markdown files checked for broken relative links (see PR). No builds or tests apply.

**Remains:** Owner review; decisions D-01…D-09; enabling branch protection on `main`.

**Pull Request:** [#1](https://github.com/thesasan1/app/pull/1) — merged 2026-11-24 under ADR-012.

**Recommended next task:** Phase 1 — Android project skeleton with version catalog and convention plugins.

### Checkpoint 1.1 — 2026-11-24 — Android skeleton submitted

**Completed (in review, PR #2):**

- Gradle multi-module Android project in `android/`: version catalog, `build-logic` convention plugins, Gradle 8.11.1 wrapper.
- `:core:common` (pure JVM): `Outcome`, `AppError`, dispatcher qualifiers.
- `:core:designsystem`: base light/dark color schemes from DESIGN_SYSTEM tokens, spacing scale, `AppTheme`.
- `:feature:dashboard`: time-of-day greeting, localized date, honest empty state; EN + FA strings; type-safe route.
- `:app`: Hilt application, edge-to-edge `MainActivity`, `AppNavHost`, `AppModule` (Clock, dispatchers), RTL enabled, backup rules exclude preferences, R8 for release.
- `.github/workflows/android-ci.yml`: unit tests, lint, debug APK artifact.

**Tested:** `./gradlew :core:common:test` → Passed locally (7 tests). `android-ci` on PR #2 → Passed (unit tests, lint, debug APK). Merged 2026-11-24.

**Remains in Phase 1:** backend skeleton, backend CI + static analysis, full design-system foundation, Room + outbox, logging/error foundation.

**Recommended next task:** `feature/backend-skeleton`.

### Checkpoint 1.2 — 2026-11-24 — Backend skeleton submitted

**Completed (merged, PR #3):** NestJS 11 backend in `backend/` with validated config, `/v1/health/live` and `/v1/health/ready`, pino logging (redaction, request ids), RFC 9457 errors incl. body-parser 413, global validation, helmet, OpenAPI generation with drift check, `backend-ci` workflow. Android CI actions bumped to v5.

**Tested (local):** lint, format check, typecheck → Passed; `npm test` → Passed (13); `npm run test:e2e` → Passed (12); `openapi:check` → Passed; `npm audit` → 0 vulnerabilities; production start smoke test → `/v1/health/ready` 200, `/v1/docs` 404 in production.

**Recommended next task:** `feature/design-system-foundation`.

### Checkpoint 1.3 — 2026-11-24 — Design-system foundation submitted

**Completed (merged, PR #4):** semantic color tokens with six accents, Inter/Vazirmatn variable fonts with locale-aware type scale, spacing/radius/motion tokens, `AppTheme` with token accessors, `AppButton` (4 variants, 3 sizes, loading), `AppCard`, `EmptyState`; dashboard moved to tokens; Roborazzi screenshots (light, dark, Persian RTL, 200% font scale) recorded in CI.

**Tested:** contrast and typography unit tests, screenshot rendering → verified in CI only (KI-002). Golden comparison not yet enabled (KI-008).

**Recommended next task:** `feature/ci` — dependency review and Android static analysis.

### Checkpoint 1.4 — 2026-11-24 — CI hardening submitted

**Completed (merged, PR #5):** Spotless + ktlint and detekt for all Kotlin modules through the convention plugins; existing code formatted and detekt findings fixed (`DayPeriod` hour constants, `AppError` line length, justified suppression in `runCatchingOutcome`); `dependency-review` workflow; Dependabot for Gradle, npm and Actions; CONTRIBUTING pre-push checks.

**Tested (local):** `./gradlew spotlessCheck detekt` → Passed; `./gradlew :core:common:test` → Passed. Android unit tests, lint and assemble → verified in CI.

**Recommended next task:** `feature/local-database`.

### Checkpoint 1.5 — 2026-11-24 — Local database submitted

**Completed (in review, PR #14):** `:core:database` with Room 2.7.2 (KSP, exported schema v1): `projects`, `tags`, `categories`, `sync_outbox`, `sync_cursor`; embedded `SyncColumns`; DAOs with soft delete, active queries and trash purge; `TransactionRunner` for atomic change + outbox writes; Hilt `DatabaseModule`; migration harness; CI check that the schema is committed. `UuidV7Generator` in `:core:common`. ADR-017.

**Tested:** `:core:common:test` (incl. 5 UUIDv7 tests), `spotlessCheck`, `detekt` → Passed locally. Robolectric DAO, outbox and migration tests → CI (KI-002).

**Recommended next task:** `feature/backend-database`.
