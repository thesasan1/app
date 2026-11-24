# Design System — Foundation

| Field | Value |
|---|---|
| Status | Draft v0.1 — Phase 0 foundation, pending approval. Tokens are implemented in `:core:designsystem` in Phase 1. |
| Direction | Premium Minimalism + Modern Productivity + Refined Motion Design |
| Base | Material 3 (for accessibility, theming and component behavior), with product-specific tokens and components on top |

---

## 1. Design Principles

1. **Calm by default.** Generous whitespace, restrained color; color is used to mean something (priority, progress, state).
2. **Content first.** Chrome recedes; the user's words and progress are the hero.
3. **One-thumb speed.** Primary actions sit in the lower half of the screen; capture is always one gesture away.
4. **Honest feedback.** Every action acknowledges itself (motion, haptic, or message) within 100 ms.
5. **Bilingual by design.** Every component is built and reviewed in both LTR (English) and RTL (Persian).

## 2. Color System

Tokens are semantic; raw palette values never appear in feature code.

### 2.1 Core palette (initial proposal)

| Token | Light | Dark | Use |
|---|---|---|---|
| `color.bg.canvas` | `#F7F7F5` | `#0E0F11` | App background |
| `color.bg.surface` | `#FFFFFF` | `#16181B` | Cards, sheets |
| `color.bg.surfaceRaised` | `#FFFFFF` | `#1E2024` | Elevated sheets, menus |
| `color.bg.sunken` | `#EFEFEC` | `#0A0B0C` | Inputs, wells |
| `color.text.primary` | `#16181B` | `#F2F2F0` | |
| `color.text.secondary` | `#5C6168` | `#A3A8AF` | |
| `color.text.tertiary` | `#8A8F96` | `#6F747B` | Placeholders, metadata |
| `color.border.subtle` | `#E4E4E0` | `#26292E` | Dividers |
| `color.accent` | `#4F5BD5` (Indigo) | `#8B93F0` | Primary actions, focus — user-customizable |
| `color.accent.onAccent` | `#FFFFFF` | `#0E0F11` | |
| `color.success` | `#2F9E6B` | `#4CC38A` | Completion, streaks |
| `color.warning` | `#C98A12` | `#E8B04A` | Due soon |
| `color.danger` | `#D2453C` | `#F06A60` | Overdue, destructive |
| `color.info` | `#2F7FD1` | `#5FA3EC` | |

Accent options (user choice): Indigo, Ocean, Emerald, Amber, Rose, Graphite. Each accent ships light/dark variants verified at ≥ 4.5:1 contrast against its text color. Android 12+ dynamic color is offered as an optional "Wallpaper" accent.

### 2.2 Priority colors

`priority.urgent = danger`, `priority.high = warning`, `priority.medium = accent`, `priority.low = text.tertiary`. Priority is never communicated by color alone (also icon + label).

### 2.3 Habit heatmap scale

5 steps from `bg.sunken` to `success` (0, 25, 50, 75, 100% of target); colorblind-safe check required.

## 3. Typography

| Script | Family (proposal) | Notes |
|---|---|---|
| Latin | **Inter** (variable) | Open license (OFL), excellent UI legibility |
| Persian/Arabic | **Vazirmatn** (variable) | OFL, designed for UI, harmonizes with Inter metrics |
| Numerals | Locale-aware: Persian digits (۰–۹) in `fa`, Latin in `en`; user can override | |

Licenses must be verified and included in `third_party/licenses` when added (Phase 1).

| Token | Size / Line height (sp) | Weight | Use |
|---|---|---|---|
| `display` | 36 / 44 | 600 | Dashboard greeting |
| `headline` | 28 / 36 | 600 | Screen titles |
| `title.lg` | 22 / 28 | 600 | Section headers |
| `title.md` | 18 / 24 | 600 | Card titles |
| `body.lg` | 17 / 26 | 400 | Note editor |
| `body.md` | 15 / 22 | 400 | Default text |
| `body.sm` | 13 / 18 | 400 | Metadata |
| `label.md` | 14 / 20 | 500 | Buttons, chips |
| `label.sm` | 12 / 16 | 500 | Badges |

Persian line heights get +2sp to fit diacritics. All sizes in `sp` and scale with system font size up to 200% without clipping.

## 4. Spacing, Layout, Shape

