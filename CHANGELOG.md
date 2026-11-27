# Changelog

All notable changes are documented here. Format based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/); versions follow [Semantic Versioning](https://semver.org/) once releases begin.

## [Unreleased]

### Added

- Habits can be "times a week" (for example, gym 3 times a week), with "2 of 3 this week" on the row.
- Home shows today at a glance: tasks due and overdue, and habits done.
- Detail screens use a back arrow instead of a "Back" text button.
- Habits: set a daily reminder time in the New and Edit habit dialogs.
- Notes: a pill-shaped search field with a search icon.
- Tasks: a rounded quick-add field with a built-in "+" button, and round check circles.
- Habit rows show one round check button; Undo, Rest, Edit and Archive moved to a "⋮" menu.
- A cleaner look: a bottom tab bar for Home, Tasks, Habits and Notes, icon tiles on Home, large screen titles, and "+" buttons instead of the old text Back buttons.
- Habit reminder engine: a habit's daily reminder rings only on days it's due and not done yet, and Done on the notification checks off a yes/no habit. Setting the time from the app comes next.
- Edit a habit: change its name, goal, unit or days. How it's tracked stays the same, so its history keeps its meaning.
- Archived habits: open Archived from the Habits screen to restore a habit or delete it.
- Habit streaks: daily and weekday habits show their current streak and best. Rest days and streak freezes keep a streak going; history is never changed to save one.
- Habits screen: create a habit, check it off, log an amount or minutes, undo, take a rest day, or archive it. Open it from Habits on the home screen.
- Habit logic: schedules with rest days, check-ins, undo, corrections and daily progress for yes/no, amount, duration and multi-check-in habits.
- Habit storage (Room v5): habits with their schedules, and an append-only log of check-ins, undos and corrections. Not shown in the app yet.
- Custom reminder times (minutes, hours or days before, up to 30 days), and a 1 hour snooze button on reminder notifications.
- Completing a task in the app, or removing its reminder, takes its reminder notification off the screen.
- Task editor Reminders section: remind at the due time or 10 minutes, 1 hour or 1 day before, with Repeat until done. Asks for notification permission and explains when reminders may ring late.
- Reminder notifications have Done and Snooze buttons. Done also completes the task. Reminders can repeat every 15 minutes until done.
- Reminder alarms: they ring at the exact minute when Android allows it, and are set again after a reboot, an update or a time-zone change.
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

### Fixed

- Checking a habit off in the app now clears today's reminder notification.

### Changed

- Backend: NestJS 12 on Node.js 26 LTS; Jest runs with VM modules to load ESM-only packages.
- Android toolchain: Gradle 9.9, AGP 9.5, Kotlin 2.4, compileSdk 37, targetSdk 36, JDK 21, plus current AndroidX libraries.
- `android-ci` uses v5 of the checkout, setup-java, setup-gradle and upload-artifact actions.
