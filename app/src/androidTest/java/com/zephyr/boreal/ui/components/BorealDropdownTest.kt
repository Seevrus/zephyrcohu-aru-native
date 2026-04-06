package com.zephyr.boreal.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.zephyr.boreal.ui.theme.BorealTheme
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class BorealDropdownTest {
  @get:Rule
  val composeTestRule = createComposeRule()

  @Test
  fun borealDropdown_displaysLabelAndSelectedValue() {
    val label = "Select Printer"
    val items = listOf(DropdownItem("1", "Printer 1"), DropdownItem("2", "Printer 2"))
    val selectedKey = "1"

    composeTestRule.setContent {
      BorealTheme {
        BorealDropdown(
          label = label,
          data = items,
          selectedKey = selectedKey,
          onSelect = {},
        )
      }
    }

    composeTestRule.onNodeWithText(label).assertIsDisplayed()
    composeTestRule.onNodeWithText("Printer 1").assertIsDisplayed()
  }

  @Test
  fun borealDropdown_expandsAndShowsOptionsOnClick() {
    val items = listOf(DropdownItem("1", "Option 1"), DropdownItem("2", "Option 2"))

    composeTestRule.setContent {
      BorealTheme {
        BorealDropdown(
          label = "Test",
          data = items,
          selectedKey = null,
          onSelect = {},
        )
      }
    }

    // Click to expand
    composeTestRule.onNodeWithText("Kérem válasszon…").performClick()

    composeTestRule.onNodeWithText("Option 1").assertIsDisplayed()
    composeTestRule.onNodeWithText("Option 2").assertIsDisplayed()
  }

  @Test
  fun borealDropdown_triggersOnSelect_whenOptionIsClicked() {
    val items = listOf(DropdownItem("1", "Option 1"))
    val onSelect: (String) -> Unit = mock()

    composeTestRule.setContent {
      BorealTheme {
        BorealDropdown(
          label = "Test",
          data = items,
          selectedKey = null,
          onSelect = onSelect,
        )
      }
    }

    // Click to expand
    composeTestRule.onNodeWithText("Kérem válasszon…").performClick()

    // Click option
    composeTestRule.onNodeWithText("Option 1").performClick()

    verify(onSelect).invoke("1")
  }
}
