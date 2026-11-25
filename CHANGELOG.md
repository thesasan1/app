# Changelog

All notable changes are documented here. Format based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/); versions follow [Semantic Versioning](https://semver.org/) once releases begin.

## [Unreleased]

### Added

- Local storage for reminders and their firings (database v4), so a reminder never fires twice after a restart.
- Tasks Completed, Archive and Trash screens: reopen, unarchive, restore, and delete forever with confirmation.
- Task detail screen: edit the title and notes (saved as you type), priority, due date and time with pickers, subtasks, and a history of changes; archive or move to trash.
- Tasks screen: tasks grouped by Overdue, Today, Upcoming and No date, quick add with "Due today", and tick-to-complete with Undo. Open it from the dashboard.
- Task logic: create, edit, reschedule, snooze, complete and reopen, archive, trash and restore, with a history of every change.
- Local storage for tasks: subtasks, due dates, priority, search and an append-only history (database v3).
- Notes archive and trash: unarchive, restore, delete forever with confirmation, and automatic removal after 30 days in the trash.
- Notes screens: a list with pinned notes and search, and an editor that saves as you type, opened from the dashboard.
- Notes repository with the rich-text document format (ADR-009), autosave-safe saving, trash, and sync outbox entries.
- Notes storage (Room schema v2) with Persian-aware full-text search, archive, trash and purge.
- `debug-latest` pre-release with the newest debug APK from `main`, for testers.
- Redacting logger (`AppLogger`) and stable error codes (`AppError.code`).
- Local database module (Room schema v1: projects, tags, categories, sync outbox and cursor), migration test harness, and UUIDv7 id generator.
- Kotlin formatting (Spotless + ktlint) and static analysis (detekt) for all Android modules, enforced in `android-ci`; dependency review on pull requests; Dependabot for Gradle, npm and GitHub Actions.
- Design-system foundation: semantic color tokens with six accent options, bundled Inter and Vazirmatn variable fonts with a locale-aware type scale, spacing, radius and motion tokens, `AppButton`, `AppCard` and `EmptyState`, and Roborazzi screenshot rendering in CI.
- Backend skeleton (`backend/`): NestJS 11, zod-validated environment, `/v1/health/live` and `/v1/health/ready`, structured logging with redaction and request ids, RFC 9457 problem details, global validation, security headers, OpenAPI document with drift check, unit and e2e tests.
- `backend-ci` GitHub Actions workflow.
- README instructions for installing the CI-built debug APK.
- Android project skeleton (`android/`): Gradle version catalog and convention plugins, `:app`, `:core:common`, `:core:designsystem`, `:feature:dashboard` (greeting + date + empty state, English and Persian), Hilt, type-safe Navigation, JUnit 5 unit tests.
- `android-ci` GitHub Actions workflow: unit tests, Android lint, debug APK artifact.
- ADR-012 (agent merge authority) and ADR-013 (Android skeleton baseline).
- Phase 0 product and technical foundation: PRD, architecture, database model, API specification, design system, development roadmap, testing strategy, security model, decisions log, known issues and risks, progress tracking, release notes placeholder.
- GitHub workflow conventions: `CONTRIBUTING.md`, Pull Request template, issue templates, `.gitignore`, `.editorconfig`.

### Changed

- Backend: NestJS 12 on Node.js 26 LTS; Jest runs with VM modules to load ESM-only packages.
- Android toolchain: Gradle 9.9, AGP 9.5, Kotlin 2.4, compileSdk 37, targetSdk 36, JDK 21, plus current AndroidX libraries.
- `android-ci` uses v5 of the checkout, setup-java, setup-gradle and upload-artifact actions.
