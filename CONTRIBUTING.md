# Contributing to Whispry

Thanks for wanting to help out. Bug reports, feature requests, and code
are all welcome.

## Propose before you build

For anything bigger than a bug fix or small tweak — a new feature, a
behavior change to an existing trigger/gesture, a design-system change —
open an [Issue](https://github.com/cosmictaserdev-creator/whispry/issues)
or [Discussion](https://github.com/cosmictaserdev-creator/whispry/discussions)
first. It's a quick way to confirm the direction before you spend time on
code that might not land as-is.

## Getting started

1. Fork the repository.
2. Clone your fork and open it in **Android Studio** (Ladybug or newer
   recommended — the project targets `compileSdk 37`).
3. Create a branch off `master`:
   ```
   git checkout -b feature/short-description
   ```

### Build setup

Whispry is a single-module Android app — Kotlin, Jetpack Compose, Hilt
for DI, Room + DataStore for local data, Retrofit/OkHttp for network
calls.

- **Min SDK**: 26 · **Target SDK**: 34 · **Compile SDK**: 37
- No API keys or secrets are needed to build a debug APK — transcription
  and formatting API keys are entered per-user at runtime in Settings.
- Command-line build (from the repo root):
  ```
  ./gradlew assembleDebug          # debug APK
  ./gradlew testDebugUnitTest      # unit tests
  ./gradlew compileDebugKotlin     # fast compile check while iterating
  ```
- To build a **signed release APK** you additionally need a keystore and
  a `keystore.properties` file at the repo root (see `app/build.gradle.kts`
  for the exact keys it reads: `storeFile`, `storePassword`, `keyAlias`,
  `keyPassword`). You don't need this for day-to-day contributions —
  `assembleDebug` is enough to run and test the app.

### Code style

- Follow the existing Kotlin/Compose conventions in the file you're
  editing over anything else — match what's already there.
- Run Android Studio's built-in inspections (`Analyze → Inspect Code`) or
  `./gradlew lint` before opening a PR; there's no separate formatter
  config beyond the default Kotlin style.
- Prefer reusing an existing composable/util/pattern over adding a new
  one — check `ui/components/` and `ui/util/` before writing a new
  helper.

### Commit messages

Keep it simple — a short imperative summary, optionally in
[Conventional Commits](https://www.conventionalcommits.org/) style
(`fix:`, `feat:`, `refactor:`, `docs:`, …) if that's natural for the
change. What matters more than the prefix is that the message says *why*,
not just *what* — the diff already shows what changed.

## Opening a pull request

Before you open the PR:

- [ ] `./gradlew assembleDebug` builds cleanly
- [ ] `./gradlew testDebugUnitTest` passes
- [ ] No new lint errors (`./gradlew lint`)
- [ ] If you touched a screen or gesture, you actually ran it on a device
      or emulator — Compose previews and unit tests don't catch feel/UX
      regressions
- [ ] PR description explains **what** changed and **why**, and links the
      Issue/Discussion it follows up on if there is one

Then open the PR against `master` and fill in the template. A maintainer
will review as soon as they can — this is a community project, so review
turnaround is best-effort, not guaranteed.

## Project layout

See [PROJECT_STRUCTURE.md](PROJECT_STRUCTURE.md) for a map of the
codebase before you go digging for where something lives.

## License

By contributing, you agree your contribution is licensed under the same
terms as the rest of the project — see [LICENSE](LICENSE).
