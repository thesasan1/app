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

### ADR-012 — Agent merge authority

- **Status:** Accepted (project-owner instruction, 2026-11-24)
- **Context:** `AGENTS.md` §12.2 reserves merging for the project owner. On 2026-11-24 the owner instructed: "from now on, you can merge and handle all pull requests."
- **Decision:** The agent may mark PRs ready, merge them (squash), and manage them, once CI is green (where CI exists) and the PR description is complete. All other §12.2 restrictions still apply: no direct pushes to `main`, no bypassing branch protection or required checks, no force-pushing shared branches, no permission or security-setting changes, no production release without owner approval.
- **Consequences:** The owner can revoke this at any time; PRs remain the review record.

### ADR-013 — Android project skeleton baseline

- **Status:** Accepted (agent decision under the owner's "continue" instruction, 2026-11-24; reversible)
- **Context:** D-01 and D-02 were unanswered; Phase 1 cannot start without some value. Both are cheap to change before the first release.
- **Decision:**
  - `applicationId`/namespace `com.thesasan1.app` (debug suffix `.debug`), display name "Personal OS" / «سیستم‌عامل شخصی» — provisional.
  - minSdk 26, compileSdk/targetSdk 35, JDK 17 toolchain.
  - Toolchain set known to be mutually compatible: Gradle 8.11.1, AGP 8.7.3, Kotlin 2.1.0 (K2, Compose compiler plugin), KSP 2.1.0-1.0.29, Hilt 2.53.1, Compose BOM 2024.12.01, Navigation 2.8.5 (type-safe routes), Lifecycle 2.8.7.
  - JUnit 5 for local unit tests (parameterized tests, clearer assertions); MockK and Turbine available.
  - `:core:common` is a pure Kotlin/JVM module (no Android), so `Outcome`/`AppError` and dispatcher qualifiers are reusable by domain code.
  - Convention plugins: `app.android.application`, `app.android.library`, `app.android.compose`, `app.android.feature`, `app.hilt`, `app.jvm.library`.
- **Alternatives:** Waiting for owner input (rejected: blocks all of Phase 1 for a reversible choice); newest-available library versions (deferred: bump deliberately with Dependabot once CI is green, targetSdk 36 tracked in KI-005).
- **Consequences:** Changing the applicationId later is a one-line change plus package rename before release; after release it is effectively permanent.

---

## Pending Decisions (owner input required)

| ID | Decision | Recommendation | Blocks |
|---|---|---|---|
| D-01 | Product name and `applicationId` | **Provisional:** `com.thesasan1.app`, display name "Personal OS" (ADR-013). Must be final before the first Play upload | First release |
| D-02 | minSdk | **Adopted provisionally: 26** (ADR-013); owner may still change it before release | — |
| D-03 | AI provider(s) (paid) | Start with one major LLM API behind the adapter; choose by Persian quality, cost, data-retention terms | Phase 5 |
| D-04 | Cloud hosting (API, Postgres, Redis, object storage, KMS) | Managed Postgres + container hosting from one provider | Phase 6 (staging earlier) |
| D-05 | Crash reporting / telemetry | Privacy-respecting, opt-in; e.g., self-hosted Sentry or Firebase Crashlytics (data-sharing implications) | Phase 1 (abstraction only), Phase 6 |
| D-06 | Sign-in methods | Email+password and Google Sign-In; passkeys later | Phase 6 |
| D-07 | Speech-to-text | On-device `SpeechRecognizer` first (offline where available), server STT fallback for Persian quality | Phase 3 |
| D-08 | Monetization | Affects AI quotas and feature gating | Phase 5 |
| D-09 | Google Play services dependency (geofencing, Credential Manager) | Accept, with graceful degradation on devices without Play services | Phase 2 (location reminders) |
