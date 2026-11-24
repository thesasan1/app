# app — AI-Powered Personal Operating System for Android

> Capture Everything. Organize Intelligently. Execute Consistently. Grow Continuously.

A premium, offline-first personal operating system for Android that unifies smart capture, notes, tasks, reminders, habits, goals, planning and an AI assistant — in English and Persian (RTL).

**Status:** Phase 1 — Engineering Foundation in progress. The Android app is a skeleton (dashboard shell only) and the backend serves health endpoints only; see [docs/PROGRESS.md](docs/PROGRESS.md).

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

### Android

Requirements: JDK 21, Android SDK with platform 37.2 (`ANDROID_HOME` or `android/local.properties`).

```bash
cd android
./gradlew spotlessApply                          # format Kotlin (ktlint)
./gradlew spotlessCheck detekt                   # formatting + static analysis
./gradlew testDebugUnitTest :core:common:test   # unit, Robolectric and screenshot tests
./gradlew lintDebug                              # Android lint
./gradlew assembleDebug                          # debug APK → app/build/outputs/apk/debug/
```

CI runs the same commands on every pull request touching `android/` (`.github/workflows/android-ci.yml`).

### Backend

Requirements: Node.js 26 LTS (24.9+ works).

```bash
cd backend
npm ci
npm run start:dev     # http://localhost:3000/v1/health/live, Swagger at /v1/docs
npm test && npm run test:e2e
```

See [backend/README.md](backend/README.md) for every command. CI: `.github/workflows/backend-ci.yml`.

### Trying the Android app

The quickest way: every green `android-ci` run on `main` refreshes the **Debug build** pre-release (tag `debug-latest`) on the repository's Releases page. Download `personal-os-debug.apk` there, with no sign-in needed. It is debug-signed and for testing only.

Every CI run, pull requests included, also uploads a debug APK. Open the latest green `android-ci` run under the repository's **Actions** tab, download the `app-debug-apk` artifact (GitHub sign-in required), unzip it, and install the `.apk` on an Android 8.0+ device (allow installs from unknown sources) or drag it onto an Android Studio emulator.
