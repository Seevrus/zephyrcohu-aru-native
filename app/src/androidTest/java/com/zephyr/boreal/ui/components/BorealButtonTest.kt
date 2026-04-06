package com.zephyr.boreal.ui.components

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.zephyr.boreal.ui.theme.BorealTheme
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

class BorealButtonTest {
  @get:Rule
  val composeTestRule = createComposeRule()

  @Test
  fun borealButton_displaysText() {
    val buttonText = "Click Me"

    composeTestRule.setContent {
      BorealTheme {
        BorealButton(text = buttonText, onClick = {})
      }
    }

    composeTestRule.onNodeWithText(buttonText).assertIsDisplayed()
  }

  @Test
  fun borealButton_triggersOnClick() {
    val buttonText = "Click Me"
    val onClick: () -> Unit = mock()

    composeTestRule.setContent {
      BorealTheme {
        BorealButton(text = buttonText, onClick = onClick)
      }
    }

    composeTestRule.onNodeWithText(buttonText).performClick()

    verify(onClick).invoke()
  }

  @Test
  fun borealButton_isDisabled_whenVariantIsDisabled() {
    val buttonText = "Disabled"
    val onClick: () -> Unit = mock()

    composeTestRule.setContent {
      BorealTheme {
        BorealButton(text = buttonText, variant = ButtonVariant.DISABLED, onClick = onClick)
      }
    }

    composeTestRule.onNodeWithText(buttonText).assertIsNotEnabled()
    composeTestRule.onNodeWithText(buttonText).performClick()

    verifyNoInteractions(onClick)
  }

  @Test
  fun borealButton_hasClickAction_whenNotDisabled() {
    val buttonText = "Active"

    composeTestRule.setContent {
      BorealTheme {
        BorealButton(text = buttonText, variant = ButtonVariant.NEUTRAL, onClick = {})
      }
    }

    composeTestRule.onNodeWithText(buttonText).assertHasClickAction()
  }
}
