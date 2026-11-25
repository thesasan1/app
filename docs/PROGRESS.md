# Progress

The GitHub repository is the source of truth. A checkpoint is added at the end of every development unit; older checkpoints are kept.

## Current State

| Field | Value |
|---|---|
| Current phase | **Phase 2 — Core Productivity** (Phase 1 done except PR 5b, waiting on ADR-007) |
| Status | Reminder scheduler (REM-01, REM-06, REM-07) in review |
| Code status | Phase 1 merged: Android skeleton (#2), backend skeleton (#3), design system (#4), CI hardening (#5), local database (#14), logging (#19), toolchain upgrade (#20), NestJS 12 / Node 26 (#21), debug APK pre-release (#22). notes data layer (#23), notes repository (#24) and notes list/editor (#27) and archive/trash (#28) tasks data layer (#29) and tasks repository (#30) and tasks list (#31) task detail/editor (#32) tasks shelves (#33) and reminder storage (#34) merged. Reminder scheduler in review |
| Build status | `android-ci` and `backend-ci` green on `main` after #5 and the Dependabot action bumps |
| Test status | Backend: 13 unit + 12 e2e (CI green). Android: unit tests, lint, screenshots run in CI |
| Blockers | ADR-007 (Prisma) needs owner approval before PR 5b; ADR-010 (local DB encryption) before the first release with real content. D-03…D-09 pending for later phases |
| Next task | Reminder notification actions (complete, snooze; REM-02) and repeat-until-done; then the reminder picker on tasks with the permission prompts; PR 5b once ADR-007 is approved |

## Completed Features

- Phase 0 foundation documentation — merged (PR #1).
- Android engineering skeleton + `android-ci` — merged (PR #2).
- Backend skeleton + `backend-ci` — merged (PR #3).
- Design-system foundation — merged (PR #4).
- CI hardening — merged (PR #5).
- Local database — merged (PR #14).
- Logging foundation — merged (PR #19).
- Android toolchain upgrade — merged (PR #20).
- Backend NestJS 12 / Node 26 — merged (PR #21).
- Debug APK pre-release — merged (PR #22).
- Notes data layer — merged (PR #23).
- Notes repository and document format — merged (PR #24).
- Notes list and editor with autosave — merged (PR #27).
- Notes archive and trash screens — merged (PR #28).
- Tasks data layer — merged (PR #29).
- Tasks repository with history — merged (PR #30).

## Features In Progress

- Phase 2: reminder scheduler (REM-01, REM-06, REM-07).

## Merged Pull Requests

- #1 Phase 0: Product and architecture foundation — merged 2026-11-24 (squash `d92d349`).
- #2 Android engineering skeleton + Android CI — merged 2026-11-24 (squash `d69a475`), `android-ci` green.
- #3 Backend skeleton + backend CI — merged 2026-11-24 (squash `bc6aafb`), `backend-ci` and `android-ci` green.
- #4 Design-system foundation — merged 2026-11-24 (squash `8e301d6`), `android-ci` green after one compile fix.
- #5 CI hardening — merged 2026-11-24 (squash `bdb5035`) after the owner enabled the dependency graph.
- Dependabot #6–#10 (GitHub Actions bumps) and #13 (backend lint/test tooling) — merged 2026-11-24 with green CI. #11 and #12 left open (KI-011).
- #14 Local database — merged 2026-11-24 (squash `5281ef2`), `android-ci` green.
- #17 JUnit 6.1.3 (Dependabot) — merged 2026-11-24.
- #19 Logging foundation — merged 2026-11-24 (squash `f92ac60`).
- #20 Android toolchain upgrade — merged 2026-11-24 (squash `7a8f4a8`); Dependabot #11, #15, #16, #18 closed as superseded.
- #21 NestJS 12 on Node 26 — merged 2026-11-24 (squash `f1b8939`); Dependabot #12 closed.
- #22 Debug APK pre-release — merged 2026-11-24 (squash `71872c3`); `debug-latest` published.
- #23 Notes data layer — merged 2026-11-24 (squash `c5cd5b3`).
- #24 Notes repository and ADR-009 document format — merged 2026-11-24 (squash `4b12820`).
- #27 Notes list and editor with autosave — merged 2026-11-25 (squash `46e7d3e`).
- #28 Notes archive and trash screens — merged 2026-11-25 (squash `d9185d4`).
- #29 Tasks data layer, Room v3 — merged 2026-11-25 (squash `0c427cc`).
- #30 Tasks repository with history and outbox — merged 2026-11-25 (squash `ea9bcf1`).

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

**Completed (merged, PR #14):** `:core:database` with Room 2.7.2 (KSP, exported schema v1): `projects`, `tags`, `categories`, `sync_outbox`, `sync_cursor`; embedded `SyncColumns`; DAOs with soft delete, active queries and trash purge; `TransactionRunner` for atomic change + outbox writes; Hilt `DatabaseModule`; migration harness; CI check that the schema is committed; Robolectric runtime cached and used offline in CI (KI-010). `UuidV7Generator` in `:core:common`. ADR-017.

**Tested:** `:core:common:test` (incl. 5 UUIDv7 tests), `spotlessCheck`, `detekt` → Passed locally. Robolectric DAO, outbox and migration tests → CI (KI-002).

**Recommended next task:** `feature/backend-database`.

### Checkpoint 1.6 — 2026-11-24 — Logging foundation submitted

**Completed (merged, PR #19):** `AppLogger` with lazy messages, `RedactingLogger` (emails, tokens, secrets, long numbers incl. Persian digits; UUIDs kept), `LogSink`, `AppError.code`; Logcat sink and Hilt `LoggingModule` (debug: DEBUG + stack traces; release: WARN+ without throwables); README commands; ADR-018.

**Tested (local):** `:core:common:test` (26 tests, incl. 8 redaction cases and 4 logger tests) → Passed; `spotlessCheck detekt` → Passed. App module compile → CI.

**Recommended next task:** PR 5b after the owner approves ADR-007; otherwise the Android toolchain upgrade.

### Checkpoint 1.7 — 2026-11-24 — Android toolchain upgrade merged (#20)

**Completed:** Gradle 9.9.2, AGP 9.5.2 (built-in Kotlin), Kotlin 2.4.21, KSP 2.3.14, compileSdk 37 / targetSdk 36, JDK 21 toolchain, plus every AndroidX/Hilt/Room/Robolectric/Roborazzi bump from Dependabot #11/#15/#16/#18. Convention plugins moved to the AGP 9 DSL. CI on JDK 21 with the SDK 37 Robolectric runtime. ADR-019.

**Tested (local, with a manually installed SDK 36; compileSdk was raised to 37 after CI showed AndroidX needs it):** configuration, Kotlin compile of every module including `:app` and KSP (Room schema unchanged), `:core:common:test`, `spotlessCheck`, `detekt` → Passed. Robolectric tests, lint, APK → CI (aapt2 and memory limits locally, KI-002).

**Recommended next task:** PR 5b after ADR-007 approval.

### Checkpoint 1.8 — 2026-11-24 — NestJS 12 on Node 26 merged (#21)

**Completed:** NestJS 12.6 / @nestjs/swagger 12.1 (from Dependabot #12); backend runtime and CI on Node.js 26 LTS via `backend/.nvmrc`; Jest started with `--experimental-vm-modules` so it can load ESM-only packages. ADR-020; KI-011 resolved.

**Tested (local, Node 26.13.1):** lint, format:check, typecheck, unit tests (13), e2e tests (12), openapi:check, build, and a production start answering `/v1/health/live` and `/v1/health/ready` → Passed.

**Recommended next task:** PR 5b after ADR-007 approval.

### Checkpoint 1.9 — 2026-11-24 — Debug APK pre-release merged (#22)

**Completed:** `publish-debug-apk` job in `android-ci` (main only, plus manual dispatch) that refreshes the `debug-latest` pre-release with the newest debug APK. ADR-021; README install steps.

**Tested:** workflow YAML parses → Passed. Publishing runs only after merge to `main` and is verified then.

### Checkpoint 2.1 — 2026-11-24 — Notes data layer merged (#23)

**Completed:** Room schema v2 (`notes`, `notes_fts`) via AutoMigration; `NoteDao` (active list with pinned first, project filter, archive, trash, restore, search, purge); `SearchText` normaliser and safe MATCH builder for Persian and English. ADR-022. Phase 2 started while PR 5b waits on ADR-007.

**Tested:** compile and KSP schema export (local) → Passed; `SearchTextTest` 4/4 (local) → Passed; `spotlessCheck`, `detekt` (local) → Passed. `NoteDaoTest` (10) and the v1→v2 migration test (Robolectric, CI) → Passed.

### Checkpoint 2.2 — 2026-11-24 — Notes repository merged (#24)

**Completed:** `:feature:notes` module. `NoteDocument` block model and `NoteDocumentCodec` (ADR-009 format v1, forward-compatible). `NoteRepository` / `OfflineNoteRepository`: create, autosave-safe `saveContent` (no write when unchanged), organize, pin, archive, trash, restore, delete forever (trash only), 30-day purge, search. Every write queues a sync outbox entry in the same transaction. Hilt binding; app depends on the module.

**Tested:** `NoteDocumentCodecTest` 7/7 and `OfflineNoteRepositoryTest` 10/10 (local, JVM fakes) → Passed; `spotlessCheck`, `detekt` (local) → Passed. App build with the Hilt graph → CI.

### Checkpoint 2.3 — 2026-11-25 — Notes list and editor merged (#27)

**Completed:** Notes list (pinned section, pin/unpin, debounced search, empty and no-results states) and a plain-text editor with autosave: saves 600 ms after typing stops and at once on lifecycle stop or back; saves run non-cancellable; the draft lives in `SavedStateHandle` so it survives process death; a new note is stored only once it has content; delete moves to the trash. A title edit keeps rich formatting the editor can't show yet (KI-012). Dashboard has a Notes entry; English and Persian strings.

**Tested:** view-model and repository tests 28/28 (local), `spotlessApply`, `detekt`, app compile (local) → Passed; lint, Robolectric, APK (CI) → Passed.

### Checkpoint 2.4 — 2026-11-25 — Notes archive and trash merged (#28)

**Completed:** Archive screen (open, unarchive) and trash screen (restore; delete forever behind a confirmation dialog; notes past the 30-day window are purged whenever the trash opens). The editor's More menu archives (and closes) or unarchives a note and moves it to the trash. The notes list links to both screens. English and Persian strings.

**Tested:** `:feature:notes` tests 33/33, `spotlessApply`, `detekt`, app compile (local) → Passed; lint, Robolectric, APK (CI) → Passed.

### Checkpoint 2.5 — 2026-11-25 — Tasks data layer merged (#29)

**Completed:** Room schema v3 (AutoMigration 2→3) with `tasks`, `tasks_fts` and append-only `task_history` (ADR-023). `TaskDao`: open, due-on, overdue, by-project, subtasks, completed, archived, trash, full-text search, history, delete forever (with subtasks) and purge. Migration test 2→3 and `TaskDaoTest`.

**Tested:** compile with KSP schema export, `spotlessApply`, `detekt` (local) → Passed; `TaskDaoTest` and migration 2→3 (Robolectric, CI) → Passed.

### Checkpoint 2.6 — 2026-11-25 — Tasks repository merged (#30)

**Completed:** New `:feature:tasks` module. Domain `Task`, `TaskDraft`, `TaskEvent` and `TaskRepository`; `OfflineTaskRepository` writes the task, an append-only history entry (only the fields that changed) and outbox entries for both in one transaction. Covers create (title 1–500 characters, time needs a date, one-level subtasks that inherit the parent's project), edit, reschedule (clears snooze), snooze, status changes (COMPLETED/REOPENED/STATUS_CHANGED), archive, trash and restore (a subtask waits for its parent), delete forever (trash only, with subtasks) and the 30-day purge. Changes that alter nothing write nothing.

**Tested:** `OfflineTaskRepositoryTest` 12/12, `spotlessCheck`, `detekt`, `:app:compileDebugKotlin` (local) → Passed; CI → Passed.

### Checkpoint 2.7 — 2026-11-25 — Tasks list and quick add merged (#31)

**Completed:** Tasks screen in `feature/tasks/ui`: open top-level tasks grouped into Overdue, Today, Upcoming and No date (regrouped on resume, so a list left open past midnight moves on); quick-add bar with a "Due today" chip (title capped at 500 characters, kept on failure); tick to complete with an Undo snackbar that restores the previous status; due label (Today/Tomorrow/date plus time) and High/Urgent priority. English and Persian strings. The dashboard gets a Tasks button wired by the app (ADR-003).

**Tested:** `TasksViewModelTest` 5/5, `OfflineTaskRepositoryTest` 12/12, dashboard tests, `spotlessCheck`, `detekt`, `:app:compileDebugKotlin` (local) → Passed; CI → Passed.

### Checkpoint 2.8 — 2026-11-25 — Task detail and editor merged (#32)

**Completed:** Tapping a task opens its detail screen (`TaskDetailRoute`). Title and notes autosave 600 ms after typing stops and when the screen stops; a blank title shows an error and keeps the saved one. Mark done / Reopen; priority chips (None to Urgent); due date with Today, Tomorrow, a date picker and Clear; due time with a time picker (a time on an undated task makes it due today) and Clear. Subtasks: list, tick, open, and add (top-level tasks only, one level deep, ADR-023). History of changes, newest first. More menu: Archive (top-level tasks) and Move to trash, both closing the screen. English and Persian strings.

**Tested:** `TaskDetailViewModelTest` 5/5, `TasksViewModelTest` 5/5, `OfflineTaskRepositoryTest` 12/12, `spotlessCheck`, `detekt`, `:app:compileDebugKotlin` (local) → Passed; CI → Passed.

### Checkpoint 2.9 — 2026-11-25 — Tasks completed, archive and trash screens merged (#33)

**Completed:** The tasks list header opens three shelves. Completed (newest first, Reopen, tap to open), Archive (Unarchive, tap to open) and Trash (Restore, Delete forever behind a confirmation that names the task and warns that subtasks and history go too; tasks older than 30 days are purged when the trash opens). Restoring a subtask whose parent is still in the trash shows "Restore the parent task first". English and Persian strings.

**Tested:** `TaskShelfViewModelsTest` 3/3 and the other 22 tasks tests, `spotlessCheck`, `detekt`, `:app:compileDebugKotlin` (local) → Passed; CI → Passed.

### Checkpoint 2.10 — 2026-11-25 — Reminder storage merged (#34)

**Completed:** Room schema v4 (`AutoMigration` 3→4) adds `reminders` (all DATABASE.md §3.4 columns, with location and event fields stored for later) and the local-only `reminder_occurrences`, whose ids are derived from reminder and instant so rebuilding never duplicates a firing (ADR-024). `ReminderDao`: upsert, per-target lists, the enabled time reminders to rebuild from, idempotent occurrence insert, pending occurrences by ring time, pruning of finished ones, and delete forever.

**Not yet:** the scheduler (AlarmManager, boot/time-change rebuild), notifications and their actions, and the reminder picker on tasks.

**Tested:** `ReminderDaoTest` 5/5 and the 3→4 migration test (CI, Robolectric) → Passed; compile, `spotlessCheck`, `detekt` (local) → Passed.

### Checkpoint 2.11 — 2026-11-25 — Reminder scheduler submitted

**Completed (in review):** new `:core:reminders` (ADR-025). `ReminderTimeCalculator` works out the next firing: an offset from the task's due date and time (all-day tasks count as 09:00), a FIXED instant, or a FLOATING local time in the device zone; DST gaps move forward. `ReminderScheduler.rebuild()` is idempotent: it keeps one occurrence per firing, drops occurrences (and alarms) that no longer match because the task moved, closed or the reminder was disabled, records a firing missed by more than 12 hours as MISSED instead of ringing late, and (re)sets every pending alarm. `deliver()` rings an occurrence once; if notifications are blocked it records a miss. Android side: `AndroidAlarmScheduler` (exact while idle when allowed, otherwise inexact while idle, REM-07), the "Reminders" notification channel, an alarm receiver and a reschedule receiver for boot, app update, time and time-zone changes and the exact-alarm permission. The app rebuilds alarms on start. The manifest declares `SCHEDULE_EXACT_ALARM`, not `USE_EXACT_ALARM` (R-02).

**Not yet:** notification actions and repeat-until-done (REM-02), recurrence, the reminder picker on tasks, and the notification and exact-alarm permission prompts (KI-013).

**Tested:** see the PR.

**Recommended next task:** notification actions (REM-02).
