# Known Issues, Risks and Dependencies

## Known Issues

| ID | Issue | Status |
|---|---|---|
| KI-001 | Backend code does not exist yet; the Android app is a skeleton with only a dashboard shell. | Expected — Phase 1/2 |
| KI-002 | The agent sandbox is linux/arm64: pure-JVM modules build and test locally, but Android modules cannot (Android build-tools/aapt2 are x86-64 only on Linux). Android build, lint and tests are verified by GitHub Actions CI. | Environment limitation; mitigated by CI |
| KI-003 | `android-ci`, `backend-ci` and `dependency-review` exist but are not required status checks on `main`. | Owner action (branch protection) |
| KI-007 | Backend has no database, auth, or worker yet; readiness reports no dependency checks. | Phase 1 PR 5b / Phase 6 |
| KI-008 | Screenshot tests record images in CI (artifact `designsystem-screenshots`) but are not yet compared against committed reference images, so visual regressions are not caught automatically. References must be recorded on CI-compatible hardware and committed; the sandbox cannot run Android builds (KI-002). | Phase 1 follow-up |
| KI-005 | targetSdk was 35; raised to 36 by ADR-019. Re-check the Play requirement before the first upload. | Resolved (toolchain PR) |
| KI-006 | No launcher icon yet (lint `MissingApplicationIcon` warning). | Design-system PR |
| KI-009 | The Room database is protected only by Android file-based encryption. Whether to add SQLCipher is open (ADR-010) and should be settled before Phase 2 stores real notes and journal entries. | Owner decision |
| KI-010 | Robolectric downloaded its Android runtime from Maven Central during CI tests, and rate limiting (HTTP 429) failed runs. CI now caches the runtime, fetches it with retries, and runs tests offline. Tests run on SDK 34 and Robolectric also loads the compileSdk (36) runtime, which needs JDK 21; changing either, or the Robolectric version, means updating `ROBOLECTRIC_JARS` in `android-ci.yml`. | Mitigated (PR #14) |
| KI-011 | Dependabot #11, #15, #16 and #18 (AndroidX, Kotlin, Hilt, Roborazzi) are superseded by the toolchain upgrade (ADR-019) and closed once it merges. #12 (NestJS 12, ESM-only) still needs its own backend PR. | Android: resolved; backend: open |
| KI-004 | Branch protection and secret scanning on `main` are not confirmed enabled (requires owner action in repository settings). | Owner action |

## Technical Risks

| ID | Risk | Impact | Likelihood | Mitigation |
|---|---|---|---|---|
| R-01 | Android background limits / OEM battery killers delay reminders | High (P2 trust) | High | Exact alarms when granted, alarm rebuild on boot/time change, battery-optimization guidance screen, reliability telemetry (opt-in), OEM matrix testing |
| R-02 | Exact-alarm permission restrictions (Android 12+/14+) | High | Medium | `USE_EXACT_ALARM` only if eligible under Play policy (alarm/reminder app); otherwise `SCHEDULE_EXACT_ALARM` with user consent; inexact fallback stated in UI |
| R-03 | Floating overlay (`SYSTEM_ALERT_WINDOW`) and foreground-service-type policy on Play | Medium | Medium | Overlay optional; in-app FAB, tile, widget as primary; review Play policy before Phase 3 |
| R-04 | Persian speech-to-text quality on-device | Medium | High | D-07: server STT fallback; user review before save |
| R-05 | Persian NLP for dates/intents (Jalali, colloquial forms, digits) | Medium | High | Deterministic EN/FA parser with extensive test corpus; LLM fallback |
| R-06 | Sync conflicts causing perceived data loss | High | Medium | Conflict copies, conflict log, sync history UI, scenario tests |
| R-07 | Scope breadth (20+ modules) vs. quality | High | High | Strict phase gating, focused PRs, shared design system and data patterns |
| R-08 | AI cost overruns / provider outage | Medium | Medium | Quotas, caching, circuit breaker, deterministic fallbacks |
| R-09 | Google API verification for sensitive scopes (Calendar) takes weeks | Medium | High | Start OAuth consent-screen verification early in Phase 6; minimal scopes |
| R-10 | Rich-text editor in Compose lacks a mature first-party solution | Medium | Medium | Evaluate libraries in Phase 2 spike; build on `BasicTextField` + span model if needed |
| R-11 | Room FTS tokenization for Persian | Low–Medium | Medium | Verify ICU tokenizer availability; fall back to normalized-text LIKE search with Persian normalization (ی/ي, ک/ك) |
| R-12 | Privacy regulations (GDPR-like) for EU users, data of Iranian users and sanctions-related service availability (Google APIs, AI providers, Play billing) | High | Unknown | **Legal/compliance review required by owner** before launch markets are decided |

## External Dependencies

| Dependency | Needed for | Owner action |
|---|---|---|
| Android SDK / JDK 21 in CI | Builds and tests | Provided by GitHub Actions runners (Phase 1) |
| Google Cloud project + OAuth consent screen | Google Sign-In, Calendar, Tasks | Owner creates project and credentials (secrets stored in CI/secret manager) |
| AI provider account (paid) | Phase 5 | Owner selects (D-03) and provisions key |
| Cloud hosting account | Phase 6 | Owner selects (D-04) |
| Play Console account + signing | Release | Owner |
| Font licenses (Inter, Vazirmatn — OFL) | Design system | Verify and include license texts |
