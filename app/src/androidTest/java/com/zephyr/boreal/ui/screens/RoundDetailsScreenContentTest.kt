package com.zephyr.boreal.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.zephyr.boreal.ComposeContentTestActivity
import com.zephyr.boreal.domain.model.Round
import com.zephyr.boreal.domain.model.RoundPartnerList
import com.zephyr.boreal.domain.model.RoundStore
import com.zephyr.boreal.domain.model.RoundUser
import com.zephyr.boreal.ui.theme.BorealTheme
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

@HiltAndroidTest
class RoundDetailsScreenContentTest {
  private val hiltRule = HiltAndroidRule(this)
  private val composeTestRule = createAndroidComposeRule<ComposeContentTestActivity>()

  @get:Rule
  val ruleChain: RuleChain = RuleChain.outerRule(hiltRule).around(composeTestRule)

  private fun buildRound(roundFinished: String? = "2026-01-06") =
    Round(
      id = 17,
      user = RoundUser(id = 1, userName = "user1", name = "Kovács Béla"),
      store = RoundStore(id = 2, code = "K1", name = "Kamion 1"),
      partnerList = RoundPartnerList(id = 3, name = "Északi kör"),
      yearCode = 2026,
      roundStarted = "2026-01-05",
      roundFinished = roundFinished,
      lastSerialNumber = 50,
    )

  private fun setContent(uiState: RoundDetailsUiState) {
    composeTestRule.setContent {
      BorealTheme {
        RoundDetailsScreenContent(uiState = uiState)
      }
    }
  }

  @Test
  fun loadingStateShowsIndicatorAndHidesContent() {
    setContent(uiState = RoundDetailsUiState(isLoading = true))

    composeTestRule.onNodeWithTag("loading_indicator").assertIsDisplayed()
    composeTestRule.onNodeWithText("Kamion 1").assertDoesNotExist()
  }

  @Test
  fun detailsRenderAllFourFields() {
    setContent(uiState = RoundDetailsUiState(isLoading = false, round = buildRound()))

    composeTestRule.onNodeWithText("Kamion 1").assertIsDisplayed()
    composeTestRule.onNodeWithText("2026-01-05").assertIsDisplayed()
    composeTestRule.onNodeWithText("2026-01-06").assertIsDisplayed()
    composeTestRule.onNodeWithText("Északi kör").assertIsDisplayed()
  }

  @Test
  fun blankFinishRendersWithoutCrashing() {
    setContent(uiState = RoundDetailsUiState(isLoading = false, round = buildRound(roundFinished = null)))

    composeTestRule.onNodeWithText("Kamion 1").assertIsDisplayed()
  }

  @Test
  fun printSectionHiddenWhenTextToPrintIsNull() {
    setContent(uiState = RoundDetailsUiState(isLoading = false, round = buildRound(), textToPrint = null))

    composeTestRule.onNodeWithText("Kapcsolódás a nyomtatóhoz").assertDoesNotExist()
  }

  @Test
  fun printSectionShownWhenTextToPrintIsPresent() {
    setContent(
      uiState =
        RoundDetailsUiState(
          isLoading = false,
          round = buildRound(),
          textToPrint = "KÖRELSZÁMOLÁS",
          suggestedFileName = "korelszamolas.txt",
        ),
    )

    composeTestRule.onNodeWithText("Kapcsolódás a nyomtatóhoz").assertIsDisplayed()
  }
}
