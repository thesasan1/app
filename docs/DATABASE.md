# Database Design

| Field | Value |
|---|---|
| Status | Draft v0.2. Room schema **version 2** is implemented: `projects`, `tags`, `categories`, `sync_outbox`, `sync_cursor` (v1, ADR-017), and `notes` + `notes_fts` (v2, ADR-022). Other tables are still design only. |
| Local store | Room (SQLite) on Android — source of truth for user interaction |
| Server store | PostgreSQL 16 |

---

## 1. Conventions

| Convention | Rule |
|---|---|
| Primary keys | `id` UUIDv7 (time-ordered), generated on the client; `TEXT` in SQLite, `uuid` in Postgres |
| Ownership | Every server row has `user_id` (FK `users.id`); every query is scoped by it |
| Sync columns (all syncable tables) | `version BIGINT` (server-assigned), `created_at`, `updated_at` (UTC epoch millis locally, `timestamptz` server), `deleted_at` nullable (tombstone); the HLC used for ordering is a separate column added with the sync engine (ADR-017) |
| Local-only columns | `sync_state` (`SYNCED`/`PENDING`/`CONFLICT`), `last_synced_version` |
| Time | Instants stored as UTC epoch millis (SQLite) / `timestamptz` (PG). "Floating" local date-times stored as ISO-8601 local string + `time_zone_mode` (`FLOATING` / `FIXED`) |
| Enums | Stored as `TEXT` with CHECK constraints (PG) / converters (Room) |
| Naming | `snake_case`, plural table names |
| Deletion | Soft delete via `deleted_at`; trash purge after 30 days (configurable); hard delete on account deletion |
| Migrations | Room: exported schema JSON + `MigrationTestHelper` test per version. PG: versioned migrations; destructive migrations need owner approval (`AGENTS.md` §17) |

## 2. Entity Overview

```
users ─┬─ devices
       ├─ sessions
       ├─ user_settings
       ├─ projects ──┬─ tasks ──┬─ subtasks (tasks.parent_id)
       │             │          ├─ task_history
       │             │          └─ reminders ── reminder_occurrences (local)
       │             └─ notes
       ├─ tags ── entity_tags (polymorphic join)
       ├─ categories
       ├─ entity_links (note↔task↔goal… polymorphic)
       ├─ attachments
       ├─ captures (inbox)
       ├─ habits ── habit_events (check-ins, freezes, corrections)
       ├─ goals ── milestones ; goal_links → tasks/habits
       ├─ focus_sessions
       ├─ journal_entries ; mood_entries
       ├─ vision_items
       ├─ learning_items ── learning_sessions
       ├─ achievements_unlocked ; xp_ledger
       ├─ automation_rules
       ├─ daily_plans ; daily_reviews
       ├─ ai_conversations ── ai_messages ; ai_usage ; ai_permissions
       ├─ integration_accounts ── integration_mappings
       └─ conflict_log ; sync_outbox (local) ; idempotency_keys (server)
```

## 3. Core Tables (Phase 2 scope)

### 3.1 `notes`

| Column | Type | Notes |
|---|---|---|
| id | uuid PK | |
| user_id | uuid FK | server |
| title | text | nullable |
| body | text | Rich text as a portable JSON document model (block list) — see ADR-009 |
| body_plain | text | Derived plain text for search/FTS |
| category_id | uuid FK nullable | |
| project_id | uuid FK nullable | |
| is_pinned | bool | default false |
| archived_at | timestamp nullable | |
| deleted_at | timestamp nullable | trash |
| version, created_at, updated_at | sync | |

Indexes: `(user_id, updated_at)`, `(user_id, is_pinned, updated_at)`, FTS on `title, body_plain`.

Local (Room v2): no `user_id`, and `category_id`/`project_id` aren't foreign keys, since sync may deliver a note before its project. `notes_fts` is an FTS4 table (`unicode61`) holding `note_id` plus normalised `title` and `body_plain`, written by `NoteDao.upsert` in the same transaction (ADR-022).

### 3.2 `tasks`

