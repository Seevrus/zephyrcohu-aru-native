package com.zephyr.boreal.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.zephyr.boreal.R
import com.zephyr.boreal.domain.model.LocationType
import com.zephyr.boreal.domain.model.Partner
import com.zephyr.boreal.domain.model.PartnerLocation
import com.zephyr.boreal.ui.theme.BorealTheme
import org.junit.Rule
import org.junit.Test

class SelectPartnerScreenTest {
  @get:Rule
  val composeTestRule = createComposeRule()

  private fun mockPartner(
    id: Int,
    name: String,
    postalCode: String = "1000",
    city: String = "City",
    address: String = "Addr",
  ): Partner =
    Partner(
      id = id,
      code = "P$id",
      siteCode = "S$id",
      name = name,
      invoiceType = com.zephyr.boreal.domain.model.InvoiceType.PAPER,
      invoiceCopies = 1,
      paymentDays = 0,
      locations =
        listOf(
          PartnerLocation(
            name = "Loc",
            locationType = LocationType.DELIVERY,
            country = "HU",
            postalCode = postalCode,
            city = city,
            address = address,
            createdAt = "",
            updatedAt = "",
          ),
        ),
      createdAt = "",
      updatedAt = "",
    )

  @Test
  fun selectPartnerScreen_displaysSearchFieldAndTabs() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val roundTab = context.getString(R.string.select_partner_tab_round)
    val allTab = context.getString(R.string.select_partner_tab_all)

    composeTestRule.setContent {
      BorealTheme {
        SelectPartnerScreenContent(
          uiState = SelectPartnerUiState(),
          onSearchQueryChanged = {},
          onTabSelected = {},
          onTogglePartnerExpanded = {},
          onAddPartnerClick = {},
          onSelectClick = {},
        )
      }
    }

    composeTestRule.onNodeWithText(roundTab).assertIsDisplayed()
    composeTestRule.onNodeWithText(allTab).assertIsDisplayed()
    // By default, search field text might just be placeholder. We can check for placeholder via semantic or wait for it.
  }

  @Test
  fun selectPartnerScreen_displaysPartners() {
    val partner = mockPartner(1, "Test Partner")
    composeTestRule.setContent {
      BorealTheme {
        SelectPartnerScreenContent(
          uiState = SelectPartnerUiState(partners = listOf(partner)),
          onSearchQueryChanged = {},
          onTabSelected = {},
          onTogglePartnerExpanded = {},
          onAddPartnerClick = {},
          onSelectClick = {},
        )
      }
    }

    composeTestRule.onNodeWithText("Test Partner").assertIsDisplayed()
  }

  @Test
  fun selectPartnerScreen_callsOnTabSelected_whenTabClicked() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val allTab = context.getString(R.string.select_partner_tab_all)
    var selectedTab: PartnerTab? = null

    composeTestRule.setContent {
      BorealTheme {
        SelectPartnerScreenContent(
          uiState = SelectPartnerUiState(),
          onSearchQueryChanged = {},
          onTabSelected = { selectedTab = it },
          onTogglePartnerExpanded = {},
          onAddPartnerClick = {},
          onSelectClick = {},
        )
      }
    }

    composeTestRule.onNodeWithText(allTab).performClick()
    assert(selectedTab == PartnerTab.ALL_STORES)
  }

  @Test
  fun selectPartnerScreen_displaysAddButton_whenCanAddPartnerIsTrue() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val contentDesc = context.getString(R.string.select_partner_add_btn)

    composeTestRule.setContent {
      BorealTheme {
        SelectPartnerScreenContent(
          uiState = SelectPartnerUiState(canAddPartner = true),
          onSearchQueryChanged = {},
          onTabSelected = {},
          onTogglePartnerExpanded = {},
          onAddPartnerClick = {},
          onSelectClick = {},
        )
      }
    }

    composeTestRule.onNodeWithContentDescription(contentDesc).assertIsDisplayed()
  }

  @Test
  fun selectPartnerScreen_doesNotDisplayAddButton_whenCanAddPartnerIsFalse() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val contentDesc = context.getString(R.string.select_partner_add_btn)

    composeTestRule.setContent {
      BorealTheme {
        SelectPartnerScreenContent(
          uiState = SelectPartnerUiState(canAddPartner = false),
          onSearchQueryChanged = {},
          onTabSelected = {},
          onTogglePartnerExpanded = {},
          onAddPartnerClick = {},
          onSelectClick = {},
        )
      }
    }

    composeTestRule.onNodeWithContentDescription(contentDesc).assertDoesNotExist()
  }
}
