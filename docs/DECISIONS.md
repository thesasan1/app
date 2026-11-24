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

- **Status:** Accepted, implemented in `:core:common` (ADR-017)
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
  - minSdk 26, compileSdk/targetSdk 35, JDK 17 toolchain. (Superseded by ADR-019: compileSdk 37, targetSdk 36, JDK 21.)
  - Toolchain set known to be mutually compatible: Gradle 8.11.1, AGP 8.7.3, Kotlin 2.1.0 (K2, Compose compiler plugin), KSP 2.1.0-1.0.29, Hilt 2.53.1, Compose BOM 2024.12.01, Navigation 2.8.5 (type-safe routes), Lifecycle 2.8.7.
  - JUnit 5 for local unit tests (parameterized tests, clearer assertions); MockK and Turbine available.
  - `:core:common` is a pure Kotlin/JVM module (no Android), so `Outcome`/`AppError` and dispatcher qualifiers are reusable by domain code.
  - Convention plugins: `app.android.application`, `app.android.library`, `app.android.compose`, `app.android.feature`, `app.hilt`, `app.jvm.library`.
- **Alternatives:** Waiting for owner input (rejected: blocks all of Phase 1 for a reversible choice); newest-available library versions (deferred: bump deliberately with Dependabot once CI is green, targetSdk 36 tracked in KI-005).
- **Consequences:** Changing the applicationId later is a one-line change plus package rename before release; after release it is effectively permanent.

### ADR-014 — Backend skeleton conventions

- **Status:** Accepted (agent decision, 2026-11-24)
- **Decision:**
  - NestJS 11 on Node.js 22, TypeScript 5.7 `strict` + `noUncheckedIndexedAccess`, CommonJS output.
  - Environment validated with **zod** at startup; invalid config stops the process.
  - Logging with **nestjs-pino**; request id from a valid `x-request-id` header or a new UUID, echoed in the response and in problem bodies; authorization, cookies and token-like fields redacted; request bodies never logged.
  - All errors as RFC 9457 `application/problem+json` with a stable `code`; unknown errors return a generic 500 and are logged server-side only.
  - Global `ValidationPipe` (whitelist, forbid unknown, transform) with field-level errors.
  - `helmet` security headers, `x-powered-by` disabled, 1 MB JSON limit, CORS off unless `CORS_ORIGINS` is set.
  - Swagger UI and JSON only outside production; committed `openapi/openapi.v1.json` checked for drift in CI.
  - Tooling: ESLint 10 flat config with type-aware `typescript-eslint`, Prettier, Jest 29 + ts-jest, supertest.
- **Alternatives:** `@nestjs/config` + Joi (zod chosen for typed inference shared with future DTO/AI schema work); Fastify adapter (Express kept for ecosystem compatibility; revisit if throughput requires).

### ADR-015 — Design-system foundation implementation

- **Status:** Accepted (agent decision, 2026-11-24)
- **Decision:**
  - Tokens live in `:core:designsystem` under `theme/`: `AppColors` (semantic roles), six `AccentColor` options, `AppTypography`, `Spacing`, `Radii`, `Motion`. Feature code reads `AppTheme.colors` and `AppTheme.typography`; the Material 3 scheme is derived from the same tokens so stock components match.
  - Accent values for Ocean, Emerald, Amber, Rose and Graphite are new (Indigo is from DESIGN_SYSTEM §2.1). All pass 4.5:1 against on-accent text and 3:1 against the canvas; `ColorContrastTest` enforces this.
  - Fonts bundled as variable TTFs from google/fonts (Inter, Vazirmatn; SIL OFL 1.1, licenses in `third_party/licenses/`). Locale picks the family: `fa`/`ar`/`ur` use Vazirmatn (which also carries Latin glyphs) with +2sp line height; everything else uses Inter. Adds about 1.1 MB to the APK.
  - Screenshot testing: **Roborazzi 1.39 on Robolectric 4.14 (native graphics)**, JUnit 4 tests run through the JUnit Platform vintage engine. Chosen over Paparazzi for its Robolectric base (shared with future UI tests) and active Compose support.
  - Icon family (Lucide vs Material Symbols) is deferred to the first PR that needs icons.
- **Alternatives:** downloadable Google Fonts (rejected: needs Play services and network on first run, conflicts with offline-first); Paparazzi.

### ADR-016 — Kotlin formatting and static analysis

- **Status:** Accepted (agent decision, 2026-11-24)
- **Decision:**
  - **Spotless 7.2 + ktlint 1.5** formats every module's Kotlin sources and module build scripts. The style is `android_studio` with a 120-column limit, trailing commas allowed, and Composable functions exempt from function-naming rules. Settings live in the root `.editorconfig` so IDEs agree.
  - **detekt 1.23.8** runs on main and test sources with `buildUponDefaultConfig` and overrides in `android/config/detekt/detekt.yml` (Compose-friendly naming, length limits and magic numbers). detekt's classpath is pinned to Kotlin 2.0.21, the version it is built with, so the project's Kotlin 2.1 does not break it.
  - Both are applied by the existing convention plugins (`configureStaticAnalysis()`), so new modules get them automatically. CI runs `spotlessCheck detekt` before the unit tests.
  - **Dependency review** (`actions/dependency-review-action`) on every PR fails on moderate or worse vulnerabilities and on GPL/AGPL licenses. **Dependabot** opens weekly grouped updates for Gradle, npm and GitHub Actions.
