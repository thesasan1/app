# API Specification

| Field | Value |
|---|---|
| Status | Draft v0.1 — Phase 0 contract outline, pending approval. **No endpoint is implemented yet.** |
| Style | REST over HTTPS, JSON, OpenAPI 3.1 generated from NestJS decorators |
| Base URL | `https://api.<domain>/v1` (domain pending D-01/D-04) |

---

## 1. Conventions

| Topic | Rule |
|---|---|
| Versioning | URI prefix `/v1`. Additive changes allowed in-version; breaking changes require `/v2`. |
| Auth | `Authorization: Bearer <access JWT>` (15 min TTL). Refresh tokens are opaque, rotated on every use, stored hashed server-side. |
| Content | `application/json; charset=utf-8`. Field names `camelCase`. |
| IDs | UUIDv7 strings, client-generated for user content. |
| Time | RFC 3339 UTC instants (`2026-11-24T05:51:36Z`); local date-times as `YYYY-MM-DDTHH:mm:ss` with `timeZoneMode`. |
| Idempotency | All `POST`/`PUT`/`PATCH`/`DELETE` accept `Idempotency-Key` (required on `/sync/push` and AI tool execution). |
| Concurrency | Single-entity writes send `If-Match: <version>`; mismatch → `409` with current entity. |
| Pagination | Cursor-based: `?limit=50&cursor=<opaque>`; response `{ items, nextCursor }`. |
| Errors | RFC 9457 Problem Details: `{ type, title, status, detail, instance, code, errors? }`. |
| Rate limits | `429` with `Retry-After`; headers `RateLimit-Limit`, `RateLimit-Remaining`, `RateLimit-Reset`. |
| Client headers | `X-App-Version`, `X-Device-Id`, `Accept-Language` (`en`, `fa`). |
| Validation | Server-side DTO validation on every endpoint; unknown fields rejected. |

### Standard error codes

| HTTP | `code` | Meaning |
|---|---|---|
| 400 | `VALIDATION_FAILED` | DTO validation errors in `errors[]` |
| 401 | `UNAUTHENTICATED` / `TOKEN_EXPIRED` | |
| 403 | `FORBIDDEN` | Authenticated but not allowed |
| 404 | `NOT_FOUND` | Also returned for resources owned by another user |
| 409 | `VERSION_CONFLICT` | `If-Match` mismatch |
| 413 | `PAYLOAD_TOO_LARGE` | |
| 422 | `UNPROCESSABLE` | Semantically invalid (e.g., bad RRULE) |
| 426 | `UPGRADE_REQUIRED` | App version unsupported |
| 429 | `RATE_LIMITED` | |
| 503 | `AI_UNAVAILABLE` | Provider down / circuit open — client must fall back |

## 2. Endpoint Groups

### 2.1 Auth — `/v1/auth` [Phase 6]

| Method | Path | Purpose |
|---|---|---|
| POST | `/auth/register` | Email + password sign-up (argon2id); sends verification |
| POST | `/auth/login` | Returns `{ accessToken, refreshToken, expiresIn, user }` |
| POST | `/auth/google` | Exchange Google ID token for session (if D-06 approves) |
| POST | `/auth/refresh` | Rotate refresh token; reuse detection revokes the session family |
| POST | `/auth/logout` | Revoke current session |
| POST | `/auth/password/forgot` · `/auth/password/reset` | Reset flow |
| POST | `/auth/email/verify` | Verify email |
| GET | `/auth/sessions` · DELETE `/auth/sessions/{id}` | List/revoke devices |

### 2.2 Users — `/v1/users/me` [Phase 6]

| Method | Path | Purpose |
|---|---|---|
| GET / PATCH | `/users/me` | Profile, locale, timezone |
| GET / PUT | `/users/me/settings` | Synced preferences |
| POST | `/users/me/export` | Start export job → `202 { jobId }` |
| GET | `/users/me/export/{jobId}` | Status + presigned download URL |
| DELETE | `/users/me` | Account deletion (requires recent re-auth) → `202` |

### 2.3 Sync — `/v1/sync` [Phase 6] — primary data channel

**Push**

```http
POST /v1/sync/push
Idempotency-Key: 0193a0c4-...
{
  "deviceId": "…",
  "changes": [
    { "entityType": "task", "op": "UPSERT", "id": "…", "baseVersion": 12,
      "updatedAt": "2026-11-24T05:51:36.120Z-0001-devA", "data": { … } },
    { "entityType": "note", "op": "DELETE", "id": "…", "baseVersion": 4 }
  ]
}
→ 200 {
  "results": [
    { "id": "…", "status": "APPLIED", "version": 13 },
    { "id": "…", "status": "CONFLICT_RESOLVED", "version": 5, "resolution": "FIELD_MERGE", "current": { … } }
  ]
}
```

Max 500 changes / 1 MB per request. `status ∈ APPLIED | CONFLICT_RESOLVED | CONFLICT_COPY_CREATED | REJECTED` (with `error`).

**Pull**

```http
GET /v1/sync/pull?cursor=<opaque>&limit=500
→ 200 { "changes": [ { "entityType", "id", "version", "deleted", "data" } ], "nextCursor": "…", "hasMore": false }
```

