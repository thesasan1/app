# Product Requirements Document (PRD)

| Field | Value |
|---|---|
| Product | AI-Powered Personal Operating System for Android (working name: **app**) |
| Document status | Draft v0.1 — Phase 0, pending project-owner approval |
| Source of truth for scope | `AGENTS.md` §2, §3, §5, §6 |
| Last updated | 2026-11-24 |

> This PRD restates and structures the scope defined in `AGENTS.md`. It does not reduce that scope. Where this document makes a product interpretation, it is flagged as **[Decision needed]** or **[Assumption]** and listed in §11.

---

## 1. Vision

A premium, AI-powered personal operating system that helps people organize their minds, manage responsibilities, build better habits, achieve meaningful goals, and make personal growth measurable.

**Product philosophy:** Capture Everything. Organize Intelligently. Execute Consistently. Grow Continuously.

It is explicitly **not** a basic task manager, note app, or habit tracker; it is one integrated platform where captured information, commitments, routines, and goals are connected.

## 2. Problems (priority order)

| # | Problem | Primary product answer |
|---|---|---|
| P1 | Mental clutter and unrecorded thoughts | Zero-friction Smart Capture (FAB, widget, tile, share sheet, voice) |
| P2 | Forgetting tasks, commitments, ideas | Reliable reminders, intent detection on capture, overdue management |
| P3 | Information scattered across places | Unified local store; notes ↔ tasks ↔ projects ↔ goals links; Google Calendar/Tasks integration; search |
| P4 | Difficulty maintaining consistent habits | Flexible habit engine, honest streaks, heatmaps, AI habit analysis |
| P5 | Ineffective daily planning and time management | Dashboard, morning planning, evening review, time blocking, focus timer |

Every feature decision is traced to at least one of P1–P5.

## 3. Target Users and Personas

| Persona | Description | Key needs |
|---|---|---|
| **The Founder** (entrepreneurs, business owners) | Many parallel commitments, ideas arrive anywhere | Instant capture, projects, priority selection, weekly reports |
| **The Achiever** (goal-oriented individuals, self-improvement enthusiasts) | Tracks goals and habits deliberately | Goals ↔ habits linkage, analytics, achievements |
| **The Student** | Deadlines, courses, reading | Reminders, learning tracker, focus timer |
| **The Professional / Manager** | Meetings and tasks across tools | Calendar integration, time blocking, dashboard |
| **The Struggler** (difficulty with organization and consistency) | Overwhelmed, abandons tools quickly | Beginner-friendly onboarding, low friction, forgiving streaks, gentle AI guidance |

The product must be approachable for beginners while offering depth for power users (progressive disclosure).

**Languages from day one:** English and Persian (فارسی), including full RTL layout support.

## 4. Core Principles (acceptance lens for every feature)

Zero-Friction Capture · Intelligent Organization · Visible Personal Growth · Premium UX · Reliability · Offline-First · Privacy-First · Performance · Accessibility · Scalability · Maintainability · User Control.

## 5. Non-Negotiable Requirements

Restated from `AGENTS.md` §3; these are release gates, not goals:

1. Full-scale product, not a prototype; all approved modules are eventually implemented.
2. No silent removal/simplification of requirements; no mocks in production paths.
3. Core productivity (notes, tasks, reminders, habits) works fully offline.
4. AI is never a single point of failure; captured data is preserved when AI fails.
5. No data loss from network failures, crashes, or sync problems.
6. No secrets or private data exposed; no private AI keys in the Android app.
7. Every important action gives clear user feedback.
8. Every development stage ships through a reviewed GitHub Pull Request.

## 6. Functional Requirements

Requirement IDs are stable and are referenced by PRs, tests, and `PROGRESS.md`. Priority: **M** = must for v1.0, all requirements in `AGENTS.md` are M unless noted; roadmap phase in brackets.

### 6.1 Smart Capture — `CAP` [Phase 3]

