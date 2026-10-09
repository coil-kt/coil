# Repository Guidelines

## Project Structure & Module Organization
This is a Kotlin Multiplatform monorepo built with Gradle. Public library modules live at the repository root (for example `coil-core`, `coil-compose`, `coil-network-okhttp`, `coil-gif`, `coil-svg`, `coil-video`, `coil-test`, `coil-bom`). Internal tooling and test support modules are under `internal/`, and runnable examples are under `samples/` (`samples:compose`, `samples:compose-android`, `samples:view`).

Most modules use source sets like `src/commonMain`, `src/commonTest`, `src/androidMain`, `src/androidHostTest`, and `src/androidDeviceTest`. Shared build logic and the custom source-set hierarchy live in `buildSrc/`; dependency versions are in `gradle/libs.versions.toml`. Documentation content lives in `docs/`.

## Build, Test, and Development Commands
- `./gradlew spotlessCheck`: run Kotlin formatting/lint checks.
- `./gradlew lint`: run Android lint checks.
- `./gradlew verifySkikoVersionsMatch`: check that Coil and Compose request matching Skiko versions.
- `./gradlew checkKotlinAbi`: verify public API compatibility.
- `./gradlew updateKotlinAbi`: refresh ABI baselines when API changes are intentional.
- `./test.sh`: run style and ABI checks plus unit, instrumentation, and screenshot tests.
- `./test.sh --skip-instrumentation-tests`: run the suite without connected-device tests.
- `./test.sh --skip-checks`: skip style and ABI checks when they have already passed.
- `./gradlew allTests testDebugUnitTest`: run aggregate multiplatform and Android unit tests.
- `./gradlew connectedDebugAndroidTest`: run Android instrumentation tests (emulator/device required).

## Coding Style & Naming Conventions
Follow `.editorconfig`: UTF-8, LF endings, 4-space indentation, max line length 100, final newline, no trailing whitespace. Kotlin formatting is enforced via Spotless + ktlint (`intellij_idea` style with project-specific rule overrides).

Match existing naming patterns: packages under `coil3.*`, types in `UpperCamelCase`, members in `lowerCamelCase`, constants in `UPPER_SNAKE_CASE`. Keep file names aligned with primary types (for example `NetworkFetcher.kt`).

## Testing Guidelines
Tests use Kotlin Test/JUnit, Robolectric, Android instrumentation, and screenshot tooling (Paparazzi/Roborazzi/Compose screenshot validation). Name test files `*Test.kt` and place them in the appropriate source set (`commonTest`, `androidHostTest`, etc.).

For bug fixes, add or update a failing test first when possible. Run `./test.sh` before opening a PR.

## Commit & Pull Request Guidelines
Use concise, imperative commit subjects (for example `Fix macOS test flakiness`). Dependency updates commonly use Conventional Commit style (for example `fix(deps): ...` or `chore(deps): ...`).

PRs should include:
- A clear summary of what changed and why.
- Linked issue(s) when applicable.
- Test evidence (commands run, and screenshots for UI/screenshot test changes).
- Updated ABI files when public API changes are intentional.
- Add the following text at the end of the last line of the PR description: `review-path-agent`.

Before submitting, ensure `./test.sh` passes locally.
