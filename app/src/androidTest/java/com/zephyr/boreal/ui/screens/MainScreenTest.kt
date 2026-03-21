package com.zephyr.boreal.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.platform.app.InstrumentationRegistry
import com.zephyr.boreal.R
import com.zephyr.boreal.ui.theme.BorealTheme
import org.junit.Rule
import org.junit.Test

class MainScreenTest {
  @get:Rule
  val composeTestRule = createComposeRule()

  @Test
  fun mainScreen_displaysCorrectTitleAndTiles() {
    // InstrumentationRegistry.getInstrumentation().targetContext provides access to the
    // application's environment (resources, assets, etc.) while it's running on the device.
    // It is used here to retrieve localized string resources (R.string.*) to ensure that
    // the UI assertions match the actual values defined in strings.xml.
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val appTitle = context.getString(R.string.title_main)
    val loadingTitle = context.getString(R.string.tile_storage)
    val unloadingTitle = context.getString(R.string.tile_sell)
    val routesTitle = context.getString(R.string.tile_errands)
    val documentsTitle = context.getString(R.string.tile_receipts, 0)

    composeTestRule.setContent {
      BorealTheme {
        MainScreen()
      }
    }

    // Assert App Title
    composeTestRule.onNodeWithText(appTitle).assertIsDisplayed()

    // Assert Tiles
    composeTestRule.onNodeWithText(loadingTitle).assertIsDisplayed()
    composeTestRule.onNodeWithText(unloadingTitle).assertIsDisplayed()
    composeTestRule.onNodeWithText(routesTitle).assertIsDisplayed()
    composeTestRule.onNodeWithText(documentsTitle).assertIsDisplayed()
  }
}
