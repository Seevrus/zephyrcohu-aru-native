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