- **Spacing scale (dp):** 0, 2, 4, 8, 12, 16, 20, 24, 32, 40, 48, 64. Base unit 4.
- **Screen margins:** 20dp phones, 24dp ≥ 600dp width; content max width 720dp on tablets.
- **Grid:** 4-column phone, 8-column tablet, 16dp gutters.
- **Radius tokens:** `xs 6`, `sm 10`, `md 14` (cards), `lg 20` (sheets), `xl 28` (FAB, large surfaces), `full` (chips, avatars).
- **Touch targets:** ≥ 48×48dp always.

## 5. Surfaces and Elevation

Elevation is expressed mostly by **tone**, not shadow (premium-minimal look):

| Level | Light | Dark | Used for |
|---|---|---|---|
| 0 canvas | `bg.canvas` | `bg.canvas` | Background |
| 1 surface | `bg.surface` + 1dp `border.subtle` | `bg.surface` | Cards, list groups |
| 2 raised | `bg.surfaceRaised` + soft shadow (y=4, blur=16, 6% black) | `bg.surfaceRaised` | Bottom sheets, menus |
| 3 overlay | Scrim 32% black | Scrim 56% black | Modal backdrop |

## 6. Iconography

Single outlined icon family with 1.75dp stroke at 24dp (proposal: **Lucide** (ISC) or Material Symbols Rounded (Apache-2.0) — final pick in Phase 1, recorded in DECISIONS). Directional icons mirror automatically in RTL; non-directional (e.g., clock, check) never mirror.

## 7. Motion

| Token | Duration | Easing | Use |
|---|---|---|---|
| `motion.instant` | 90 ms | standard | Press states, toggles |
| `motion.quick` | 160 ms | emphasized-decelerate | Checkbox check, chip select |
| `motion.base` | 240 ms | emphasized | Sheet open, card expand |
| `motion.slow` | 360 ms | emphasized | Screen transitions |
| `spring.gentle` | stiffness 400, damping 0.8 | spring | Drag, FAB, reorder |

Rules: motion explains spatial change; nothing blocks input; celebrations (streak milestones, achievements) ≤ 1.2 s and skippable; **reduced-motion** (system animator scale 0 or in-app setting) replaces movement with fades ≤ 120 ms. Haptics: `CONFIRM` on completion, `REJECT` on error, `CLOCK_TICK` on drag snap — all toggleable.

## 8. Component Library (Phase 1 foundation set)

| Component | Notes |
|---|---|
| `AppButton` (primary, secondary, tertiary, destructive; sizes S/M/L) | Loading state built in |
| `AppIconButton` | 48dp target |
| `AppTextField` / `AppSearchField` | Bidi-safe, supports Persian IME |
| `AppCheckbox` (animated) | Spring check + haptic |
| `AppChip` (filter, input, tag) | |
| `AppCard`, `AppListItem` | Swipe actions (complete / snooze / delete with undo) |
| `AppBottomSheet` | Fluid drag, keyboard-aware |
| `AppTopBar`, `AppNavigationBar` | |
| `CaptureFab` | Tap / long-press / drag semantics |
| `PriorityIndicator`, `DueDateBadge`, `TagPill` | |
| `ProgressRing`, `ProgressBar`, `StreakBadge`, `Heatmap` | |
| `EmptyState`, `LoadingState` (skeletons), `ErrorState` (retry) | Every screen uses these |
| `AppSnackbar` (with Undo) | Default feedback for reversible actions |
| `ConfirmDialog` | Required for destructive actions |

Each component: light/dark, LTR/RTL, font scale 100/200%, TalkBack semantics, preview + screenshot test (Paparazzi or Roborazzi — Phase 1 decision).

## 9. Interaction and System States

- States for every interactive component: enabled, pressed, focused (2dp accent ring), hovered (tablets/ChromeOS), disabled (38% opacity content), loading, error.
- **Empty states** teach the next action ("Capture your first thought") with one primary button.
- **Loading** uses skeletons for > 300 ms; nothing for shorter.
- **Errors** say what happened and what to do; data is never discarded on error.
- **Offline** is not an error: a subtle sync indicator shows `Synced`, `Syncing`, `Offline – saved on device`, `Needs attention`.

## 10. Accessibility Checklist

Dynamic type to 200% · TalkBack labels and roles on every control · contrast ≥ 4.5:1 text, ≥ 3:1 UI · 48dp targets · logical focus order (incl. RTL) · keyboard/D-pad navigation · no color-only meaning · reduced motion · captions/transcripts for voice captures.

## 11. RTL and Persian

- Use `start/end`, never `left/right`.
- Mixed-direction text wrapped with bidi isolation.
- Jalali calendar display option and Persian month names; week start configurable (Saturday default for `fa`).
- Screens reviewed in both directions before merge.
