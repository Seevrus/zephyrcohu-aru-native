package com.zephyr.boreal

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class MainActivityTest {
  @get:Rule
  val composeTestRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun mainActivity_loadsMainScreen() {
    val context = composeTestRule.activity
    val mainActivityTitle = context.getString(R.string.screen_main_title)

    // Assert that MainScreen is loaded by checking for the App Bar title
    composeTestRule.onNodeWithText(mainActivityTitle).assertIsDisplayed()
  }
}
