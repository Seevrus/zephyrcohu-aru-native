package com.zephyr.boreal.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.zephyr.boreal.R
import com.zephyr.boreal.ui.components.TileVariant
import com.zephyr.boreal.ui.theme.BorealTheme
import org.junit.Assert.assertTrue
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
    val appTitle = context.getString(R.string.screen_main_title)
    val loadingTitle = context.getString(R.string.tile_loading)
    val unloadingTitle = context.getString(R.string.tile_unloading)
    val routesTitle = context.getString(R.string.tile_rounds)
    val documentsTitle = context.getString(R.string.tile_receipts, 0)
    val settingsDescription = context.getString(R.string.settings_icon_description)

    composeTestRule.setContent {
      BorealTheme {
        MainScreenContent(
          uiState =
            MainScreenUiState(
              isReady = true,
              isLoggedIn = true,
              canUseApp = true,
              isInternetReachable = true,
              isPasswordExpired = false,
              tiles =
                listOf(
                  TileUiModel(
                    id = MainTileId.STORAGE,
                    titleResId = R.string.tile_loading,
                    variant = TileVariant.OK,
                    iconResId = R.drawable.truck_solid_full,
                  ),
                  TileUiModel(
                    id = MainTileId.SELL,
                    titleResId = R.string.tile_unloading,
                    variant = TileVariant.OK,
                    iconResId = R.drawable.cart_arrow_down_solid_full,
                  ),
                  TileUiModel(
                    id = MainTileId.ERRANDS,
                    titleResId = R.string.tile_rounds,
                    variant = TileVariant.OK,
                    iconResId = R.drawable.rectangle_list_solid_full,
                  ),
                  TileUiModel(
                    id = MainTileId.RECEIPTS,
                    titleResId = R.string.tile_receipts,
                    titleArg = 0,
                    variant = TileVariant.OK,
                    iconResId = R.drawable.receipt_solid_full,
                  ),
                ),
            ),
          onNavigateToAppLocked = {},
          onNavigateToSettings = {},
          onNavigateToLogin = {},
          onNavigateToChangePassword = {},
          onTileClick = {},
          onDismissAlert = {},
        )
      }
    }

    // Assert App Title
    composeTestRule.onNodeWithText(appTitle).assertIsDisplayed()

    // Assert Tiles
    composeTestRule.onNodeWithText(loadingTitle).assertIsDisplayed()
    composeTestRule.onNodeWithText(unloadingTitle).assertIsDisplayed()
    composeTestRule.onNodeWithText(routesTitle).assertIsDisplayed()
    composeTestRule.onNodeWithText(documentsTitle).assertIsDisplayed()

    // Assert Settings Icon
    composeTestRule.onNodeWithContentDescription(settingsDescription).assertIsDisplayed()
  }

  @Test
  fun mainScreen_displaysAlert_whenAlertStateIsPresent() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val alertTitle = "Alert Title"
    val alertMessage = "Alert Message"

    composeTestRule.setContent {
      BorealTheme {
        MainScreenContent(
          uiState =
            MainScreenUiState(
              isReady = true,
              alertState =
                AlertUiState(
                  titleResId = R.string.alert_title_feature_not_available,
                  messageResId = R.string.disabled_tile_offline,
                ),
            ),
          onNavigateToAppLocked = {},
          onNavigateToSettings = {},
          onNavigateToLogin = {},
          onNavigateToChangePassword = {},
          onTileClick = {},
          onDismissAlert = {},
        )
      }
    }

    val expectedTitle = context.getString(R.string.alert_title_feature_not_available)
    val expectedMessage = context.getString(R.string.disabled_tile_offline)

    composeTestRule.onNodeWithText(expectedTitle).assertIsDisplayed()
    composeTestRule.onNodeWithText(expectedMessage).assertIsDisplayed()
  }

  @Test
  fun mainScreen_tileClick_triggersCallback() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val loadingTitle = context.getString(R.string.tile_loading)
    var clickedTile: TileUiModel<MainTileId>? = null

    val tile =
      TileUiModel(
        id = MainTileId.STORAGE,
        titleResId = R.string.tile_loading,
        variant = TileVariant.OK,
        iconResId = R.drawable.truck_solid_full,
      )

    composeTestRule.setContent {
      BorealTheme {
        MainScreenContent(
          uiState =
            MainScreenUiState(
              isReady = true,
              tiles = listOf(tile),
            ),
          onNavigateToAppLocked = {},
          onNavigateToSettings = {},
          onNavigateToLogin = {},
          onNavigateToChangePassword = {},
          onTileClick = { clickedTile = it },
          onDismissAlert = {},
        )
      }
    }

    composeTestRule.onNodeWithText(loadingTitle).performClick()

    assertTrue(clickedTile?.id == MainTileId.STORAGE)
  }
}
