# Project Structure

Top-level map of this repo. Build outputs, IDE caches, and Gradle
wrappers are omitted — this is the stuff you'll actually touch.

```
whispry/
├── app/                      # The Android app module (everything below lives under
│                              # app/src/main/java/com/example/whispry/)
├── docs/                     # Design/architecture notes and PRDs (see below)
├── gradle/                   # Gradle wrapper + version catalog (libs.versions.toml)
├── keystore/                 # Local signing keystore — gitignored, never committed
├── .github/                  # Issue/PR templates, release CI workflow
├── build.gradle.kts,
│   settings.gradle.kts       # Top-level Gradle config
├── LICENSE, NOTICE           # AGPL-3.0 + attribution requirement (see LICENSE)
├── CONTRIBUTING.md           # How to build, branch, and open a PR
├── CODE_OF_CONDUCT.md        # Contributor Covenant v2.1
├── SECURITY.md               # How to report a vulnerability
├── CHANGELOG.md              # Keep a Changelog-format release history
└── PRD-*.md, session-slices.md,
    .opencode/, hooks/, skills/, opencode.json
                              # AI-coding-agent tooling and planning docs used during
                              # development — not part of the app itself, kept for
                              # transparency into how this repo is worked on.
```

## `app/src/main/java/com/example/whispry/`

The app follows a fairly standard Compose + MVI layering, with a few
self-contained `features/` slices for things that don't need to touch
the rest of the app.

| Package | Purpose |
| --- | --- |
| `data/local/` | Room entities/DAOs and the DataStore-backed settings/preferences layer (`datasource/`). |
| `data/remote/` | Retrofit API services and DTOs for the transcription/formatting AI providers. |
| `data/repository/` | Repository implementations bridging `domain/` interfaces to the local/remote data sources. |
| `di/` | Hilt modules (`AppModule`, `NetworkModule`, `DatabaseModule`) wiring the above together. |
| `domain/model/` | Plain Kotlin domain models (no Android/Compose dependencies). |
| `domain/repository/` | Repository interfaces the presentation layer depends on. |
| `domain/usecase/` | Single-purpose use cases (e.g. formatting a transcript). |
| `navigation/` | Type-safe `Route` sealed interface and the `WhispryNavHost` that wires every screen together. |
| `notification/` | Notification channels and the premium-feature reminder worker. |
| `presentation/` | Screens that aren't split into their own `features/` slice: `main/` (home), `settings/`, `history/` (transcript library), `presets/`, `about/`, `onboarding/`, plus `common/` shared composables. Each screen follows MVI: `*Contract.kt` (State/Intent), `*ViewModel.kt`, `*Screen.kt`. |
| `service/` | The background accessibility service, the floating widget overlay (`FloatingWidgetManager`, `WidgetSwitchVisual`, gesture resolvers), the keyboard-logo trigger, and the recording pipeline (`BubbleService`). This is where most of the always-on, non-Compose-lifecycle logic lives. |
| `ui/components/` | Shared design-system composables (cards, buttons, the liquid-touch press effect). |
| `ui/theme/` | Color tokens, accent presets, typography. |
| `ui/util/` | Compose modifier/gesture helpers used across the design system. |
| `util/` | General-purpose helpers (haptics, etc.) with no Android UI dependency. |

## `features/`

Self-contained slices, each with its own `presentation/` (and, where
needed, `data/`/`domain/`) — these are the app's opt-in productivity
tools, kept separate from the core trigger/transcription flow so they're
easy to review or disable independently:

| Feature | Purpose |
| --- | --- |
| `expander/` | Text Expander — expands short typed triggers into longer snippets. |
| `hiddenapps/` | Per-app suppression list (both floating widgets stay hidden in chosen apps). |
| `memory/` | Persistent "memory" entries the AI formatting step can draw on. |
| `myinfo/` | User-supplied facts (name, etc.) available to the formatting step. |
| `tone/` | App-aware tone — per-app output style presets. |
| `voicecommand/` | Spoken command router (e.g. "expand X", "calculate Y"). |

## `docs/agents/`

Instructions for AI coding agents working in this repo (issue-tracker
conventions, triage labels, domain-doc layout) — referenced from
`CLAUDE.md`, not needed to build or contribute by hand.
