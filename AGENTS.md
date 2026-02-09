# Repository Guidelines

## Project Structure & Module Organization
This is a Kotlin Multiplatform monorepo built with Gradle. Public library modules live at the repository root (for example `coil-core`, `coil-compose`, `coil-network-okhttp`, `coil-gif`, `coil-svg`, `coil-video`, `coil-test`, `coil-bom`). Internal tooling and test support modules are under `internal/`, and runnable examples are under `samples/` (`samples:compose`, `samples:compose-android`, `samples:view`).

Most modules use source sets like `src/commonMain`, `src/commonTest`, `src/androidMain`, `src/androidUnitTest`, and `src/androidInstrumentedTest`. Documentation content lives in `docs/`.

## Build, Test, and Development Commands
- `./gradlew spotlessCheck`: run Kotlin formatting/lint checks.
- `./gradlew lint`: run Android lint checks.
- `./gradlew checkLegacyAbi`: verify public API compatibility.
- `./gradlew updateLegacyAbi`: refresh ABI baselines when API changes are intentional.
- `./test.sh`: full local validation used by maintainers (style + unit/instrumentation/screenshot tests).
- `./gradlew allTests testDebugUnitTest`: run core unit tests quickly.
- `./gradlew connectedDebugAndroidTest`: run Android instrumentation tests (emulator/device required).

## Coding Style & Naming Conventions
Follow `.editorconfig`: UTF-8, LF endings, 4-space indentation, max line length 100, final newline, no trailing whitespace. Kotlin formatting is enforced via Spotless + ktlint (`intellij_idea` style with project-specific rule overrides).

Match existing naming patterns: packages under `coil3.*`, types in `UpperCamelCase`, members in `lowerCamelCase`, constants in `UPPER_SNAKE_CASE`. Keep file names aligned with primary types (for example `NetworkFetcher.kt`).

## Testing Guidelines
Tests use Kotlin Test/JUnit, Robolectric, Android instrumentation, and screenshot tooling (Paparazzi/Roborazzi/Compose screenshot validation). Name test files `*Test.kt` and place them in the appropriate source set (`commonTest`, `androidUnitTest`, etc.).

For bug fixes, add or update a failing test first when possible. Run `./test.sh` before opening a PR.

## Commit & Pull Request Guidelines
Use concise, imperative commit subjects (for example `Fix macOS test flakiness`). Dependency updates commonly use Conventional Commit style (for example `fix(deps): ...` or `chore(deps): ...`).

PRs should include:
- A clear summary of what changed and why.
- Linked issue(s) when applicable.
- Test evidence (commands run, and screenshots for UI/screenshot test changes).
- Updated ABI files when public API changes are intentional.

Before submitting, ensure `./test.sh` passes locally.