- **Alternatives:** the ktlint Gradle plugin (Spotless also covers other formats later); detekt 2.0 alpha (not stable).
- **Revisit:** when detekt 2.0 is stable, drop the Kotlin pin.

### ADR-017 — Local database foundation

- **Status:** Accepted (agent decision, 2026-11-24)
- **Decision:**
  - **Room 2.7.2 with KSP**, in a new `:core:database` module, set up by the `app.android.room` convention plugin. Kotlin code generation is on. (Upgraded to Room 2.8.5 with ADR-019; schema unchanged.)
  - **Schemas are exported** to `android/core/database/schemas/` and committed. CI fails when the exported schema differs from the committed one.
  - **Migrations:** there is no destructive fallback. Every version bump adds a `Migration` to `DatabaseMigrations` and a case to `DatabaseMigrationTest` (Room `MigrationTestHelper` on Robolectric).
  - **Version 1 tables:** `projects`, `tags`, `categories` and the sync support tables `sync_outbox` and `sync_cursor` (DATABASE.md §3.8, §5). Feature tables arrive in Phase 2.
  - **Sync columns:** `created_at`, `updated_at` and `deleted_at` are UTC epoch milliseconds. Also stored: server `version`, `sync_state` and `last_synced_version`, all through the embedded `SyncColumns`. The hybrid logical clock for cross-device ordering (ADR-005) will be its own column, added with the sync engine. This refines DATABASE.md §1, which described `updated_at` as an HLC string.
  - **Tags** are unique by `name_key` (trimmed, lower-cased with the root locale). The unique index includes tombstones, so re-creating a deleted tag restores the old row.
  - **Atomic writes:** repositories write a change and its outbox entry through `TransactionRunner` (one Room transaction).
  - **IDs:** `UuidV7Generator` in `:core:common` (RFC 9562), injected as `IdGenerator`. ADR-004 is now implemented.
- **Not decided here:** at-rest encryption (ADR-010) stays open for the owner.

### ADR-018 — Error codes and logging

- **Status:** Accepted (agent decision, 2026-11-24)
- **Decision:**
  - App code logs only through `AppLogger` (`:core:common`). Messages are lambdas, so nothing is built when a level is off.
  - `RedactingLogger` masks emails, bearer tokens and JWTs, secret key/value pairs, long opaque strings, and 7+ digit numbers (Latin, Persian and Arabic-Indic digits) before anything reaches a sink. UUIDs are kept.
  - Logs carry ids, counts, codes and states, never user content. Redaction is a safety net, not permission.
  - Debug builds log at DEBUG with stack traces. Release builds log WARN+ without throwables, since exception messages can contain user input. No crash reporting or remote logging until D-05 (telemetry) is decided.
  - `AppError.code` gives a short, content-free code (`OFFLINE`, `HTTP_503`, `NETWORK:IOException`, `VALIDATION:title`) for logs, metrics and the outbox's `last_error`.
- **Alternatives:** Timber (adds a dependency and no redaction); logging raw throwables (leaks content).

### ADR-019 — Android toolchain upgrade (AGP 9)

- **Status:** Accepted (agent decision, 2026-11-24)
- **Context:** The skeleton's toolchain (AGP 8.7, Kotlin 2.1) blocked current AndroidX releases: Dependabot #11, #15, #16 and #18 all failed CI. Google Play also requires a newer targetSdk (KI-005).
- **Decision:**
  - Gradle 9.9.2, AGP 9.5.2 with built-in Kotlin (the `org.jetbrains.kotlin.android` plugin is no longer applied), Kotlin 2.4.21, KSP 2.3.14.
  - compileSdk 37 (current AndroidX releases require it), targetSdk 36, minSdk 26. compileSdk only unlocks APIs; targetSdk 37 behaviour is a separate later bump.
  - Libraries: Hilt 2.60.1, Compose BOM 2026.11.01, Navigation 2.10.2, hilt-navigation-compose 1.4.0, Lifecycle 2.11.1, Activity 1.13.0, Core 1.19.1, Room 2.8.5, coroutines 1.11.0, serialization 1.13.0, Robolectric 4.18.1, Roborazzi 1.79.0, Spotless 8.12.0.
  - JDK 21 toolchain, still emitting Java 17 bytecode. Robolectric's SDK 36+ runtimes need JDK 21, and CI uses JDK 21.
  - AGP 9 DSL: `CommonExtension` has no type parameters, and nested blocks are configured through their properties (`defaultConfig.apply { }`).
  - Unchanged: ktlint 1.5.0 (no reformat churn), and detekt 1.23.8 with its Kotlin 2.0.21 classpath pin (ADR-016).
- **Consequences:** Contributors need JDK 21 and SDK platform 37. targetSdk 36 brings Android 16 behaviour (predictive back, large-screen orientation and resizability are no longer optional), which future UI work must respect.

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
