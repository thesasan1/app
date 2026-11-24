# Contributing

All work reaches `main` through a Pull Request. The project owner has delegated merging to the agent (ADR-012): PRs are squash-merged once checks are green.

## Branches

| Prefix | Use |
|---|---|
| `main` | Stable, approved code. Protected. No direct pushes. |
| `feature/<short-name>` | New functionality |
| `fix/<short-name>` | Bug fixes |
| `refactor/<short-name>` | Behavior-preserving restructuring |
| `docs/<short-name>` | Documentation-only changes |
| `hotfix/<short-name>` | Urgent production fixes |

Branches start from the latest `main`. A `develop` branch is not used until parallel release trains require it (ADR-011). Never force-push a branch someone else is reviewing.

## Commits

[Conventional Commits](https://www.conventionalcommits.org/): `feat(tasks): add recurrence expansion`, `fix(reminders): rebuild alarms after timezone change`, `docs: …`, `test: …`, `chore(ci): …`. Scopes match module names. One logical change per commit.

## Pull Requests

- One coherent, reviewable unit per PR; split large features into dependent PRs.
- Use the PR template. Every section is required; write "Not applicable" when it is.
- Report test commands and their **actual** results: Passed / Failed / Not run / Blocked by environment.
- Reference requirement IDs from `docs/PRD.md` (e.g., `TASK-03`).
- Update `docs/PROGRESS.md`, `CHANGELOG.md`, and any affected docs in the same PR.
- CI must be green before review is requested (from Phase 1).
- Squash-merge after checks pass (by the agent under ADR-012, or by the owner).

## Recommended repository settings (owner)

- Protect `main`: require PR, 1 approval, required status checks (`android-ci`, `backend-ci` once added), linear history, no force pushes, no deletions.
- Enable secret scanning with push protection, Dependabot alerts, and private vulnerability reporting.
