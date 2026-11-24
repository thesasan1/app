# Architecture

| Field | Value |
|---|---|
| Status | Draft v0.1 — Phase 0, pending approval. **Nothing below is implemented yet.** |
| Scope | Android app, backend, AI layer, sync, integrations |
| Related | `DATABASE.md`, `API_SPECIFICATION.md`, `SECURITY.md`, `DECISIONS.md` |

---

## 1. System Overview

```
┌─────────────────────────── Android device ───────────────────────────┐
│  Capture surfaces: FAB overlay · Widget · QS Tile · Share · Voice    │
│                │                                                     │
│  UI (Compose, MVVM) ──► Domain (use cases) ──► Data (repositories)   │
│                                          │             │             │
│                                   Room (source of truth)  DataStore  │
│                                          │                           │
│               Sync engine (outbox + WorkManager) ── Reminder engine  │
│                                          │        (AlarmManager)     │
└──────────────────────────────────────────┼───────────────────────────┘
                                           │ HTTPS (TLS 1.2+), JWT
┌──────────────────────────── Backend (NestJS) ────────────────────────┐
│ API v1 ─ Auth ─ Users ─ Sync ─ Files ─ AI Orchestrator ─ Integrations│
│            │        │       │        │            │            │     │
│        PostgreSQL   │   Job queue  Object       LLM provider   Google│
│                     │  (BullMQ/    storage      adapters       APIs  │
│                     │   Redis)     (S3-compat)                       │
└──────────────────────────────────────────────────────────────────────┘
```

Key properties:

- **Local-first.** Every user action commits to Room first; the UI observes Room via `Flow`. The network is an asynchronous replication channel, never on the critical path of core CRUD.
- **Backend is a modular monolith.** One deployable NestJS service with domain modules and a separate worker process for background jobs. No microservices until justified (`AGENTS.md` §4.3).
- **AI is server-side and optional.** The app calls `/v1/ai/*`; the backend calls providers. The app has deterministic fallbacks for every AI-assisted flow.

## 2. Android Architecture

### 2.1 Layers (Clean Architecture + MVVM)

| Layer | Contents | Depends on |
|---|---|---|
| **UI** (`feature:*:ui`) | Compose screens, `ViewModel`s exposing `StateFlow<UiState>`, one-off events via `Channel` | domain |
| **Domain** (`core:domain`, `feature:*:domain`) | Pure Kotlin: entities/models, use cases, repository interfaces, business rules (recurrence, streaks, conflict rules) | nothing Android |
| **Data** (`core:data`, `core:database`, `core:network`, `feature:*:data`) | Repository implementations, Room DAOs, Retrofit services, mappers, sync outbox | domain |

Rules:

- Domain has **no** Android or framework imports → fully JVM unit-testable.
- ViewModels never touch DAOs or Retrofit directly; only use cases.
- Unidirectional data flow: `UiEvent → ViewModel → UseCase → Repository → Room → Flow → UiState`.
- All I/O on injected `CoroutineDispatcher`s (`@IoDispatcher`, `@DefaultDispatcher`) to keep tests deterministic.

### 2.2 Module Structure (Gradle)

```
:app                         Application, MainActivity, NavHost, DI graph root
:build-logic                 Convention plugins (android-library, compose, hilt, room, test)

:core:common                 Result types, dispatchers, clock abstraction, ids (UUIDv7)
:core:domain                 Shared domain models (Tag, Project, Attachment, Recurrence), shared use cases
:core:database               Room DB, DAOs, migrations, type converters
:core:datastore              Preferences + Proto DataStore (settings, session)
:core:network                Retrofit, OkHttp, auth interceptor, serialization, error mapping
:core:data                   Shared repositories (tags, projects, attachments, search)
:core:sync                   Outbox, SyncWorker, pull/push protocol, conflict resolution
:core:designsystem           Theme, tokens, typography (EN/FA), components, motion
:core:ui                     Shared UI helpers (state containers, previews, a11y utils)
:core:notifications          Channels, builders, notification actions
:core:reminders              Scheduling engine (AlarmManager/WorkManager), reboot/time-change receivers
:core:security               Keystore wrappers, app lock (biometric/PIN), encrypted prefs
:core:ai                     AI client (backend), deterministic NLP fallbacks (date parsing EN/FA)
:core:analytics              Opt-in telemetry abstraction (no-op until D-05 decided)
:core:testing                Fakes, test dispatchers, Room in-memory helpers, Compose test rules

:feature:capture             FAB overlay service, quick capture sheet, voice capture, share target, tile, widget
:feature:notes
:feature:tasks
:feature:reminders           Reminder UI (engine lives in :core:reminders)
:feature:habits
:feature:dashboard
:feature:planning            Morning planning / evening review
:feature:assistant           Conversational AI UI
:feature:goals
:feature:focus
:feature:journal
:feature:mood
:feature:vision
:feature:learning            Reading + learning tracker
:feature:achievements
:feature:automation
:feature:integrations        Google account linking UI, status
:feature:settings            Theme, language, lock, data export, account deletion
:feature:onboarding
:feature:search
```

