# Changelog

All notable changes are documented here. Format based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/); versions follow [Semantic Versioning](https://semver.org/) once releases begin.

## [Unreleased]

### Added

- Backend skeleton (`backend/`): NestJS 11, zod-validated environment, `/v1/health/live` and `/v1/health/ready`, structured logging with redaction and request ids, RFC 9457 problem details, global validation, security headers, OpenAPI document with drift check, unit and e2e tests.
- `backend-ci` GitHub Actions workflow.
- README instructions for installing the CI-built debug APK.
- Android project skeleton (`android/`): Gradle version catalog and convention plugins, `:app`, `:core:common`, `:core:designsystem`, `:feature:dashboard` (greeting + date + empty state, English and Persian), Hilt, type-safe Navigation, JUnit 5 unit tests.
- `android-ci` GitHub Actions workflow: unit tests, Android lint, debug APK artifact.
- ADR-012 (agent merge authority) and ADR-013 (Android skeleton baseline).
- Phase 0 product and technical foundation: PRD, architecture, database model, API specification, design system, development roadmap, testing strategy, security model, decisions log, known issues and risks, progress tracking, release notes placeholder.
- GitHub workflow conventions: `CONTRIBUTING.md`, Pull Request template, issue templates, `.gitignore`, `.editorconfig`.

### Changed

- `android-ci` uses v5 of the checkout, setup-java, setup-gradle and upload-artifact actions.
