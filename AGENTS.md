# Repository Guidelines

## Project Structure & Module Organization

Smart Record is a local-first Android bookkeeping app using Kotlin, Jetpack Compose, Room, and DataStore. Gradle includes one application module, `app`.

- `app/src/main/java/com/nonen/Bookkeeping/`: application sources. `core/` contains classification utilities; `data/` contains persistence and repositories; `parse/` and `export/` handle bills; `stats/` calculates statistics; `service/` handles payment capture; `ui/` contains screens, ViewModels, components, and themes.
- `app/src/main/res/`: strings, themes, icons, and service configuration.
- `app/src/test/`: JVM unit tests; `app/schemas/`: versioned Room schemas.
- `docs/plans/` and `docs/reports/`: implementation plans and reports. `gradle/libs.versions.toml` centralizes dependencies.

## Build, Test, and Development Commands

Use JDK 21 and Android SDK 37; minimum supported Android is API 26. Configure `sdk.dir` in local, ignored `local.properties`. Run commands from the repository root:

- `.\gradlew.bat :app:assembleDebug`: build `app/build/outputs/apk/debug/app-debug.apk`.
- `.\gradlew.bat :app:installDebug`: install on a connected device or emulator; launch from its application launcher.
- `.\gradlew.bat :app:testDebugUnitTest`: run JVM tests.
- `.\gradlew.bat :app:lintDebug`: run Android Lint.
- `.\gradlew.bat :app:connectedDebugAndroidTest`: run instrumentation tests when added under `app/src/androidTest/`; requires a device or emulator.

On macOS/Linux, replace `.\gradlew.bat` with `./gradlew`.

## Coding Style & Naming Conventions

Follow `.editorconfig`: UTF-8, LF, four-space indentation, and a 120-character line limit; JSON/TOML/YAML use two spaces. Kotlin permits trailing commas. Use PascalCase for classes and composables, camelCase for functions and properties, and descriptive suffixes such as `Screen`, `ViewModel`, and `Repository`. Preserve the existing `com.nonen.Bookkeeping` package. Write concise Chinese comments, retain English technical identifiers, and place user-facing strings in resources. Android Lint is configured; standalone ktlint/Detekt tasks are currently absent.

## Testing Guidelines

Use JUnit 4, mirror production packages, and name files `<Subject>Test.kt`. Backtick test names should describe behavior. Cover classification priority, parser inputs, import/export round trips, and date/statistics boundaries when affected. No numerical coverage threshold is configured. Report JVM, lint, and device results separately.

## Commit & Pull Request Guidelines

History commonly uses concise Chinese, action-oriented subjects; prefixes such as `feat:` also appear. Keep commits focused. Pull requests should explain behavior changes, reference related issues, provide validation commands/results, and include screenshots for UI changes. Include updated Room schemas and migration details for database changes.

## Security & Configuration

Preserve local-only processing and the application's absence of network permissions. Keep personal bills, credentials, SDK paths, and generated artifacts out of commits. Store task state and logs under ignored `work/tasks/` and `work/logs/`.
