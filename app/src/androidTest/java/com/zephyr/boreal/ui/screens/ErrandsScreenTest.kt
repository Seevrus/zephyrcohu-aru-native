package com.zephyr.boreal.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.zephyr.boreal.R
import com.zephyr.boreal.ui.components.TileVariant
import com.zephyr.boreal.ui.theme.BorealTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ErrandsScreenTest {
  @get:Rule
  val composeTestRule = createComposeRule()

  @Test
  fun errandsScreen_displaysCorrectTitleAndTiles() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val appTitle = context.getString(R.string.screen_errands_title)
    val startTitle = context.getString(R.string.tile_errands_start)
    val endTitle = context.getString(R.string.tile_errands_end)
    val listTitle = context.getString(R.string.tile_errands_list)

    composeTestRule.setContent {
      BorealTheme {
        ErrandsScreenContent(
          uiState =
            ErrandsUiState(
              isReady = true,
              tiles =
                listOf(
                  TileUiModel(
                    id = ErrandTileId.START,
                    titleResId = R.string.tile_errands_start,
                    variant = TileVariant.OK,
                    iconResId = R.drawable.play_circle,
                  ),
                  TileUiModel(
                    id = ErrandTileId.END,
                    titleResId = R.string.tile_errands_end,
                    variant = TileVariant.DISABLED,
                    iconResId = R.drawable.stop_circle,
                  ),
                  TileUiModel(
                    id = ErrandTileId.LIST,
                    titleResId = R.string.tile_errands_list,
                    variant = TileVariant.NEUTRAL,
                    iconResId = R.drawable.assignment_turned_in,
                  ),
                ),
            ),
          onTileClick = {},
          onDismissAlert = {},
        )
      }
    }

    // Assert Screen Title
    composeTestRule.onNodeWithText(appTitle).assertIsDisplayed()

    // Assert Tiles
    composeTestRule.onNodeWithText(startTitle).assertIsDisplayed()
    composeTestRule.onNodeWithText(endTitle).assertIsDisplayed()
    composeTestRule.onNodeWithText(listTitle).assertIsDisplayed()
  }

  @Test
  fun errandsScreen_tileClick_triggersCallback() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val startTitle = context.getString(R.string.tile_errands_start)
    var clickedTile: TileUiModel<ErrandTileId>? = null

    val tile =
      TileUiModel(
        id = ErrandTileId.START,
        titleResId = R.string.tile_errands_start,
        variant = TileVariant.OK,
        iconResId = R.drawable.play_circle,
      )

    composeTestRule.setContent {
      BorealTheme {
        ErrandsScreenContent(
          uiState =
            ErrandsUiState(
              isReady = true,
              tiles = listOf(tile),
            ),
          onTileClick = { clickedTile = it },
          onDismissAlert = {},
        )
      }
    }

    composeTestRule.onNodeWithText(startTitle).performClick()

    assertTrue(clickedTile?.id == ErrandTileId.START)
  }
}
