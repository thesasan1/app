# app — AI-Powered Personal Operating System for Android

> Capture Everything. Organize Intelligently. Execute Consistently. Grow Continuously.

A premium, offline-first personal operating system for Android that unifies smart capture, notes, tasks, reminders, habits, goals, planning and an AI assistant — in English and Persian (RTL).

**Status:** Phase 0 — Product and Architecture Foundation (documentation only, in review). There is no runnable code yet.

## Stack

- **Android:** Kotlin, Jetpack Compose, Material 3 + custom design system, Clean Architecture/MVVM, Hilt, Room, DataStore, WorkManager, Retrofit/OkHttp.
- **Backend:** Node.js, TypeScript, NestJS, PostgreSQL, Redis, REST + OpenAPI.

## Documentation

| Document | Purpose |
|---|---|
| [AGENTS.md](AGENTS.md) | Master development instructions |
| [docs/PRD.md](docs/PRD.md) | Product requirements with requirement IDs |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | System, Android, backend, sync and AI architecture |
| [docs/DATABASE.md](docs/DATABASE.md) | Data model |
| [docs/API_SPECIFICATION.md](docs/API_SPECIFICATION.md) | API contracts |
| [docs/DESIGN_SYSTEM.md](docs/DESIGN_SYSTEM.md) | Design tokens, components, motion, accessibility |
| [docs/DEVELOPMENT_ROADMAP.md](docs/DEVELOPMENT_ROADMAP.md) | Phases and planned PRs |
| [docs/TESTING_STRATEGY.md](docs/TESTING_STRATEGY.md) | Test pyramid and CI gates |
| [docs/SECURITY.md](docs/SECURITY.md) | Threat model and controls |
| [docs/DECISIONS.md](docs/DECISIONS.md) | ADRs and pending owner decisions |
| [docs/PROGRESS.md](docs/PROGRESS.md) | Current state and checkpoints |
| [docs/KNOWN_ISSUES.md](docs/KNOWN_ISSUES.md) | Known issues, risks, dependencies |
| [docs/RELEASE_NOTES.md](docs/RELEASE_NOTES.md) | Release notes |
| [CHANGELOG.md](CHANGELOG.md) | Change history |
| [CONTRIBUTING.md](CONTRIBUTING.md) | Branching, commits, Pull Requests |

## Planned repository layout (from Phase 1)

```
android/     Android application (Gradle multi-module)
backend/     NestJS API and worker
docs/        Product and technical documentation
.github/     CI workflows and templates
```

## Building

Build and test commands will be documented here when Phase 1 creates the projects.
