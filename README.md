# Zephyr Boreal

This is the native Android implementation of the Boreal application.

## Getting Started

To build this project, you must provide the API URLs in your `local.properties` file. This file is git-ignored and used for environment-specific configuration.

Add the following lines to your `local.properties` (create it if it doesn't exist in the project root):

```properties
BASE_URL_DEV="http://local-dev/..."
BASE_URL_PROD="https://api-url..."
```

## Code Quality

This project enforces strict coding standards using `ktlint` for formatting and `detekt` for static analysis.

### Linting Rules
- **Indentation**: 2-space indentation.
- **Style Guide**: [Official Kotlin Style Guide](https://kotlinlang.org/docs/coding-conventions.html) (modified for 2-space indentation).
- **Jetpack Compose**: Includes specialized rules for Composable naming, parameter ordering, and state management.
- **Commits**: Follows [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/).

### Manual Commands
You can run the following commands to check or fix code quality issues:

- **Check Formatting**: `./gradlew ktlintCheck`
- **Auto-Fix Formatting**: `./gradlew ktlintFormat`
- **Static Analysis**: `./gradlew detekt`
- **Run All Checks**: `./gradlew ktlintCheck detekt`

## Testing

This project includes both local unit tests and instrumented (UI) tests.

### Running All Tests
To run both unit and instrumented tests (requires a connected device/emulator):
```bash
./gradlew test connectedCheck
```

### Local Unit Tests
Located in `app/src/test`. To run all unit tests:
```bash
./gradlew test
```
To run a specific test file:
```bash
./gradlew test --tests "com.zephyr.boreal.ExampleUnitTest"
```

### Instrumented (UI) Tests
Located in `app/src/androidTest`. Requires a connected device or emulator. To run all UI tests:
```bash
./gradlew connectedCheck
```
To run a specific test file:
```bash
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.zephyr.boreal.MainActivityTest
```
