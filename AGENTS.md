# Repository Guidelines

## Project Structure & Module Organization
- Multimodule Kotlin project with public modules like `coil`, `coil-core`, `coil-compose`, network adapters, and artifacts under `internal/*` and `samples/*`.
- Source sets follow `src/commonMain`, `src/jvmMain`, `src/androidMain`, plus matching `*Test` directories (for example `coil-core/src/commonTest`).
- Shared test utilities live in `coil-test`, while platform demos reside in `samples/view` and `samples/compose`.

## Build, Test, and Development Commands
- `./gradlew build` – compile every module and produce distributables.
- `./gradlew check` – run unit tests and verification tasks across targets.
- `./gradlew apiCheck` – verify binary compatibility for public APIs.
- `./gradlew spotlessApply` / `spotlessCheck` – format Kotlin + KTS sources or validate formatting.
- Samples: `./gradlew :samples:view:installDebug` (Android) or `./gradlew :samples:compose:run` (Desktop preview).

## Coding Style & Naming Conventions
- Kotlin-first codebase, 4-space indent, LF endings, ~100 char guideline, trailing commas allowed where supported.
- Run Spotless (ktlint) before committing; avoid manual formatting of generated code.
- Naming: classes/objects UpperCamelCase, functions/props lowerCamelCase, constants UPPER_SNAKE_CASE, packages lowercase.

## Testing Guidelines
- Place tests inside the matching `src/<target>Test` tree; prefer deterministic mocks over live network.
- Run `./gradlew check` for JVM/common tests and `./gradlew connectedDebugAndroidTest` for device tests (emulator required).
- Screenshot tests use `./verify_screenshot_tests.sh`; update baselines with `./record_screenshot_tests.sh`.

## Commit & Pull Request Guidelines
- Write commits in imperative mood, ideally Conventional Commits (e.g., `feat: add gif sample`).
- Before pushing, run `./test.sh`, `spotlessCheck`, and `apiCheck` when APIs change.
- PRs should describe intent, link related issues, and include screenshots or GIFs for visual updates.

## Security & Configuration Tips
- Target JDK 17 locally (`JAVA_VERSION=17`), keep dependencies deterministic, and avoid embedding secrets in code or tests.