| Column | Type | Notes |
|---|---|---|
| id | uuid PK | |
| parent_id | uuid FK nullable | subtask → parent task |
| project_id | uuid FK nullable | |
| title | text NOT NULL | 1–500 chars |
| description | text | |
| status | text | `TODO`, `IN_PROGRESS`, `DONE`, `CANCELLED` |
| priority | smallint | 0 none, 1 low, 2 medium, 3 high, 4 urgent |
| due_date | date nullable | |
| due_time | time nullable | |
| time_zone_mode | text | `FLOATING`/`FIXED` |
| time_zone_id | text nullable | IANA id when FIXED |
| recurrence_rule | text nullable | RFC 5545 RRULE |
| recurrence_parent_id | uuid nullable | instance → series |
| snoozed_until | timestamp nullable | |
| completed_at | timestamp nullable | |
| archived_at, deleted_at | timestamp nullable | |
| sort_order | text | fractional index (LexoRank-style) for manual ordering |
| version, created_at, updated_at | sync | |

Indexes: `(user_id, status, due_date)`, `(user_id, project_id)`, `(parent_id)`, FTS on `title, description`.

Local (Room v3): no `user_id`; `parent_id`, `project_id` and `recurrence_parent_id` aren't foreign keys. `due_date` and `due_time` are ISO local text. `tasks_fts` (FTS4, `unicode61`) holds `task_id` plus normalised `title` and `description`, written by `TaskDao.upsert` (ADR-023).

### 3.3 `task_history`

Append-only. `id, task_id, event_type (CREATED, STATUS_CHANGED, RESCHEDULED, SNOOZED, EDITED, COMPLETED, REOPENED, ARCHIVED, DELETED, RESTORED), from_value json, to_value json, occurred_at, device_id`.

### 3.4 `reminders`

| Column | Type | Notes |
|---|---|---|
| id | uuid PK | |
| target_type | text | `TASK`, `HABIT`, `NOTE`, `STANDALONE` |
| target_id | uuid nullable | |
| title | text | for standalone |
| trigger_type | text | `TIME`, `LOCATION`, `EVENT` |
| trigger_at_local | text nullable | ISO local date-time (floating) |
| trigger_at_utc | timestamp nullable | fixed instant |
| time_zone_mode | text | |
| offset_minutes | int nullable | relative to task due (e.g., −30) |
| recurrence_rule | text nullable | |
| repeat_until_done | bool | |
| repeat_interval_min | int nullable | for repeat-until-done |
| location_lat/lng/radius_m | double/int nullable | LOCATION trigger |
| location_transition | text nullable | `ENTER`/`EXIT` |
| event_kind | text nullable | e.g., `CALENDAR_EVENT_START`, `DEVICE_CHARGING` (subject to platform support) |
| is_enabled | bool | |
| version, created_at, updated_at, deleted_at | sync | |

Local (Room v4): stored as above with `target_id` as a plain column; `trigger_at_local` is ISO local date-time text (ADR-024).

### 3.5 `reminder_occurrences` (local only)

`id (deterministic hash of reminder_id + scheduled_for), reminder_id, scheduled_for_utc, state (SCHEDULED, DELIVERED, SNOOZED, DISMISSED, COMPLETED, MISSED), delivered_at, snooze_until`. Rebuilt from `reminders` on boot/time change; guarantees no duplicate delivery. The id is a name-based UUID of `reminder_id@scheduled_for_utc` (ADR-024).

### 3.6 `habits`

| Column | Type | Notes |
|---|---|---|
| id | uuid PK | |
| name | text | |
| description, icon, color | text | |
| measurement_type | text | `BINARY`, `NUMERIC`, `DURATION`, `MULTI_CHECKIN` |
| target_value | numeric nullable | numeric/duration (seconds)/check-in count per period |
| unit | text nullable | e.g., "pages", "ml" |
| schedule_type | text | `DAILY`, `WEEKDAYS`, `X_PER_WEEK`, `WEEKLY_TARGET`, `MONTHLY_TARGET`, `CUSTOM` |
| schedule_days | int bitmask nullable | Mon=1…Sun=64 |
| schedule_count | int nullable | for X_PER_WEEK / targets |
| schedule_rule | text nullable | RRULE for CUSTOM |
| rest_days | int bitmask nullable | |
| streak_freeze_enabled | bool | |
| freezes_available | int | |
| start_date | date | |
| archived_at | timestamp nullable | |
| goal_id | uuid nullable | |
| version, created_at, updated_at, deleted_at | sync | |

### 3.7 `habit_events` (append-only, conflict-free)

