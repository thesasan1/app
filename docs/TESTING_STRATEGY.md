# Testing Strategy

| Field | Value |
|---|---|
| Status | Draft v0.1 — Phase 0. Test infrastructure is created in Phase 1; no tests exist yet. |

---

## 1. Principles

- Testing is part of implementation; a feature PR without tests for its logic is not done (`AGENTS.md` §19).
- Results are reported honestly as **Passed / Failed / Not run / Blocked by environment**.
- CI results outrank local claims. A PR is not "verified" by code inspection alone.
- Deterministic tests: injected `Clock`, `TimeZone`, dispatchers, UUID generator; no real network in unit tests.

## 2. Test Pyramid — Android

| Level | Tooling | Scope | Runs |
|---|---|---|---|
| Unit (JVM) | JUnit 5, MockK, Turbine, kotlinx-coroutines-test | Domain use cases, recurrence expansion, streak calculation, date parsing (EN/FA), conflict rules, mappers | Every PR |
| ViewModel | JUnit 5, Turbine, fakes | UiState transitions, error and empty states | Every PR |
| Repository / DB | Robolectric + in-memory Room, `MigrationTestHelper` | DAOs, queries, FTS, transactional outbox, every migration | Every PR |
| Worker | `work-testing` (`TestListenableWorkerBuilder`) | SyncWorker, reminder rebuild worker | Every PR |
| Compose UI | Compose UI Test (Robolectric for speed; instrumented for critical flows) | Screens, a11y semantics, RTL | Every PR (Robolectric); nightly (emulator) |
| Screenshot | Roborazzi or Paparazzi (Phase 1 decision) | Design-system components: light/dark × LTR/RTL × font 1.0/2.0 | Every PR |
| Navigation | Compose navigation testing | Deep links, back stack | Every PR |
| Notification / alarm | Instrumented on emulator (API 26, 31, 34, latest) | Exact/inexact scheduling, reboot rebuild, notification actions | Nightly + release |
| Performance | Macrobenchmark, Baseline Profile generator | Startup, scroll jank, capture latency | Weekly + release |

Critical scenarios that must have tests before the related feature merges:

- Reminder fires once after reboot, timezone change, and DST transition (both directions).
- Autosave survives process death mid-edit.
- Habit streak is never altered by history-preserving operations (freeze/correction).
- Offline create → edit → delete → reconnect produces correct server state with no loss.
- Same note body edited on two devices creates a conflict copy, not an overwrite.
- AI unavailable → capture still saved, fallback parse applied.

## 3. Backend

| Level | Tooling | Scope |
|---|---|---|
| Unit | Jest | Services, validators, conflict resolution, AI output validation, tool schemas |
| Integration | Jest + Testcontainers (PostgreSQL, Redis) | Repositories, migrations, jobs |
| API / e2e | Supertest against Nest app | Auth, authorization (cross-user access returns 404), validation errors, idempotency, rate limiting |
| Sync | Scenario tests with simulated devices | Push/pull ordering, conflict cases from ARCHITECTURE §3.2 |
| AI | Fake `LlmProvider` returning recorded/adversarial outputs | Schema rejection, tool allow-list, destructive confirmation, timeout fallback |
| Contract | OpenAPI drift check | Spec matches implementation |
| Static | ESLint, `tsc --noEmit`, Prettier check | Every PR |
| Security | `npm audit` / dependency review, secret scanning | Every PR |

## 4. CI Gates (Phase 1 onward)

| Workflow | Jobs | Required to merge |
|---|---|---|
| `android-ci.yml` | `./gradlew spotlessCheck detekt lint testDebugUnitTest assembleDebug` + screenshot verify | Yes |
| `backend-ci.yml` | `npm ci && npm run lint && npm run typecheck && npm test && npm run test:e2e` | Yes |
| Dependency review | GitHub dependency-review action | Yes |
| Nightly | Emulator instrumented tests (matrix) | No (alerts) |

## 5. Coverage Targets

Coverage is a signal, not a goal: domain ≥ 90% line coverage, data ≥ 75%, backend services ≥ 85%. Critical scenarios in §2 are mandatory regardless of percentages.

## 6. Test Data and Privacy

Fixtures are synthetic. No production data in tests, ever. Persian fixtures include mixed-direction strings, Persian digits, and Jalali dates.

## 7. Reporting Format (in every PR)

```
Testing
- ./gradlew testDebugUnitTest      → Passed (124 tests)
- ./gradlew lint                    → Passed
- npm run test:e2e                  → Not run (reason)
- Instrumented alarm tests          → Blocked by environment (no emulator)
```