| ID | Requirement |
|---|---|
| CAP-01 | Floating entry point: tap → quick text capture; long-press → voice capture; drag → quick actions (New Note, Task, Reminder, Habit). |
| CAP-02 | Floating entry point settings: enable/disable, position, size, transparency. Uses `SYSTEM_ALERT_WINDOW` overlay only with explicit permission and clear explanation; degrades to in-app FAB when denied. |
| CAP-03 | Home-screen widget (Glance) for one-tap capture. |
| CAP-04 | Quick Settings Tile for capture. |
| CAP-05 | Android Share Sheet target accepting text, links, images. |
| CAP-06 | Text, link, image and screenshot capture. |
| CAP-07 | Lock-screen shortcuts where the platform supports them (no bypass of security restrictions). |
| CAP-08 | Voice capture: press-and-hold starts; recording continues after release; explicit stop; persistent recording indicator; never records without explicit action. |
| CAP-09 | Speech-to-text in English and Persian. |
| CAP-10 | Intelligent processing: intent detection, entity and date/time extraction, categorization, tag suggestions, task/reminder detection, duplicate detection, contextual suggestions. |
| CAP-11 | High-confidence results minimize confirmations; ambiguous results ask one concise clarification; destructive/sensitive actions always confirmed. |
| CAP-12 | Raw capture is persisted locally **before** any processing, so nothing is lost if processing fails ("capture inbox"). |

### 6.2 Notes — `NOTE` [Phase 2]

| ID | Requirement |
|---|---|
| NOTE-01 | Create/edit notes with reliable autosave (debounced writes + write on lifecycle stop; no data loss on process death). |
| NOTE-02 | Rich text editing (headings, lists, checklists, bold/italic, links). |
| NOTE-03 | Tags, categories, pinning, archiving. |
| NOTE-04 | Trash with recovery (soft delete, retention window, permanent delete requires confirmation). |
| NOTE-05 | Attachments, images, links. |
| NOTE-06 | Search, sorting, filtering. |
| NOTE-07 | Link notes to tasks and projects. |
| NOTE-08 | Offline access and synchronization. |

### 6.3 Tasks — `TASK` [Phase 2]

| ID | Requirement |
|---|---|
| TASK-01 | Fields: title, description, due date, due time, priority, status, tags, project, creation and completion timestamps. |
| TASK-02 | Subtasks. |
| TASK-03 | Recurrence (RFC 5545 RRULE subset). |
| TASK-04 | Multiple reminders per task. |
| TASK-05 | Lifecycle: status transitions; completion ≠ deletion; archiving; trash. |
| TASK-06 | Search, filter, sort. |
| TASK-07 | Reschedule, snooze, overdue management. |
| TASK-08 | Task history (audit of state changes). |

### 6.4 Reminders — `REM` [Phase 2]

| ID | Requirement |
|---|---|
| REM-01 | Date-, time-based and recurring reminders; multiple notifications. |
| REM-02 | Notification actions (complete, snooze, reschedule); repeat until completed. |
| REM-03 | Location-based reminders where supported (geofencing; requires explicit location permission). |
| REM-04 | Event-triggered reminders where supported. |
| REM-05 | Smart reminder suggestions (AI, Phase 5). |
| REM-06 | Reliability: survives reboot, timezone and DST changes, background restrictions, notification-permission denial, battery optimization; no duplicate notifications; restores schedules when required. |
| REM-07 | Exact-alarm delivery is only promised when `SCHEDULE_EXACT_ALARM`/`USE_EXACT_ALARM` is granted; the UI explains degraded (inexact) mode otherwise. |

### 6.5 Habits — `HAB` [Phase 2 core, Phase 4 analytics]