Each feature module is internally split by package (`ui`, `domain`, `data`) rather than three Gradle modules each, to keep build graph manageable; split further only when a feature grows large. Feature modules **must not depend on each other**; cross-feature navigation goes through route contracts in `:core:ui` (`navigation` package), cross-feature data through `:core:domain` interfaces.

### 2.3 Dependency Injection

Hilt. Each module exposes `@Module @InstallIn(SingletonComponent::class)` bindings for its repository interfaces. Workers use `@HiltWorker`. Test modules replace bindings with fakes from `:core:testing`.

### 2.4 Navigation

Navigation Compose with **type-safe routes** (Kotlin Serialization `@Serializable` route objects, Navigation 2.8+). Top-level destinations: Dashboard, Tasks, Notes, Habits, More. Capture is a global bottom-sheet destination reachable from any screen and from external entry points via deep links (`app://capture?mode=text|voice`).

### 2.5 Persistence

- **Room** is the single source of truth for user content. Schema in `DATABASE.md`; schemas exported to `core/database/schemas/` and every migration has a test.
- **DataStore** for settings and session metadata; tokens encrypted with a Keystore-held AES-GCM key.
- **Full-text search**: Room FTS4 virtual tables for notes, tasks, journal (ICU tokenizer for Persian **[verify in Phase 1]**).
- **Attachments**: stored in app-private storage; metadata in Room; uploaded via sync.

### 2.6 Reminder Engine (`:core:reminders`)

| Concern | Approach |
|---|---|
| Exact time reminders | `AlarmManager.setExactAndAllowWhileIdle` when exact-alarm permission is granted; otherwise `setAndAllowWhileIdle` (inexact) and the UI states this. |
| Source of truth | `reminder_occurrence` rows in Room; alarms are a projection that can be rebuilt at any time. |
| Rebuild triggers | `BOOT_COMPLETED`, `LOCKED_BOOT_COMPLETED` (direct-boot-aware minimal store), `TIME_SET`, `TIMEZONE_CHANGED`, app update (`MY_PACKAGE_REPLACED`), permission change. |
| Timezones/DST | Reminders store either **floating local time** (`2026-12-01T09:00` + "local") or **fixed instant** (UTC). Recurrence expanded with `java.time` in the user's current zone. |
| Duplicates | Each occurrence has deterministic id `hash(reminderId, occurrenceStart)`; notification id derived from it; delivery recorded before posting. |
| Notification actions | Complete / Snooze / Reschedule via `BroadcastReceiver` → use case → Room. |
| Location reminders | Geofencing API (requires Play services) — **[Decision needed D-09: allow Play services dependency]**. |

### 2.7 Capture Pipeline

```
Entry point → CaptureSheet/VoiceRecorder → CaptureRepository.saveRaw()   (Room, immediate)
          → CaptureProcessor (deterministic rules first: dates EN/FA, "remind me", "#tag")
          → optional AI enrichment (/v1/ai/capture/interpret) with timeout
          → suggestion shown → user confirms/edits (or auto-applied when high confidence & non-destructive)
          → typed entity created (note/task/reminder/habit); capture marked processed
```

Raw captures never get deleted by processing failure; they stay in the Capture Inbox.

### 2.8 Background Work

WorkManager for sync (periodic + expedited on change, `NetworkType.CONNECTED`, exponential backoff), attachment upload, report generation requests, and reminder re-validation. Foreground service (type `microphone`) only during active voice recording; overlay service for the floating button (type `specialUse` **[verify Play policy in Phase 3]**).

### 2.9 Error Handling and Logging

- Domain errors as sealed `AppError` types; repositories return `Result<T, AppError>`; no silent `catch`.
- Logging via a thin `Logger` interface (Timber in debug); **no user content in logs**.
- Crash reporting pending **D-05**.

## 3. Offline-First Sync

### 3.1 Model

- Every syncable row has: `id` (UUIDv7, client-generated), `version` (server-assigned monotonic int), `updated_at` (client HLC timestamp), `deleted_at` (tombstone), `sync_state` (`SYNCED | PENDING | CONFLICT`).
- Local writes append an **outbox** entry (`entity_type, entity_id, op, payload, idempotency_key, attempt_count`) in the **same Room transaction** as the data write.
- `SyncWorker`: **push** outbox in order (batched) → **pull** changes since `last_server_cursor` → apply in a transaction.

### 3.2 Conflict Resolution Strategy

| Case | Rule |
|---|---|
| Different fields changed on two devices | Field-level merge (server keeps per-field `updated_at` for mergeable entities: tasks, notes metadata, habits). |
| Same scalar field changed | Last-writer-wins by Hybrid Logical Clock timestamp; loser value kept in `conflict_log` for 30 days and visible in "Sync history". |
| Note body edited on two devices | No silent LWW. Server keeps both: the newer becomes current, the other is saved as a **conflict copy** note linked to the original, and the user is notified. |
| Delete vs. edit | Edit wins (resurrects) unless the delete happened after the edit (HLC); deletes are soft (tombstones) with trash recovery. |
| Habit check-ins | Append-only events; merged by union, de-duplicated by id. Never conflicting. |
| Task completion toggles | Modeled as events in `task_history`; final state derived deterministically. |

