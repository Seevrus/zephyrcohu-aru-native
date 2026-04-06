package com.zephyr.boreal.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.platform.app.InstrumentationRegistry
import com.zephyr.boreal.R
import com.zephyr.boreal.ui.theme.BorealTheme
import org.junit.Rule
import org.junit.Test

class SettingsScreenTest {
  @get:Rule
  val composeTestRule = createComposeRule()

  @Test
  fun settingsScreen_displaysCorrectTitleAndTiles_whenNotLoggedIn() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val settingsTitle = context.getString(R.string.title_settings)
    val loginTitle = context.getString(R.string.tile_login)
    val printSettingsTitle = context.getString(R.string.tile_print_settings)

    composeTestRule.setContent {
      BorealTheme {
        SettingsScreenContent(
          isLoggedIn = false,
          isIdle = false,
          isLoading = false,
        )
      }
    }

    // Assert Title
    composeTestRule.onNodeWithText(settingsTitle).assertIsDisplayed()

    // Assert Tiles
    composeTestRule.onNodeWithText(loginTitle).assertIsDisplayed()
    composeTestRule.onNodeWithText(printSettingsTitle).assertIsDisplayed()
  }

  @Test
  fun settingsScreen_displaysCorrectTitleAndTiles_whenLoggedInAndIdle() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val settingsTitle = context.getString(R.string.title_settings)
    val changePasswordTitle = context.getString(R.string.tile_change_password)
    val printSettingsTitle = context.getString(R.string.tile_print_settings)
    val logoutTitle = context.getString(R.string.tile_logout)

    composeTestRule.setContent {
      BorealTheme {
        SettingsScreenContent(
          isLoggedIn = true,
          isIdle = true,
          isLoading = false,
        )
      }
    }

    // Assert Title
    composeTestRule.onNodeWithText(settingsTitle).assertIsDisplayed()

    // Assert Tiles
    composeTestRule.onNodeWithText(changePasswordTitle).assertIsDisplayed()
    composeTestRule.onNodeWithText(printSettingsTitle).assertIsDisplayed()
    composeTestRule.onNodeWithText(logoutTitle).assertIsDisplayed()
  }

  @Test
  fun settingsScreen_doesNotDisplayLogout_whenLoggedInAndNotIdle() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val logoutTitle = context.getString(R.string.tile_logout)

    composeTestRule.setContent {
      BorealTheme {
        SettingsScreenContent(
          isLoggedIn = true,
          isIdle = false,
          isLoading = false,
        )
      }
    }

    // Assert Logout Tile is not displayed
    composeTestRule.onNodeWithText(logoutTitle).assertDoesNotExist()
  }

  @Test
  fun settingsScreen_displaysLoadingIndicator_whenLoading() {
    composeTestRule.setContent {
      BorealTheme {
        SettingsScreenContent(
          isLoggedIn = false,
          isIdle = false,
          isLoading = true,
        )
      }
    }

    // Assert Loading Indicator is displayed
    composeTestRule.onNodeWithTag("loading_indicator").assertIsDisplayed()
  }

  @Test
  fun settingsScreen_callsCorrectCallback_whenTileClicked() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val loginTitle = context.getString(R.string.tile_login)
    var loginCalled = false

    composeTestRule.setContent {
      BorealTheme {
        SettingsScreenContent(
          isLoggedIn = false,
          isIdle = false,
          isLoading = false,
          onNavigateToLogin = { loginCalled = true },
        )
      }
    }

    // Perform Click
    androidx.compose.ui.test
      .performClick(composeTestRule.onNodeWithText(loginTitle))

    // Assert Callback
    assert(loginCalled)
  }
}
