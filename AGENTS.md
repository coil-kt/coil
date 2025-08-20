# Repository Guidelines

## Project Structure & Module Organization
- Multimodule Kotlin (KMP + Android) project managed by Gradle Kotlin DSL.
- Public modules: `coil`, `coil-core`, `coil-compose`, `coil-compose-core`, `coil-network-*`, `coil-gif`, `coil-svg`, `coil-video`, `coil-bom`, `coil-test`.
- Private/testing and samples live under `internal/*` and `samples/*`.
- Source layout per module: `src/commonMain`, `src/jvmMain`, `src/androidMain`, `src/*Test` (e.g., `commonTest`, `jvmTest`, `androidUnitTest`), and `src/androidInstrumentedTest` where applicable.

## Build, Test, and Development Commands
- `./gradlew build`: Compile all modules and assemble artifacts.
- `./gradlew check`: Run unit tests and verification tasks.
- `./gradlew apiCheck`: Validate public API (binary compatibility).
- `./gradlew spotlessApply` / `spotlessCheck`: Format or verify Kotlin/KTS style.
- `./test.sh`: CI-equivalent suite (API, unit, connected, screenshot tests).
- Screenshot tests: `./verify_screenshot_tests.sh` to verify; `./record_screenshot_tests.sh` to update snapshots.
- Instrumentation: `./gradlew connectedDebugAndroidTest` (requires emulator/device).
- Samples: `:samples:view:installDebug` (Android View); `:samples:compose:run` (Compose Desktop).

## Coding Style & Naming Conventions
- Kotlin-first codebase. Indent 4 spaces, LF endings, ~100 char line length, trailing commas allowed.
- Formatting via Spotless + ktlint. Run `./gradlew spotlessApply` before pushing.
- Names: classes/objects `UpperCamelCase`; functions/properties `lowerCamelCase`; constants `UPPER_SNAKE_CASE`; packages lowercase.

## Testing Guidelines
- Place tests under `src/<target>Test` (e.g., `commonTest`, `jvmTest`, `androidUnitTest`).
- Android instrumentation tests in `src/androidInstrumentedTest`; run `connectedDebugAndroidTest`.
- Screenshot tests use Paparazzi/Roborazzi; verify/update with the provided scripts.
- Prefer deterministic tests (no live network). Use helpers in `coil-test` and `internal:test-utils`.

## Commit & Pull Request Guidelines
- Commits: imperative mood; prefer Conventional Commits (e.g., `feat:`, `fix:`, `chore(deps):`). Reference issues/PRs when relevant.
- PRs: clear description, linked issues, and screenshots/GIFs for UI-visible changes.
- Before opening a PR: run `./test.sh`, ensure `spotlessCheck` passes, and run `apiCheck` for public API changes.

## Security & Configuration Tips
- Use JDK 17 (`JAVA_VERSION=17`) to match CI.
- Avoid committing secrets. Keep builds/tests deterministic; no live network in tests.
