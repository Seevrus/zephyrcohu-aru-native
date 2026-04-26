package com.zephyr.boreal.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.platform.app.InstrumentationRegistry
import com.zephyr.boreal.R
import com.zephyr.boreal.ui.theme.BorealTheme
import org.junit.Rule
import org.junit.Test

class AppLockedScreenTest {
  @get:Rule
  val composeTestRule = createComposeRule()

  @Test
  fun appLockedScreen_displaysCorrectMessageAndTitle() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val userName = "TestUser"
    val appTitle = context.getString(R.string.screen_main_title)
    val expectedMessage = context.getString(R.string.app_locked_message, userName)
    val settingsDescription = context.getString(R.string.settings_icon_description)

    composeTestRule.setContent {
      BorealTheme {
        AppLockedScreenContent(
          userName = userName,
          canUseApp = false,
          onNavigateToMain = {},
          onNavigateToSettings = {},
        )
      }
    }

    // Assert App Title
    composeTestRule.onNodeWithText(appTitle).assertIsDisplayed()

    // Assert localized error message with user name
    composeTestRule.onNodeWithText(expectedMessage).assertIsDisplayed()

    // Assert Settings Icon
    composeTestRule.onNodeWithContentDescription(settingsDescription).assertIsDisplayed()
  }

  @Test
  fun appLockedScreen_navigatesToMainWhenCanUseAppIsTrue() {
    var navigateCalled = false

    composeTestRule.setContent {
      BorealTheme {
        AppLockedScreenContent(
          userName = "TestUser",
          canUseApp = true,
          onNavigateToMain = { navigateCalled = true },
          onNavigateToSettings = {},
        )
      }
    }

    // Since canUseApp is true initially, it should trigger navigate
    composeTestRule.runOnIdle {
      assert(navigateCalled)
    }
  }
}
