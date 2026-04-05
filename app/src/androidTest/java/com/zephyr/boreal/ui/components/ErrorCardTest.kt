package com.zephyr.boreal.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.zephyr.boreal.ui.theme.BorealTheme
import org.junit.Rule
import org.junit.Test

class ErrorCardTest {
  @get:Rule
  val composeTestRule = createComposeRule()

  @Test
  fun errorCard_displaysMessageAndIcon() {
    val errorMessage = "Test Error Message"

    composeTestRule.setContent {
      BorealTheme {
        ErrorCard(message = errorMessage)
      }
    }

    // Assert message is displayed
    composeTestRule.onNodeWithText(errorMessage).assertIsDisplayed()

    // Assert icon is present
    composeTestRule.onNodeWithContentDescription("error_icon").assertIsDisplayed()
  }
}
