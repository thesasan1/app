# Progress

The GitHub repository is the source of truth. A checkpoint is added at the end of every development unit; older checkpoints are kept.

## Current State

| Field | Value |
|---|---|
| Current phase | **Phase 2 — Core Productivity** (Phase 1 done except PR 5b, waiting on ADR-007) |
| Status | Home today summary in review |
| Code status | Phase 1 merged: Android skeleton (#2), backend skeleton (#3), design system (#4), CI hardening (#5), local database (#14), logging (#19), toolchain upgrade (#20), NestJS 12 / Node 26 (#21), debug APK pre-release (#22). notes data layer (#23), notes repository (#24) and notes list/editor (#27) and archive/trash (#28) tasks data layer (#29) and tasks repository (#30) and tasks list (#31) task detail/editor (#32) tasks shelves (#33) reminder storage (#34), scheduler (#35) and notification actions (#36) merged. Reminder picker in review |
| Build status | `android-ci` and `backend-ci` green on `main` after #5 and the Dependabot action bumps |
| Test status | Backend: 13 unit + 12 e2e (CI green). Android: unit tests, lint, screenshots run in CI |
| Blockers | ADR-007 (Prisma) needs owner approval before PR 5b; ADR-010 (local DB encryption) before the first release with real content. D-03…D-09 pending for later phases |
| Next task | Habit reminder time in the habit form, then a freeze button once D-10 is decided; PR 5b once ADR-007 is approved |

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

### Checkpoint 2.11 — 2026-11-25 — Reminder scheduler merged (#35)

**Completed:** new `:core:reminders` (ADR-025). `ReminderTimeCalculator` works out the next firing: an offset from the task's due date and time (all-day tasks count as 09:00), a FIXED instant, or a FLOATING local time in the device zone; DST gaps move forward. `ReminderScheduler.rebuild()` is idempotent: it keeps one occurrence per firing, drops occurrences (and alarms) that no longer match because the task moved, closed or the reminder was disabled, records a firing missed by more than 12 hours as MISSED instead of ringing late, and (re)sets every pending alarm. `deliver()` rings an occurrence once; if notifications are blocked it records a miss. Android side: `AndroidAlarmScheduler` (exact while idle when allowed, otherwise inexact while idle, REM-07), the "Reminders" notification channel, an alarm receiver and a reschedule receiver for boot, app update, time and time-zone changes and the exact-alarm permission. The app rebuilds alarms on start. The manifest declares `SCHEDULE_EXACT_ALARM`, not `USE_EXACT_ALARM` (R-02).

**Not yet:** notification actions and repeat-until-done (REM-02), recurrence, the reminder picker on tasks, and the notification and exact-alarm permission prompts (KI-013).

**Tested:** `ReminderTimeCalculatorTest` 7/7, `ReminderSchedulerTest` 8/8 (local) → Passed; `android-ci` → Passed.

**Recommended next task:** notification actions (REM-02).

### Checkpoint 2.12 — 2026-11-25 — Reminder notification actions merged (#36)

**Completed:** reminder notifications get **Done** and **Snooze 10 min** buttons, and swiping one away is recorded (REM-02, ADR-026). Done marks the occurrence completed and completes the task through `TaskRepository`, so it gets the usual history row and outbox entry (`ReminderTargetActions` port, implemented in `:feature:tasks`). Snooze rings again after 10 minutes. A reminder set to repeat until done rings every 15 minutes for up to 24 hours; swiping it away doesn't stop it, only Done or turning the reminder off does.

**Not yet:** reschedule from the notification (needs a time picker; comes with the reminder picker), the picker itself, permission prompts (KI-013), and clearing a shown notification when the task is completed inside the app.

**Tested:** `ReminderSchedulerTest` 13/13, `ReminderTimeCalculatorTest` 7/7, `:feature:tasks` 25/25 (local) → Passed; `android-ci` → Passed.

**Recommended next task:** reminder picker on tasks (REM-01, REM-07).

### Checkpoint 2.13 — 2026-11-25 — Reminder picker on tasks merged (#37)

**Completed:** the task editor has a **Reminders** section (open tasks with a due date): add a reminder at the due time or 10 minutes, 1 hour or 1 day before; a "Repeat until done" checkbox and Remove per reminder. Tasks without a time count from 09:00 (ADR-025). `TaskReminderRepository` (`:core:reminders`) writes the reminder and its outbox entry in one transaction; removal is a soft delete. Alarms are rebuilt whenever the `tasks` or `reminders` tables change (`ReminderRebuildTrigger` on a new `DatabaseChanges` port over Room's invalidation tracker; ADR-027), so editing a due date, completing, archiving or trashing a task moves or cancels its alarms. Adding the first reminder asks for POST_NOTIFICATIONS on Android 13+; when notifications are off the section says so with a Turn on button (prompt or app notification settings), and without exact-alarm access it says reminders may ring a few minutes late, with an Allow button (REM-07). This resolves KI-013.

**Not yet:** custom offsets and fixed-time reminders, clearing an already shown notification when the task is completed in the app, and reschedule from the notification.

**Tested:** `TaskReminderRepositoryTest` 4/4, `ReminderSchedulerTest` 13/13, `:feature:tasks` 26/26 (local) → Passed; `android-ci` → Passed. On-device → Not run.

**Recommended next task:** clear shown notifications on in-app completion; then habits.

### Checkpoint 2.14 — 2026-11-25 — Stale reminder notifications cleared (#38)

**Completed:** every alarm rebuild now checks the notifications that may still be on screen (occurrences DELIVERED, or SNOOZED after ringing). If the task was completed, archived or trashed in the app, or the reminder was turned off or removed, the notification is taken down, its next repeat is cancelled, and the occurrence is recorded as DISMISSED. Because rebuilds follow every task and reminder write (ADR-027), ticking a task off in the list clears its reminder at once. New `ReminderDao.getRingingOccurrences()` (no schema change).

**Not yet:** reschedule from the notification; custom offsets and fixed-time reminders.

**Tested:** `ReminderSchedulerTest` 16/16, `:feature:tasks` 26/26 (local) → Passed; `android-ci` → Passed. On-device → Not run.

**Recommended next task:** reschedule from the notification and custom reminder times, then habits.

### Checkpoint 2.15 — 2026-11-25 — Custom reminder times and 1-hour snooze (#39)

**Completed:** the Reminders section has a **Custom…** chip: a dialog takes a number and Minutes, Hours or Days, up to 30 days before the due time (`customReminderOffset`, `ReminderUnit` in `:core:reminders`). Reminder notifications get a third button, **1 hour** (`ReminderScheduler.SNOOZE_LONG`), next to Done and Snooze 10 min, so "later" is one tap from the notification.

**Not yet:** picking an exact new time from the notification (needs an activity, not a broadcast); fixed-time and recurring reminders; reminders on habits and notes.

**Tested:** `ReminderSchedulerTest` 17/17, `TaskReminderRepositoryTest` 5/5, `:feature:tasks` 26/26 (local) → Passed; `android-ci` → Passed. On-device → Not run.

**Recommended next task:** habits data layer (HAB-01), Room v5.

### Checkpoint 2.16 — 2026-11-25 — Habits data layer (#40)

**Completed:** Room v5 adds `habits` and the append-only `habit_events` (ADR-028), with `HabitDao` (active and archived habits, event insert that ignores a known id, reads by habit day range and by day) and `WeekdayMask` for schedule and rest days. Auto-migration 4 → 5 with exported schema `5.json`.

**Not yet:** repository, schedule logic and streaks; habits screen; habit reminders.

**Tested:** `WeekdayMaskTest` 2/2 (local) → Passed; `HabitDaoTest`, migration 4 → 5 (Robolectric, CI) → Passed in `android-ci`. On-device → Not run.

**Recommended next task:** habits repository with schedule and check-in logic (HAB-01, HAB-02, HAB-06, HAB-07).

### Checkpoint 2.17 — 2026-11-25 — Habits repository and check-in rules (#41)

**Completed:** new `:feature:habits` module with `HabitRepository` (`OfflineHabitRepository`: create with validation, archive, unarchive, delete, check in, undo, correct, mark rest, observe a day) and `HabitRules` (schedule, effective events, progress, completion), ADR-029. Every write queues an outbox entry (`habit`, `habit_event`). `HabitDao` gains `getEvent` and `getEventsOn` (no schema change).

**Not yet:** UI, streaks and freezes, CUSTOM schedules, habit reminders.

**Tested:** `HabitRulesTest` 4/4, `OfflineHabitRepositoryTest` 7/7 (local) → Passed; `android-ci` → Passed. On-device → Not run.

**Recommended next task:** habits screen (today list, check-in, create habit).

### Checkpoint 2.18 — 2026-11-25 — Habits screen (#42)

**Completed:** Habits screen reached from a new Habits button on the dashboard. Lists today's active habits with status (done, progress against the goal, rest day, not due today). Actions: Done (yes/no), +1 (times a day), Log with an amount dialog (amount, minutes), Undo the newest check-in, Rest today, Archive. New habit dialog: name, how it's tracked, optional goal, unit, and days (all days = DAILY, fewer = WEEKDAYS). English and Persian strings. `HabitsViewModel` refreshes the day on resume.

**Not yet:** streaks and freezes, editing a habit, archive shelf, weekly/monthly targets and N-per-week in the form, habit reminders.

**Tested:** `HabitsViewModelTest` 4/4, `HabitRulesTest` 4/4, `OfflineHabitRepositoryTest` 7/7, `:feature:dashboard` 13/13 (local) → Passed; `android-ci` → Passed. On-device → Not run.

**Recommended next task:** streaks and freezes, then habit edit and the archive shelf.

### Checkpoint 2.19 — 2026-11-25 — Habit streaks and freezes (#43)

**Completed:** `HabitStreaks` derives each daily or weekday habit's current and best streak from its events (ADR-030); the Habits screen shows "N-day streak · best M". Rest days, days not due, used freezes and today-not-done-yet keep the run; any other missed due day ends it. `HabitRepository.useFreeze` spends a freeze on a missed past due day as a `FREEZE_USED` event and lowers `freezes_available`; undoing it gives the freeze back. `HabitDraft.freezes` (0–10) sets the starting freezes. `HabitDao.observeEventsBetween` (no schema change).

**Not yet:** a button to use a freeze and a way to earn or grant freezes (D-10); streaks for N-per-week, weekly and monthly targets; habit edit, archive shelf, reminders.

**Tested:** `HabitStreaksTest` 4/4, `OfflineHabitRepositoryTest` 9/9, `HabitRulesTest` 4/4, `HabitsViewModelTest` 4/4, `:feature:dashboard` 13/13 (local) → Passed; `android-ci` → Passed. On-device → Not run.

**Recommended next task:** habit edit and the archive shelf.

### Checkpoint 2.20 — 2026-11-25 — Habit archive shelf (#44)

**Completed:** an Archived button on the Habits screen opens `HabitsArchiveRoute`: archived habits with Restore (back to today's list) and Delete (asks first; a soft delete that keeps the event history and queues an outbox entry). `HabitsArchiveViewModel`, English and Persian strings.

**Not yet:** habit edit, freeze button (D-10), period streaks, habit reminders.

**Tested:** `HabitsArchiveViewModelTest` 2/2, `HabitsViewModelTest` 4/4, `HabitStreaksTest` 4/4, `HabitRulesTest` 4/4, `OfflineHabitRepositoryTest` 9/9 (local) → Passed; `android-ci` → Passed. On-device → Not run.

**Recommended next task:** habit edit.

### Checkpoint 2.21 — 2026-11-25 — Habit edit (#45)

**Completed:** an Edit button on each habit opens the habit form filled in. Name, goal, unit and days can change; how it's tracked and the start date can't (ADR-031). `HabitRepository.update` validates like create, keeps every event, and queues an outbox entry. The create and edit dialogs share one form.

**Not yet:** schedule history, so a days change re-evaluates past streaks (KI-014); habit reminders; freeze button (D-10).

**Tested:** `OfflineHabitRepositoryTest` 10/10, `HabitsViewModelTest` 5/5, `HabitsArchiveViewModelTest` 2/2, `HabitStreaksTest` 4/4, `HabitRulesTest` 4/4 (local) → Passed; `android-ci` → Passed. On-device → Not run.

**Recommended next task:** habit reminders.

### Checkpoint 2.22 — 2026-11-25 — Habit reminder engine (#46)

**Completed:** habit reminders in the scheduler (ADR-032). `HabitReminderRepository` keeps one daily reminder per habit (offset minutes after midnight). `HabitReminderTargets` gives the scheduler the habit's next 8 days that are due and not done or rested; `ReminderTimeCalculator` rings at the first of those still ahead, so a reminder skips days already done and moves to the next due day after ringing. "Done" on the notification checks in a yes/no or times-a-day habit; for amount and minutes habits it only clears. Targets and Done actions are now collected from each module (`AllReminderTargets`, `AllReminderTargetActions`, Hilt multibindings). Alarms rebuild on `habits` and `habit_events` changes too.

**Not yet:** a time picker in the habit form (no reminder can be set from the app yet); a shown habit notification isn't cleared when the habit is checked in in the app.

**Tested:** `ReminderSchedulerTest` 18/18, `ReminderTimeCalculatorTest` 8/8, `HabitReminderRepositoryTest` 1/1, `HabitReminderTargetsTest` 2/2 (local) → Passed; `android-ci` → Passed. On-device → Not run.

**Recommended next task:** reminder time in the habit form.

### Checkpoint 2.23 — 2026-11-26 — Modern app shell (#47)

**Completed:** UI refresh, step 1 (ADR-033). A bottom tab bar (Home, Tasks, Habits, Notes) shows on the four tab screens and hides on editors and shelves; each tab keeps its own back stack. The text "Back" buttons on the tab screens are gone; each tab opens with a large `ScreenHeader` title and its shelves as quiet actions at the end. Home shows Tasks, Habits and Notes as icon tiles (`AppTile`). Habits and Notes get a "+" floating button. Cards use the 20dp radius. Icons are drawn in `AppIcons` from Material Symbols paths (no icon library).

**Tested:** habits, tasks, notes, dashboard unit suites, compile, spotless, detekt (local) → Passed; `android-ci`, `dependency-review` → Passed. On-device → Not run.

**Recommended next task:** tidy habit rows (one primary action, the rest in a menu), then task and note rows.

### Checkpoint 2.24 — 2026-11-26 — Habit rows tidied (#48)

**Completed:** UI refresh, step 2 (ADR-033). Each habit row is one line of content: a round check-in button at the start (a check for yes/no habits, "+" for counted ones; it fills with the accent when the day is done), the name, and status and streak on one line. Undo, Rest today, Edit and Archive move into a "⋮" menu. Behaviour is unchanged.

**Tested:** habits suites (local) → Passed; `android-ci`, `dependency-review` → Passed. On-device → Not run.

**Recommended next task:** task and note rows in the same style.

### Checkpoint 2.25 — 2026-11-26 — Tasks list tidied (#49)

**Completed:** UI refresh, step 3 (ADR-033). Quick add is one rounded field with a filled "+" button inside it (no separate Add button, no underline); "Due today" is a pill chip under it. Task rows use a round check circle instead of a square checkbox. Behaviour is unchanged.

**Tested:** tasks suites (local) → Passed; `android-ci`, `dependency-review` → Passed. On-device → Not run.

**Recommended next task:** notes list rows and search field in the same style.

### Checkpoint 2.26 — 2026-11-26 — Notes search tidied (#50)

**Completed:** UI refresh, step 4 (ADR-033). The notes search is a pill-shaped field with a search icon and no outline (`AppIcons.Search`). Behaviour is unchanged.

**Tested:** notes suites (local) → Passed; `android-ci`, `dependency-review` → Passed. On-device → Not run.

**Recommended next task:** reminder time picker in the habit form (`HabitReminderRepository.set`).

### Checkpoint 2.27 — 2026-11-26 — Habit reminder time picker (#51)

**Completed:** the New and Edit habit dialogs have a "Reminder" row: it shows the time or "Off", opens a Material 3 time picker (12/24-hour from the phone setting, default 9:00), and "Clear" removes it. Saving calls `HabitReminderRepository.set` (ADR-032); the Edit dialog loads the current time first. Habit reminders can now be set in the app.

**Tested:** `HabitsViewModelTest` 6/6 and habits suites (local) → Passed; `android-ci`, `dependency-review` → Passed. On-device → Not run.

**Recommended next task:** clear a shown habit notification when the habit is checked in inside the app.

### Checkpoint 2.28 — 2026-11-26 — Clear habit notification on check-in (#52)

**Completed:** a shown habit reminder is taken down once its day is done or rested in the app. `ReminderScheduler.clearStale` now also treats a habit occurrence as stale when its local date is no longer in the target's `dueDates` (the rebuild already follows `habit_events`, ADR-032). Tasks are unchanged.

**Tested:** `ReminderSchedulerTest` 19/19 (local) → Passed; `android-ci`, `dependency-review` → Passed. On-device → Not run.

**Recommended next task:** freeze button once D-10 is answered; otherwise period streaks for weekly and monthly habits (HAB-03).

### Checkpoint 2.29 — 2026-11-26 — Icon back buttons (#53)

**Completed:** UI refresh, step 5 (ADR-033). Detail screens (task detail, note editor, task and note shelves, archived habits) use a back-arrow `BackButton` (mirrors in RTL, "Back" for screen readers) instead of a text "Back" button. The unused `feature_*_back` strings are removed.

**Tested:** habits, tasks, notes suites (local) → Passed; `android-ci`, `dependency-review` → Passed. On-device → Not run.

**Recommended next task:** freeze button once D-10 is answered; otherwise period streaks for weekly and monthly habits (HAB-03).

### Checkpoint 2.30 — 2026-11-26 — Home today summary (submitted)

**Completed (in review):** Home replaces its placeholder text with a "Today" heading; the Tasks tile reads "N due today · M overdue" (or "Nothing due today") and the Habits tile "X of Y done" (or "All done for today" / "No habits due today"). The dashboard declares a `TodaySummarySource` port; the app module implements it from `TaskRepository` and `HabitRepository` (`TodaySummaryModule`), so features stay independent (ADR-003).

**Tested:** see the PR.

**Recommended next task:** freeze button once D-10 is answered; otherwise period streaks for weekly and monthly habits (HAB-03).
