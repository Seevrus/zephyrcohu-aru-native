package com.zephyr.boreal.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.zephyr.boreal.ui.theme.BorealTheme
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

class RoundsListScreenContentTest {
  @get:Rule
  val composeTestRule = createComposeRule()

  private fun buildRow(
    id: Int = 1,
    storeName: String = "Kamion 1",
    roundStarted: String = "2026-01-05",
    roundFinished: String? = "2026-01-06",
  ) = RoundListRowUiModel(
    id = id,
    storeName = storeName,
    roundStarted = roundStarted,
    roundFinished = roundFinished,
  )

  private fun setContent(
    uiState: RoundsListUiState,
    onRoundClick: (Int) -> Unit = {},
  ) {
    composeTestRule.setContent {
      BorealTheme {
        RoundsListScreenContent(
          uiState = uiState,
          onRoundClick = onRoundClick,
        )
      }
    }
  }

  @Test
  fun showsScreenTitle() {
    setContent(uiState = RoundsListUiState(isLoading = false))

    composeTestRule.onNodeWithText("Körlista").assertIsDisplayed()
  }

  @Test
  fun loadingStateShowsIndicator() {
    setContent(uiState = RoundsListUiState(isLoading = true))

    composeTestRule.onNodeWithTag("loading_indicator").assertIsDisplayed()
  }

  @Test
  fun emptyListRendersWithoutCrashing() {
    setContent(uiState = RoundsListUiState(isLoading = false, rounds = emptyList()))

    composeTestRule.onNodeWithText("Körlista").assertIsDisplayed()
  }

  @Test
  fun rowShowsStoreNameAndDates() {
    setContent(uiState = RoundsListUiState(isLoading = false, rounds = listOf(buildRow())))

    composeTestRule.onNodeWithText("Kamion 1").assertIsDisplayed()
    composeTestRule.onNodeWithText("2026-01-05").assertIsDisplayed()
    composeTestRule.onNodeWithText("2026-01-06").assertIsDisplayed()
  }

  @Test
  fun rowWithoutFinishDateOmitsFinishRow() {
    setContent(
      uiState = RoundsListUiState(isLoading = false, rounds = listOf(buildRow(roundFinished = null))),
    )

    composeTestRule.onNodeWithText("Lezárva:").assertDoesNotExist()
  }

  @Test
  fun tappingRowInvokesCallbackWithRoundId() {
    val onRoundClick: (Int) -> Unit = mock()
    setContent(
      uiState = RoundsListUiState(isLoading = false, rounds = listOf(buildRow(id = 7))),
      onRoundClick = onRoundClick,
    )

    composeTestRule.onNodeWithText("Kamion 1").performClick()

    verify(onRoundClick).invoke(7)
  }

  @Test
  fun rowClickNotInvokedWithoutTap() {
    val onRoundClick: (Int) -> Unit = mock()
    setContent(uiState = RoundsListUiState(isLoading = false, rounds = listOf(buildRow())), onRoundClick = onRoundClick)

    verify(onRoundClick, never()).invoke(any())
  }
}
