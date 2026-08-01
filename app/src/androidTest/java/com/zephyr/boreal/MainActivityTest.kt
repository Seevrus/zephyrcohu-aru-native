package com.zephyr.boreal

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

@HiltAndroidTest
class MainActivityTest {
  private val hiltRule = HiltAndroidRule(this)
  private val composeTestRule = createAndroidComposeRule<MainActivity>()

  @get:Rule
  val ruleChain: RuleChain = RuleChain.outerRule(hiltRule).around(composeTestRule)

  @Test
  fun mainActivity_loadsMainScreen() {
    val context = composeTestRule.activity
    val mainActivityTitle = context.getString(R.string.screen_main_title)

    // Assert that MainScreen is loaded by checking for the App Bar title
    composeTestRule.onNodeWithText(mainActivityTitle).assertIsDisplayed()
  }
}
