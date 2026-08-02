package com.zephyr.boreal.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.zephyr.boreal.domain.model.Store
import com.zephyr.boreal.domain.model.StoreType
import com.zephyr.boreal.domain.model.UserState
import com.zephyr.boreal.ui.theme.BorealTheme
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class SelectStoreScreenTest {
  @get:Rule
  val composeTestRule = createComposeRule()

  private fun mockStore(
    id: Int,
    name: String,
    type: StoreType = StoreType.SECONDARY,
    state: UserState = UserState.IDLE,
  ): Store =
    Store(
      id = id,
      code = "S$id",
      name = name,
      type = type,
      state = state,
      firstAvailableSerialNumber = 1,
      lastAvailableSerialNumber = 100,
      yearCode = 2026,
      createdAt = "",
      updatedAt = "",
    )

  @Test
  fun selectStoreScreen_dropdownListsOnlyEligibleStores() {
    val eligibleStore = mockStore(1, "Eligible Store")
    val primaryStore = mockStore(2, "Primary Store", type = StoreType.PRIMARY)
    val busyStore = mockStore(3, "Busy Store", state = UserState.ON_ROUND)

    composeTestRule.setContent {
      BorealTheme {
        SelectStoreScreenContent(
          uiState = SelectStoreUiState(stores = listOf(eligibleStore, primaryStore, busyStore)),
          onStoreSelected = {},
          onSubmit = {},
        )
      }
    }

    // Expand the dropdown to reveal its options
    composeTestRule.onNodeWithText("Kérem válasszon…").performClick()

    composeTestRule.onNodeWithText("Eligible Store").assertIsDisplayed()
    composeTestRule.onNodeWithText("Primary Store").assertDoesNotExist()
    composeTestRule.onNodeWithText("Busy Store").assertDoesNotExist()
  }

  @Test
  fun selectStoreScreen_forwardIconAbsentUntilSelection() {
    composeTestRule.setContent {
      BorealTheme {
        SelectStoreScreenContent(
          uiState = SelectStoreUiState(selectedStoreId = null),
          onStoreSelected = {},
          onSubmit = {},
        )
      }
    }

    composeTestRule.onNodeWithContentDescription("Tovább").assertDoesNotExist()
  }

  @Test
  fun selectStoreScreen_forwardIconTriggersSubmit_whenStoreSelected() {
    val onSubmit: () -> Unit = mock()

    composeTestRule.setContent {
      BorealTheme {
        SelectStoreScreenContent(
          uiState = SelectStoreUiState(selectedStoreId = 1),
          onStoreSelected = {},
          onSubmit = onSubmit,
        )
      }
    }

    composeTestRule.onNodeWithContentDescription("Tovább").performClick()
    verify(onSubmit).invoke()
  }
}
