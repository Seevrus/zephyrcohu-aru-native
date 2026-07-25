package com.zephyr.boreal.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.zephyr.boreal.domain.model.InvoiceType
import com.zephyr.boreal.domain.model.Receipt
import com.zephyr.boreal.domain.model.ReceiptBuyer
import com.zephyr.boreal.domain.model.ReceiptVendor
import com.zephyr.boreal.ui.theme.BorealTheme
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

class SummaryScreenContentTest {
  @get:Rule
  val composeTestRule = createComposeRule()

  private val buyer =
    ReceiptBuyer(
      id = 7,
      name = "Partner Ltd.",
      country = "HU",
      postalCode = "2000",
      city = "Szentendre",
      address = "Kossuth utca 2.",
      deliveryName = "Partner Ltd.",
      deliveryCountry = "HU",
      deliveryPostalCode = "2000",
      deliveryCity = "Szentendre",
      deliveryAddress = "Kossuth utca 2.",
      iban = null,
      bankAccount = null,
      vatNumber = "87654321-1-13",
    )

  private val vendor =
    ReceiptVendor(
      name = "Boreal Kft.",
      country = "HU",
      postalCode = "1000",
      city = "Budapest",
      address = "Fő utca 1.",
      felir = "FELIR1",
      iban = "HU00",
      bankAccount = "11111111",
      vatNumber = "12345678-1-42",
    )

  private fun buildReceipt() =
    Receipt(
      id = 1,
      companyId = 1,
      companyCode = "BOR",
      partnerId = 7,
      partnerCode = "P007",
      partnerSiteCode = "S001",
      serialNumber = 42,
      yearCode = 26,
      cancelSerialNumber = null,
      cancelYearCode = null,
      vendor = vendor,
      buyer = buyer,
      invoiceDate = "2026-07-01",
      fulfillmentDate = "2026-07-09",
      invoiceType = InvoiceType.PAPER,
      paidDate = "2026-07-09",
      user = null,
      items = emptyList(),
      otherItems = emptyList(),
      quantity = 2.0,
      netAmount = 1000.0,
      vatAmount = 270.0,
      grossAmount = 1270.0,
      vatAmounts = emptyList(),
      roundAmount = 0.0,
      roundedAmount = 1270.0,
      lastDownloadedAt = null,
      createdAt = "2026-07-01T00:00:00Z",
      updatedAt = "2026-07-01T00:00:00Z",
    )

  private fun setContent(
    uiState: SummaryUiState,
    onReturnClick: () -> Unit = {},
  ) {
    composeTestRule.setContent {
      BorealTheme {
        SummaryScreenContent(
          uiState = uiState,
          onReturnClick = onReturnClick,
        )
      }
    }
  }

  @Test
  fun showsScreenTitle() {
    setContent(uiState = SummaryUiState())

    composeTestRule.onNodeWithText("Összegzés").assertIsDisplayed()
  }

  @Test
  fun showsSuccessMessage() {
    setContent(uiState = SummaryUiState(receipt = buildReceipt()))

    composeTestRule.onNodeWithText("Számla mentése sikeres!").assertIsDisplayed()
  }

  @Test
  fun offlineNoticeHiddenWhenOnline() {
    setContent(uiState = SummaryUiState(isInternetReachable = true))

    composeTestRule
      .onNodeWithText("Internetkapcsolat hiányában a számla még nem került beküldésre.")
      .assertDoesNotExist()
  }

  @Test
  fun offlineNoticeShownWhenOffline() {
    setContent(uiState = SummaryUiState(isInternetReachable = false))

    composeTestRule
      .onNodeWithText("Internetkapcsolat hiányában a számla még nem került beküldésre.")
      .assertIsDisplayed()
  }

  @Test
  fun returnButtonClickInvokesCallback() {
    val onReturnClick: () -> Unit = mock()
    setContent(uiState = SummaryUiState(), onReturnClick = onReturnClick)

    composeTestRule.onNodeWithText("Visszatérés a kezdőképernyőre").performClick()

    verify(onReturnClick).invoke()
  }

  @Test
  fun loadingStateHidesContentAndShowsLoadingIndicator() {
    setContent(uiState = SummaryUiState(isLoading = true, receipt = buildReceipt()))

    composeTestRule.onNodeWithTag("loading_indicator").assertIsDisplayed()
    composeTestRule.onNodeWithText("Számla mentése sikeres!").assertDoesNotExist()
    composeTestRule.onNodeWithText("Visszatérés a kezdőképernyőre").assertDoesNotExist()
  }

  @Test
  fun returnButtonNotInvokedWithoutClick() {
    val onReturnClick: () -> Unit = mock()
    setContent(uiState = SummaryUiState(), onReturnClick = onReturnClick)

    verify(onReturnClick, never()).invoke()
  }
}
