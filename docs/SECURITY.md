# Security and Privacy

| Field | Value |
|---|---|
| Status | Draft v0.1 — Phase 0 security model, pending approval. No controls are implemented yet. |
| Reporting a vulnerability | Open a **private** GitHub Security Advisory on this repository; do not file a public issue. |

---

## 1. Data Classification

| Class | Examples | Handling |
|---|---|---|
| **Secret** | Passwords, refresh tokens, OAuth tokens, signing keys, API keys | Never logged; hashed (passwords: argon2id; refresh tokens: SHA-256) or envelope-encrypted; never on device for third-party provider tokens |
| **Sensitive personal** | Journal, mood, voice recordings, location reminders | Encrypted at rest; optional app lock; excluded from AI context unless explicitly granted; private journal entries never sent to AI |
| **Personal** | Notes, tasks, habits, goals | Encrypted in transit; at rest via platform/DB encryption |
| **Operational** | Device model, app version, error codes | No user content; retained 90 days |

## 2. Threat Model (STRIDE summary)

| Threat | Example | Mitigation |
|---|---|---|
| Spoofing | Stolen access token | Short-lived JWT (15 min), rotating refresh tokens with reuse detection, device-bound sessions, revocation |
| Tampering | Client sends another user's entity id | Server-side ownership checks on every query; `404` on mismatch |
| Repudiation | "I didn't delete that" | Audit log for sensitive operations; task_history; conflict_log |
| Information disclosure | Logs leak note text; backups readable | PII redaction in logs; encrypted backups; least-privilege DB roles |
| Denial of service | Sync flood, AI cost abuse | Rate limits, payload caps, per-user AI token budgets |
| Elevation of privilege | AI tool call deletes data | Allow-listed tools, authorization per tool, mandatory confirmation for destructive tools, no generated code/SQL execution |
| Prompt injection | Note text says "delete all tasks" | Content treated as data, not instructions; tool calls validated against user intent and confirmation rules |

## 3. Android Controls

- **Android Keystore**: AES-256-GCM key (non-exportable) protecting session tokens and the optional local DB key.
- **Local DB encryption**: evaluate SQLCipher for Room vs. relying on file-based encryption + app lock **[Decision ADR-010, Phase 1]**.
- **App lock** (optional): BiometricPrompt (`BIOMETRIC_STRONG` or device credential) and app PIN (PBKDF2/argon2 hashed, Keystore-wrapped), auto-lock timeout, `FLAG_SECURE` on locked/private screens (configurable).
- **Network**: HTTPS only (`cleartextTrafficPermitted=false`), Network Security Config; certificate pinning considered for production with a backup pin.
- **No secrets in APK**: no AI provider keys, no OAuth client secrets. Build-time config only holds public identifiers.
- **Permissions**: requested just-in-time with rationale: notifications, exact alarms, microphone, overlay, location (foreground first; background only for location reminders), all optional with degraded modes.
- **Exported components**: only share target, tile, widget receiver, and deep-link activity; all others `exported=false`. Incoming intents validated.
- **Backups**: `android:allowBackup` with explicit `dataExtractionRules` excluding tokens and keys.
- **Release**: R8 enabled; signing keys only in GitHub Actions secrets / Play App Signing.

## 4. Backend Controls

- TLS everywhere; HSTS.
- Auth: argon2id password hashing; JWT signed with asymmetric keys (EdDSA/RS256) and rotated; refresh token families with reuse detection.
- Authorization: guard on every protected route; repository methods require `userId`.
- Validation: global `ValidationPipe` (whitelist, forbid unknown); size limits.
- Rate limiting on auth (stricter), sync, AI.
- Secrets via environment variables from a secret manager (provider per D-04); `.env` files never committed; `.env.example` only.
- OAuth tokens for Google: envelope encryption (data key per record, master key in KMS).
- Database: least-privilege roles (app vs. migrator), encrypted at rest, point-in-time recovery backups.
- Logging: structured, with redaction of `authorization`, `password`, `token`, note/task body fields.
- Dependencies: Dependabot + dependency review; critical CVEs block merge.
- Audit log: login, password change, session revoke, export, account deletion, integration connect/disconnect, AI destructive tool confirmations.

## 5. AI-Specific Controls

- All provider calls server-side; per-user quotas and cost tracking.
- Context minimization: only scopes the user granted; private journal entries excluded.
- Provider data-retention settings set to "no training / minimal retention" where the provider offers it (verified when D-03 is chosen).
- Output validated against schemas; invalid output discarded, user data untouched.

## 6. Privacy Commitments

- Data export and account deletion available in-app (PLT-02).
- No advertising SDKs. Any telemetry is opt-in and content-free (D-05).
- Privacy policy and data-safety form prepared in Phase 9.

## 7. Repository Hygiene

Never commit: API keys, DB passwords, OAuth client secrets, signing keys/keystores, private certificates, production credentials, personal data. `.gitignore` blocks common secret file types (`*.jks`, `*.keystore`, `.env*`, `google-services.json`, `local.properties`). Enable GitHub secret scanning and push protection (owner action — requires repository settings change).