**Never silently discard user data** (`AGENTS.md` §7): every overwritten value is recoverable from `conflict_log` or a conflict copy.

### 3.3 Idempotency

Every push carries an `Idempotency-Key`; the server stores processed keys for 7 days and returns the original result on retry.

## 4. Backend Architecture

### 4.1 Structure

```
backend/
  src/
    main.ts                 HTTP app bootstrap
    worker.ts               Background job worker bootstrap
    config/                 Typed config, env validation (zod/class-validator)
    common/                 Filters, guards, interceptors, pipes, logger, errors
    modules/
      auth/                 Sign-up/in, refresh, sessions, password reset, OAuth (Google sign-in)
      users/                Profile, preferences, export, deletion
      sync/                 Push/pull endpoints, cursors, conflict log, idempotency
      notes/  tasks/  reminders/  habits/  goals/  journal/  mood/
      focus/  learning/  vision/  achievements/  automation/
      files/                Presigned upload/download, attachment metadata
      ai/                   Orchestrator, providers/, prompts/, tools/, validation/, usage/
      integrations/         google/ (calendar, tasks), provider registry, token vault
      health/               Liveness/readiness
    database/
      migrations/           Versioned SQL migrations
  test/                     e2e (supertest) tests
```

- **ORM: Prisma** or **TypeORM/Drizzle** — recommended **Prisma** for typed schema + migrations (`DECISIONS.md` ADR-007, pending approval).
- DTO validation with `class-validator` + global `ValidationPipe({ whitelist: true, forbidNonWhitelisted: true })`.
- Global exception filter mapping to RFC 9457 Problem Details.
- Structured JSON logging (pino) with request id; PII redaction.
- OpenAPI generated from decorators at `/v1/docs` (disabled in production unless authenticated).
- Rate limiting via `@nestjs/throttler` (Redis store).
- Jobs: BullMQ on Redis — integration sync, AI reports, export generation, account deletion, attachment cleanup.

### 4.2 API Versioning

URI versioning (`/v1/...`). Breaking changes → `/v2`, with `/v1` supported for ≥ 6 months after a v2 app release. Android sends `X-App-Version`; the server can return `426 Upgrade Required` for unsupported versions.

## 5. AI Architecture

```
App ──► /v1/ai/* ──► AiOrchestrator
                       ├─ ContextRetriever (only permitted scopes; minimization)
                       ├─ PromptRegistry (versioned templates, EN/FA)
                       ├─ ModelRouter ──► ProviderAdapter (interface; one impl per vendor)
                       ├─ OutputValidator (JSON Schema / zod; reject or repair)
                       ├─ ToolExecutor (allow-listed tools only: create_task, create_reminder,…)
                       ├─ ConversationStore
                       └─ UsageMeter (tokens, cost, per-user quotas)
```

- Provider-independent `LlmProvider` interface: `complete()`, `structured()`, `embed()`.
- **Tools, not free actions.** The model can only propose calls to a fixed tool registry; each tool has a JSON schema, authorization check, and a `destructive` flag. Destructive tools return a *proposal* the user must confirm in-app.
- **No AI-generated code or SQL is ever executed.**
- Timeouts (default 15 s), bounded retries with jitter, circuit breaker per provider, idempotency keys for tool execution.
- Deterministic first: on-device rules (`:core:ai`) parse simple dates/intents offline in EN and FA; LLM used for semantic tasks.
- Cost: per-user daily token budget; usage stored in `ai_usage`.

## 6. Integrations Architecture

`IntegrationProvider` interface (`connect`, `disconnect`, `pull`, `push`, `capabilities`). Google Calendar and Google Tasks are the first two implementations. OAuth uses server-side authorization-code flow with PKCE; refresh tokens encrypted at rest (envelope encryption, KMS-held key) and never sent to the device. Each provider declares its `capabilities` so the UI only offers supported sync operations.

## 7. Deployment (initial)

Containerized (Docker). Environments: `local` (docker-compose: api, worker, postgres, redis, minio), `staging`, `production`. Hosting provider pending **D-04**. Migrations run as a release step, never on app boot in production.

## 8. Cross-Cutting Quality Attributes

| Attribute | Mechanism |
|---|---|
| Reliability | Transactional outbox, idempotent sync, alarm rebuild, migration tests |
| Performance | Baseline Profiles, Paging 3 for long lists, stable Compose state, indexes per query |
| Security | See `SECURITY.md` |
| Accessibility | Design-system components carry semantics by default; a11y checks in UI tests |
| i18n | All strings in resources (`values`, `values-fa`); `layoutDirection`-aware components; ICU number/date formatting; Jalali calendar support |
| Testability | Pure domain, injected clock/dispatchers, fakes in `:core:testing` |
