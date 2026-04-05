package com.zephyr.boreal.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
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
    val appTitle = context.getString(R.string.title_main)
    val expectedMessage = context.getString(R.string.app_locked_message, userName)

    composeTestRule.setContent {
      BorealTheme {
        AppLockedScreenContent(
          userName = userName,
          canUseApp = false,
          onNavigateToMain = {},
        )
      }
    }

    // Assert App Title
    composeTestRule.onNodeWithText(appTitle).assertIsDisplayed()

    // Assert localized error message with user name
    composeTestRule.onNodeWithText(expectedMessage).assertIsDisplayed()
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
        )
      }
    }

    // Since canUseApp is true initially, it should trigger navigate
    composeTestRule.runOnIdle {
      assert(navigateCalled)
    }
  }
}