| ID | Requirement |
|---|---|
| HAB-01 | Schedules: daily, specific weekdays, N days/week, weekly target, monthly target, custom, rest days. |
| HAB-02 | Measurement: binary, numeric, duration, multiple check-ins/day. |
| HAB-03 | Analytics: current/best streak, totals, weekly/monthly consistency, completion %, history, records. |
| HAB-04 | GitHub-style contribution heatmap and trend charts. |
| HAB-05 | Achievements. |
| HAB-06 | Flexibility: rest days, optional streak freeze, missed-day handling, recovery tracking, historical corrections. |
| HAB-07 | Integrity: history is never altered to preserve a streak; freezes and corrections are recorded as explicit events. |
| HAB-08 | AI habit assistant: consistency analysis, recurring-difficulty detection, routine and schedule suggestions, long-term insights (Phase 5). |

### 6.6 Dashboard — `DASH` [Phase 2, extended Phase 4]

| ID | Requirement |
|---|---|
| DASH-01 | Greeting, date, daily progress, today's/priority/overdue tasks, upcoming reminders, today's habits, streaks, quick capture, calendar preview, focus info. |
| DASH-02 | Customizable (show/hide, reorder cards). |
| DASH-03 | Context-aware prioritization; avoids overload. |

### 6.7 Daily Planning and Review — `PLAN` [Phase 4]

| ID | Requirement |
|---|---|
| PLAN-01 | Morning planning: review today, select priorities, time availability, habit planning, time blocking, daily intention. |
| PLAN-02 | Evening review: completed/incomplete tasks, achievements, habit performance, journal, reflection, tomorrow prep. |
| PLAN-03 | Both workflows customizable. |

### 6.8 AI Personal Assistant — `AI` [Phase 5]

| ID | Requirement |
|---|---|
| AI-01 | Natural-language creation of tasks and reminders; note organization. |
| AI-02 | Intent recognition, entity extraction, date/time understanding (EN + FA, incl. Solar Hijri dates). |
| AI-03 | Daily planning, prioritization, habit analysis, summaries, recommendations. |
| AI-04 | Weekly and monthly reports. |
| AI-05 | Contextual conversations with granular, user-controlled context permissions (notes, tasks, reminders, habits, goals, calendar, history). |
| AI-06 | Provider-independent architecture separating provider, model config, prompts, retrieval, intent, extraction, validation, tool execution, conversation. |
| AI-07 | Safety: validated outputs; no execution of AI-generated code/SQL; only predefined validated tools; timeouts, retries, idempotency, rate limits, cost tracking; confirmation for destructive operations. |
| AI-08 | App remains fully usable when AI is unavailable; deterministic parsers handle simple operations offline. |
| AI-09 | No AI provider keys in the Android app; all LLM calls go through the backend. |

### 6.9 Personal Growth Modules [Phases 4 and 8]

| ID | Module | Scope |
|---|---|---|
| GOAL-01 | Goals | Long/short-term goals, milestones, progress, deadlines, categories, linked tasks and habits. [P4] |
| FOC-01 | Focus / Pomodoro | Timer, custom durations, breaks, history, statistics, distraction tracking where appropriate. [P4] |
| GAM-01 | Achievements & gamification | XP, levels, achievements, milestones, celebrations — tasteful, optional, never manipulative. [P4] |
| JRN-01 | Journal | Daily entries, reflection, search, tags, mood association, private (locked) entries. [P8] |
| VIS-01 | Vision board | Visual goals, images, categories, goal associations. [P8] |
| MOOD-01 | Mood tracking | Daily entries, trends, optional notes, privacy controls. [P8] |
| READ-01 | Reading & learning | Books, courses, learning goals, sessions, progress, notes/takeaways. [P8] |
| AUTO-01 | Workflow automation | Predefined workflows and rules; understandable, controllable, reversible where possible. [P8] |

### 6.10 Integrations — `INT` [Phase 7]

| ID | Requirement |
|---|---|
| INT-01 | Google Calendar via official API, OAuth 2.0, minimal scopes. |
| INT-02 | Google Tasks via official API. |
| INT-03 | Secure token handling (server-side refresh tokens), sync, error recovery, conflict handling. |
| INT-04 | User-controlled connect/disconnect and clear status. |
| INT-05 | Only claim sync operations the API actually supports (e.g., Google Tasks has no priority, tags, or sub-day due times — documented as limitations). |
| INT-06 | Pluggable integration architecture for future providers. |

