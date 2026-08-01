package com.zephyr.boreal

import androidx.activity.ComponentActivity
import dagger.hilt.android.AndroidEntryPoint

/**
 * Bare Hilt-enabled host for `createAndroidComposeRule` in Compose UI tests. Unlike
 * [MainActivity], it doesn't call `setContent` in `onCreate`, so tests are free to call
 * `composeTestRule.setContent { ... }` themselves — required for any composable under test
 * that resolves a Hilt ViewModel (e.g. via `hiltViewModel()`), which needs a
 * `GeneratedComponentManager`-backed host activity to work at all.
 *
 * Lives in `debug/`, not `androidTest/`: a self-instrumenting test's `ActivityScenario` resolves
 * activities against the app-under-test's own package, so an activity declared only in the
 * androidTest manifest (a separate package, `<applicationId>.test`) fails to resolve at runtime.
 */
@AndroidEntryPoint
class ComposeContentTestActivity : ComponentActivity()