| Column | Type | Notes |
|---|---|---|
| id | uuid PK | |
| habit_id | uuid FK | |
| local_date | date | the habit day this applies to (user's local date) |
| event_type | text | `CHECK_IN`, `UNDO`, `FREEZE_USED`, `REST`, `CORRECTION` |
| value | numeric nullable | amount for numeric/duration |
| note | text nullable | |
| corrects_event_id | uuid nullable | for CORRECTION/UNDO |
| recorded_at | timestamp | real time it was recorded (never back-dated) |

Streaks and analytics are **derived** from events; nothing is rewritten to preserve a streak (HAB-07). Derived daily aggregates may be cached in a local `habit_day_summary` table and rebuilt at any time.

### 3.8 Shared organization tables

- `projects` — `id, name, color, icon, archived_at, sort_order` + sync.
- `tags` — `id, name (unique per user, case-insensitive), color` + sync.
- `categories` — `id, name, scope (NOTE/TASK/GOAL/…)` + sync.
- `entity_tags` — `entity_type, entity_id, tag_id` (PK all three) + sync.
- `entity_links` — `id, from_type, from_id, to_type, to_id, link_kind` + sync.
- `attachments` — `id, owner_type, owner_id, mime_type, size_bytes, sha256, local_path (device), storage_key (server), upload_state, width, height, duration_ms` + sync.
- `captures` — `id, source (FAB, WIDGET, TILE, SHARE, VOICE, LOCKSCREEN, IN_APP), content_type (TEXT, VOICE, IMAGE, LINK), raw_text, attachment_id, language, status (NEW, PROCESSING, SUGGESTED, PROCESSED, FAILED), suggestion json, result_type, result_id` + sync.

## 4. Later-Phase Tables (outline; detailed when their phase starts)

| Table | Phase | Key columns |
|---|---|---|
| `goals` | 4 | name, description, horizon (LONG/SHORT), category_id, deadline, progress_mode (MANUAL/MILESTONES/LINKED), status |
| `milestones` | 4 | goal_id, title, due_date, completed_at, sort_order |
| `focus_sessions` | 4 | started_at, ended_at, planned_minutes, type (FOCUS/BREAK), task_id, interruptions int, notes |
| `daily_plans` | 4 | local_date, intention, priority_task_ids json, time_blocks json, available_minutes |
| `daily_reviews` | 4 | local_date, reflection, wins, journal_entry_id, mood_entry_id |
| `achievements_unlocked`, `xp_ledger` | 4 | achievement_key, unlocked_at / amount, reason, source |
| `ai_conversations`, `ai_messages` | 5 | title / role, content, tool_calls json |
| `ai_permissions` | 5 | scope (NOTES, TASKS, …), granted bool, granted_at |
| `ai_usage` (server) | 5 | provider, model, input_tokens, output_tokens, cost_micros, feature |
| `users`, `devices`, `sessions` (server) | 6 | email, password_hash (argon2id), locale / device name, push token / refresh token hash, expires_at, revoked_at |
| `integration_accounts` (server) | 7 | provider, external_account_id, scopes, encrypted_refresh_token, status, last_sync_at |
| `integration_mappings` | 7 | provider, local_type, local_id, external_id, external_etag |
| `journal_entries` | 8 | local_date, title, body, is_private (requires app lock), mood_entry_id |
| `mood_entries` | 8 | local_date, mood (1–5), emotions json, note |
| `vision_items` | 8 | title, image attachment_id, category_id, goal_id, sort_order |
| `learning_items`, `learning_sessions` | 8 | kind (BOOK/COURSE), title, author, total_units, current_unit, status / minutes, units_progressed, notes |
| `automation_rules` | 8 | trigger json, conditions json, actions json (allow-listed), enabled, last_run_at |

## 5. Sync Support Tables

- `sync_outbox` (local): `seq INTEGER PK AUTOINCREMENT, entity_type, entity_id, op (UPSERT/DELETE), payload json, idempotency_key, attempt_count, last_error, created_at`.
- `sync_cursor` (local): `scope, server_cursor`.
- `conflict_log` (local + server): `id, entity_type, entity_id, field, losing_value json, winning_value json, resolved_by (AUTO/USER), created_at`. Retained 30 days.
- `idempotency_keys` (server): `key, user_id, response_hash, created_at` (TTL 7 days).

## 6. Data Retention and Privacy

- Trash: 30 days, then purge job.
- Conflict log: 30 days.
- AI conversations: retained until user deletes; user can disable storage.
- Account deletion: hard delete of all user rows and object-storage files within 30 days (target: immediate soft-lock, async purge job), audit record without content.
- Export: JSON + attachments ZIP covering every table above.