### 6.11 Platform-wide — `PLT`

| ID | Requirement |
|---|---|
| PLT-01 | Offline-first: local DB is source of truth; sync queue; conflict resolution; sync status. |
| PLT-02 | Accounts: sign-up/sign-in, multi-device sync, data export, account deletion. |
| PLT-03 | Security: optional biometric/PIN app lock, Keystore-backed secrets, encryption in transit and at rest. |
| PLT-04 | Personalization: light/dark, accent color, font preferences, EN/FA typography, RTL/LTR. |
| PLT-05 | Accessibility: dynamic type, TalkBack labels, contrast, 48dp targets, keyboard nav, reduced motion. |
| PLT-06 | Backup and recovery. |
| PLT-07 | Search across all entity types. |

## 7. Non-Functional Requirements

| Area | Target (initial; refined in Phase 9) |
|---|---|
| Cold start | ≤ 1.5 s to first interactive frame on a mid-range device (Baseline Profiles) |
| Capture latency | Quick text capture surface opens in ≤ 300 ms from FAB/tile tap |
| UI | 60 fps scrolling on lists of 10k items; no main-thread DB/network I/O |
| Data durability | Zero acknowledged writes lost on crash/process death |
| Offline | 100% of core CRUD available offline |
| Min SDK / Target SDK | minSdk 26 (Android 8.0), targetSdk = latest stable (35+) **[Decision needed D-02]** |
| Battery | No unbounded background work; WorkManager with constraints |
| Availability (backend) | 99.5% initial SLO; app degrades gracefully when backend is down |
| Privacy | Data minimization; no third-party analytics/ads SDKs without explicit approval |
| Accessibility | WCAG 2.2 AA–equivalent contrast and target sizes |

## 8. Out of Scope (v1.0)

- iOS, web, desktop clients (architecture keeps the API client-agnostic).
- Team/shared workspaces and collaboration.
- Non-Google integrations (architecture allows them later).
- On-device LLM inference (may be evaluated later; see `DECISIONS.md`).

## 9. Success Metrics

| Metric | Why |
|---|---|
| Time from intent to saved capture (median) | P1 zero-friction |
| % captures processed into a task/note/reminder within 24 h | P2/P3 organization |
| Reminder delivery accuracy (delivered within window / scheduled) | P2 reliability |
| 30-day habit retention (users with ≥1 active habit) | P4 |
| Daily planning completion rate | P5 |
| Crash-free sessions ≥ 99.8%, data-loss incidents = 0 | Reliability |

Measuring these requires privacy-respecting, opt-in telemetry; the mechanism is **[Decision needed D-05]**.

## 10. Release Definition

v1.0 is reached when all Phase 0–9 deliverables are merged, the Definition of Done (`AGENTS.md` §19) holds for every requirement above, and the project owner approves the production release.

## 11. Open Questions and Assumptions

See `docs/DECISIONS.md` → "Pending decisions" for the authoritative list. Summary:

- **D-01** Product name and Android `applicationId`.
- **D-02** minSdk 26 vs. 28.
- **D-03** AI provider(s) for the backend (paid service — owner must choose).
- **D-04** Hosting/cloud provider for backend, PostgreSQL, object storage.
- **D-05** Telemetry/crash reporting vendor (privacy-sensitive).
- **D-06** Authentication method(s): email+password, Google Sign-In, passkeys.
- **D-07** Speech-to-text: on-device Android `SpeechRecognizer` vs. server-side STT for Persian quality.
- **D-08** Monetization model (free / subscription) — affects AI cost controls.
- **[Assumption]** Calendar: Gregorian and Solar Hijri (Jalali) display both supported, chosen per user locale.