Entity types: `note, task, task_history, reminder, habit, habit_event, project, tag, category, entity_tag, entity_link, attachment, capture, goal, milestone, focus_session, daily_plan, daily_review, journal_entry, mood_entry, vision_item, learning_item, learning_session, automation_rule, setting`.

### 2.4 Entity REST endpoints [Phase 6]

Each syncable entity also exposes conventional REST for non-sync clients and admin tools:

`GET /v1/{entities}` (filter, cursor) · `GET /v1/{entities}/{id}` · `PUT /v1/{entities}/{id}` (upsert, `If-Match`) · `PATCH /v1/{entities}/{id}` · `DELETE /v1/{entities}/{id}` (soft) · `POST /v1/{entities}/{id}/restore`.

Collections: `notes, tasks, reminders, habits, habits/{id}/events, projects, tags, categories, goals, goals/{id}/milestones, focus-sessions, journal-entries, mood-entries, vision-items, learning-items, automation-rules`.

Representative DTO — **Task**:

```json
{
  "id": "0193a0c4-7b1e-7c3a-9f6e-2d1b5a8e4c10",
  "parentId": null,
  "projectId": null,
  "title": "Renew passport",
  "description": "",
  "status": "TODO",
  "priority": 3,
  "dueDate": "2026-12-01",
  "dueTime": "09:00:00",
  "timeZoneMode": "FLOATING",
  "timeZoneId": null,
  "recurrenceRule": null,
  "tagIds": [],
  "completedAt": null,
  "archivedAt": null,
  "deletedAt": null,
  "version": 13,
  "createdAt": "2026-11-24T05:51:36Z",
  "updatedAt": "2026-11-24T05:51:36Z"
}
```

### 2.5 Files — `/v1/files` [Phase 6]

| Method | Path | Purpose |
|---|---|---|
| POST | `/files/upload-url` | `{ mimeType, sizeBytes, sha256 }` → presigned PUT URL (5 min), `storageKey` |
| POST | `/files/{storageKey}/complete` | Server verifies size/hash, links to attachment |
| GET | `/files/{storageKey}/download-url` | Presigned GET (5 min), ownership checked |

Allowed MIME types allow-listed; max 25 MB (images), 50 MB (audio) — tunable.

### 2.6 AI — `/v1/ai` [Phase 5]

| Method | Path | Purpose |
|---|---|---|
| POST | `/ai/capture/interpret` | `{ text, locale, now, timeZone }` → `{ intent, confidence, entities, suggestion }` (no side effects) |
| POST | `/ai/conversations` | Start a conversation |
| POST | `/ai/conversations/{id}/messages` | Send message; streamed via SSE (`text/event-stream`) |
| POST | `/ai/tool-calls/{id}/confirm` | User confirms a proposed destructive/sensitive tool call |
| POST | `/ai/reports` | `{ kind: WEEKLY \| MONTHLY, period }` → `202 { jobId }` |
| GET | `/ai/reports/{id}` | Report result |
| POST | `/ai/habits/{habitId}/analysis` | Habit insights |
| GET / PUT | `/ai/permissions` | Granular context scopes |
| GET | `/ai/usage` | Current quota usage |

`interpret` response:

```json
{
  "intent": "CREATE_REMINDER",
  "confidence": 0.93,
  "language": "fa",
  "entities": { "title": "تماس با دکتر", "triggerAtLocal": "2026-11-25T10:00:00", "timeZoneMode": "FLOATING" },
  "requiresConfirmation": false,
  "clarification": null
}
```

Tool registry (initial): `create_task, update_task, complete_task, create_reminder, create_note, append_to_note, create_habit, log_habit, search, get_today_overview`. Destructive tools (`delete_*`, bulk updates) always return `PENDING_CONFIRMATION`.

### 2.7 Integrations — `/v1/integrations` [Phase 7]

| Method | Path | Purpose |
|---|---|---|
| GET | `/integrations` | List providers + status + capabilities |
| POST | `/integrations/google/authorize` | Returns auth URL (PKCE, state) |
| POST | `/integrations/google/callback` | Exchange code (server-side) |
| POST | `/integrations/{provider}/sync` | Trigger sync → `202` |
| PATCH | `/integrations/{provider}` | Settings (which calendars/task lists) |
| DELETE | `/integrations/{provider}` | Disconnect + revoke token |

### 2.8 Health

`GET /v1/health/live`, `GET /v1/health/ready` (DB + Redis checks; no auth; no internals leaked).

## 3. Security Requirements per Endpoint

- All except `auth/register|login|refresh|password/*|google`, `health/*` require a valid access token.
- Every resource lookup filters by `user_id = token.sub`; cross-user access returns `404`.
- Mutations are audited for sensitive operations (login, password change, export, deletion, integration connect/disconnect).
- Payload size limits at the gateway (1 MB JSON default).

## 4. Contract Testing

OpenAPI JSON is committed at `backend/openapi/openapi.v1.json` once Phase 1 generates it; CI fails if the generated spec drifts from the committed one without a version note. The Android client's DTOs are validated against fixtures generated from the spec.
