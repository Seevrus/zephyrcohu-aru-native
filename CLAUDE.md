# Project Rules & Conventions

## Kotlin & Android

- Use Kotlin for all new code.
- Follow the official [Kotlin Style Guide](https://kotlinlang.org/docs/coding-conventions.html), but with 2-space indentation.
- Use Jetpack Compose for UI.
- Use `ktlint` for formatting.
- Use `detekt` for static analysis.

## Git & Commits

- Use [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/).

## Testing Conventions

- Use `any()` in Unit Tests when:
  - **Stubbing:** The specific input values are irrelevant to the behavior being tested (e.g., `whenever(api.call(any())).thenReturn(...)`).
  - **Verification:** You only need to confirm a method was executed, but the data it received is not the focus of the current test.
- Prefer explicit values, `eq()`, or `argumentCaptor` when:
  - Testing data transformation, mapping logic, or ensuring correct parameters are passed between layers (e.g., Repository to DAO).

## AI Instructions

- Always run `./gradlew ktlintFormat` before finishing a task that involves Kotlin changes.
- Ensure `./gradlew detekt` passes before declaring a task complete.
- When adding `@Composable` functions, ensure they follow PascalCase and include a `Modifier` parameter if they emit UI.

## Emulator & ADB Safety

- **NEVER** use `adb uninstall` without explicit user confirmation, especially if multiple emulators/devices are connected.
- **NEVER** assume an emulator is dedicated to this project. Always ask the user which device is safe to use for testing.
- If an `INSTALL_FAILED_VERSION_DOWNGRADE` occurs, prefer `adb install -r -d` (downgrade flag) over uninstallation.
- Before running any `adb` command that modifies the device state, verify the package name matches the current project's `applicationId`.

### Running Instrumented Tests (`connectedAndroidTest`)

- **ALWAYS** run `adb devices` before `connectedAndroidTest`. If more than one device is listed, **STOP and inform the user** — do not proceed without confirmation.
- **NEVER** use `-Pandroid.serialno=<serial>` — it is silently ignored by the Android Gradle Plugin and tests will run on ALL connected devices.
- To restrict to one device, set the `ANDROID_SERIAL` environment variable:
  - Bash: `ANDROID_SERIAL=emulator-5556 ./gradlew :app:connectedDebugAndroidTest ...`
  - PowerShell: `$env:ANDROID_SERIAL="emulator-5556"; ./gradlew :app:connectedDebugAndroidTest ...`
- `ANDROID_SERIAL` is respected by ADB and the AGP; `-Pandroid.serialno` is not.
