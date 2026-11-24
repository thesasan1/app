# Decisions Log

Architecture Decision Records (ADRs) and decisions pending the project owner. Format: context → decision → alternatives → consequences. Status: `Proposed` (awaiting owner approval via the PR that introduces it), `Accepted`, `Superseded`.

---

## Accepted / Proposed ADRs

### ADR-001 — Core stack as specified in AGENTS.md

- **Status:** Proposed (Phase 0 PR)
- **Decision:** Android: Kotlin, Jetpack Compose, Material 3 + custom design system, Hilt, Room, DataStore, Navigation Compose, WorkManager, Retrofit/OkHttp, Kotlin Serialization, Coil. Backend: Node.js LTS, TypeScript, NestJS, PostgreSQL, Redis (jobs/rate limits).
- **Consequences:** No deviation without a new ADR.

### ADR-002 — Modular monolith backend

- **Status:** Proposed
- **Context:** `AGENTS.md` §4.3 forbids premature microservices.
- **Decision:** One NestJS codebase with domain modules; two processes (API, worker) from the same image.
- **Alternatives:** Microservices (rejected: operational cost), serverless functions (rejected: long-running sync/AI jobs, cold starts).

### ADR-003 — Feature-oriented Gradle modularization

- **Status:** Proposed
- **Decision:** `:core:*` and `:feature:*` modules with convention plugins in `build-logic`; features never depend on each other. Layers inside a feature are packages, not separate modules (split later if needed).
- **Alternatives:** Single module (rejected: build times, boundaries); 3 modules per feature (rejected for now: overhead).

### ADR-004 — Client-generated UUIDv7 identifiers

- **Status:** Proposed
- **Decision:** IDs created on device so offline creation needs no server round trip; v7 for index locality.
- **Consequences:** Server must validate format and ownership; collisions practically impossible.

### ADR-005 — Transactional outbox + push/pull sync with HLC

- **Status:** Proposed
- **Decision:** Outbox written in the same Room transaction as the data; server-assigned versions; Hybrid Logical Clocks for ordering; field-level merge, conflict copies for note bodies, append-only events for habits (ARCHITECTURE §3).
- **Alternatives:** CRDTs everywhere (rejected for v1: complexity, payload size; may revisit for rich-text bodies); pure LWW (rejected: silent data loss).

### ADR-006 — AI only through backend, provider-independent

- **Status:** Proposed
- **Decision:** `LlmProvider` adapter interface; allow-listed tool registry; schema-validated outputs; on-device deterministic fallbacks.
- **Consequences:** AI features need connectivity and an account; core app does not.

### ADR-007 — Backend ORM/migrations: Prisma (recommended)

- **Status:** Proposed — needs owner approval
- **Alternatives:** TypeORM (mature in Nest ecosystem, weaker typing, migration drift risk), Drizzle (lightweight, SQL-first, younger), Kysely + raw migrations.
- **Rationale:** Strong type safety, reliable migration workflow, good Postgres support. Re-verify maintenance status at Phase 1.

### ADR-008 — REST + OpenAPI with URI versioning

- **Status:** Proposed
- **Decision:** `/v1` prefix; RFC 9457 errors; cursor pagination; idempotency keys.
- **Alternatives:** GraphQL (rejected: sync is the main channel, REST suits it; simpler caching/auth).

### ADR-009 — Rich-text storage as portable JSON block model

- **Status:** Proposed
- **Decision:** Note bodies stored as a versioned JSON block list (paragraph, heading, list, checklist, quote, code, image ref) with inline marks; `body_plain` derived for search.
- **Alternatives:** HTML (hard to sanitize/diff), Markdown (lossy for some formatting, but readable). Export will offer Markdown.

### ADR-010 — Local database encryption

- **Status:** Open — decide in Phase 1
- **Options:** SQLCipher for Room (stronger at-rest protection, ~5–15% perf cost, APK size) vs. Android file-based encryption + optional app lock.

### ADR-011 — Branch strategy

- **Status:** Proposed
- **Decision:** `main` protected and always releasable; short-lived `feature/*`, `fix/*`, `refactor/*`, `docs/*`, `hotfix/*` branches off `main`; no `develop` branch until parallel release trains justify it. Squash-merge by the owner only.

---

## Pending Decisions (owner input required)

| ID | Decision | Recommendation | Blocks |
|---|---|---|---|
| D-01 | Product name and `applicationId` (e.g., `com.<org>.<name>`) | Choose before Phase 1 (package name is hard to change after release) | Phase 1 PR 1 |
| D-02 | minSdk | **26** (Android 8.0): ~97% device reach, notification channels, adaptive icons. Alternative 28 simplifies some APIs | Phase 1 PR 1 |
| D-03 | AI provider(s) (paid) | Start with one major LLM API behind the adapter; choose by Persian quality, cost, data-retention terms | Phase 5 |
| D-04 | Cloud hosting (API, Postgres, Redis, object storage, KMS) | Managed Postgres + container hosting from one provider | Phase 6 (staging earlier) |
| D-05 | Crash reporting / telemetry | Privacy-respecting, opt-in; e.g., self-hosted Sentry or Firebase Crashlytics (data-sharing implications) | Phase 1 (abstraction only), Phase 6 |
| D-06 | Sign-in methods | Email+password and Google Sign-In; passkeys later | Phase 6 |
| D-07 | Speech-to-text | On-device `SpeechRecognizer` first (offline where available), server STT fallback for Persian quality | Phase 3 |
| D-08 | Monetization | Affects AI quotas and feature gating | Phase 5 |
| D-09 | Google Play services dependency (geofencing, Credential Manager) | Accept, with graceful degradation on devices without Play services | Phase 2 (location reminders) |
