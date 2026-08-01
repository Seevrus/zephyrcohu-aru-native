package com.zephyr.boreal.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.zephyr.boreal.ComposeContentTestActivity
import com.zephyr.boreal.domain.model.InvoiceType
import com.zephyr.boreal.domain.model.Receipt
import com.zephyr.boreal.domain.model.ReceiptBuyer
import com.zephyr.boreal.domain.model.ReceiptVendor
import com.zephyr.boreal.ui.theme.BorealTheme
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

@HiltAndroidTest
class ReceiptDetailsScreenContentTest {
  private val hiltRule = HiltAndroidRule(this)
  private val composeTestRule = createAndroidComposeRule<ComposeContentTestActivity>()

  @get:Rule
  val ruleChain: RuleChain = RuleChain.outerRule(hiltRule).around(composeTestRule)

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

  private fun buildReceipt(
    cancelSerialNumber: Int? = null,
    cancelYearCode: Int? = null,
  ) = Receipt(
    id = 1,
    companyId = 1,
    companyCode = "BOR",
    partnerId = 7,
    partnerCode = "P007",
    partnerSiteCode = "S001",
    serialNumber = 42,
    yearCode = 2026,
    cancelSerialNumber = cancelSerialNumber,
    cancelYearCode = cancelYearCode,
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
    uiState: ReceiptDetailsUiState,
    onCancelRequested: () -> Unit = {},
  ) {
    composeTestRule.setContent {
      BorealTheme {
        ReceiptDetailsScreenContent(
          uiState = uiState,
          onCancelRequested = onCancelRequested,
        )
      }
    }
  }

  @Test
  fun showsScreenTitleFromReceipt() {
    setContent(uiState = ReceiptDetailsUiState(isLoading = false, receipt = buildReceipt()))

    composeTestRule.onNodeWithText("42/2026").assertIsDisplayed()
  }

  @Test
  fun loadingStateShowsIndicatorAndHidesContent() {
    setContent(uiState = ReceiptDetailsUiState(isLoading = true))

    composeTestRule.onNodeWithTag("loading_indicator").assertIsDisplayed()
    composeTestRule.onNodeWithText("Számla nyomtatása").assertDoesNotExist()
  }

  @Test
  fun normalReceiptShowsNormalSubtitle() {
    setContent(uiState = ReceiptDetailsUiState(isLoading = false, receipt = buildReceipt()))

    composeTestRule.onNodeWithText("Számla nyomtatása").assertIsDisplayed()
  }

  @Test
  fun canceledReceiptShowsStornoSubtitle() {
    setContent(
      uiState =
        ReceiptDetailsUiState(
          isLoading = false,
          receipt = buildReceipt(cancelSerialNumber = 101, cancelYearCode = 2026),
        ),
    )

    composeTestRule.onNodeWithText("Stornó számla nyomtatása").assertIsDisplayed()
  }

  @Test
  fun canceledPreviouslyShowsPreviouslyCanceledMessage() {
    setContent(
      uiState =
        ReceiptDetailsUiState(
          isLoading = false,
          receipt = buildReceipt(cancelSerialNumber = 101, cancelYearCode = 2026),
          hasCanceledNow = false,
        ),
    )

    composeTestRule.onNodeWithText("Ezt a számlát korábban stornózták.").assertIsDisplayed()
    composeTestRule.onNodeWithText("Sikeres stornózás!").assertDoesNotExist()
  }

  @Test
  fun canceledJustNowShowsSuccessMessage() {
    setContent(
      uiState =
        ReceiptDetailsUiState(
          isLoading = false,
          receipt = buildReceipt(cancelSerialNumber = 101, cancelYearCode = 2026),
          hasCanceledNow = true,
        ),
    )

    composeTestRule.onNodeWithText("Sikeres stornózás!").assertIsDisplayed()
    composeTestRule.onNodeWithText("Ezt a számlát korábban stornózták.").assertDoesNotExist()
  }

  @Test
  fun offlineNoticeShownWhenOffline() {
    setContent(
      uiState = ReceiptDetailsUiState(isLoading = false, receipt = buildReceipt(), isInternetReachable = false),
    )

    composeTestRule
      .onNodeWithText("Internetkapcsolat hiányában a számla nem stornózható.")
      .assertIsDisplayed()
  }

  @Test
  fun offlineNoticeHiddenWhenOnline() {
    setContent(
      uiState = ReceiptDetailsUiState(isLoading = false, receipt = buildReceipt(), isInternetReachable = true),
    )

    composeTestRule
      .onNodeWithText("Internetkapcsolat hiányában a számla nem stornózható.")
      .assertDoesNotExist()
  }

  @Test
  fun errorMessageIsDisplayedWhenPresent() {
    setContent(
      uiState =
        ReceiptDetailsUiState(
          isLoading = false,
          receipt = buildReceipt(),
          errorMessage = "Váratlan hiba lépett fel a számla sztornózása során.",
        ),
    )

    composeTestRule
      .onNodeWithText("Váratlan hiba lépett fel a számla sztornózása során.")
      .assertIsDisplayed()
  }

  @Test
  fun cancelButtonShownWhenEligible() {
    setContent(
      uiState =
        ReceiptDetailsUiState(
          isLoading = false,
          receipt = buildReceipt(),
          isInternetReachable = true,
        ),
    )

    composeTestRule.onNodeWithText("Számla stornózása").assertIsDisplayed()
  }

  @Test
  fun cancelButtonHiddenWhenOffline() {
    setContent(
      uiState =
        ReceiptDetailsUiState(
          isLoading = false,
          receipt = buildReceipt(),
          isInternetReachable = false,
        ),
    )

    composeTestRule.onNodeWithText("Számla stornózása").assertDoesNotExist()
  }

  @Test
  fun cancelButtonHiddenWhenAlreadyCanceled() {
    setContent(
      uiState =
        ReceiptDetailsUiState(
          isLoading = false,
          receipt = buildReceipt(cancelSerialNumber = 101, cancelYearCode = 2026),
          isInternetReachable = true,
        ),
    )

    composeTestRule.onNodeWithText("Számla stornózása").assertDoesNotExist()
  }

  @Test
  fun tappingCancelButtonShowsConfirmDialog() {
    setContent(uiState = ReceiptDetailsUiState(isLoading = false, receipt = buildReceipt()))

    composeTestRule.onNodeWithText("Számla stornózása").performClick()

    composeTestRule
      .onNodeWithText("Ez a művelet számlakészítéssel jár és nem visszavonható. Biztosan folytatni szeretné?")
      .assertIsDisplayed()
  }

  @Test
  fun dismissingConfirmDialogDoesNotInvokeCancel() {
    val onCancelRequested: () -> Unit = mock()
    setContent(
      uiState = ReceiptDetailsUiState(isLoading = false, receipt = buildReceipt()),
      onCancelRequested = onCancelRequested,
    )

    composeTestRule.onNodeWithText("Számla stornózása").performClick()
    composeTestRule.onNodeWithText("Mégsem").performClick()

    verify(onCancelRequested, never()).invoke()
  }

  @Test
  fun confirmingDialogInvokesCancelCallback() {
    val onCancelRequested: () -> Unit = mock()
    setContent(
      uiState = ReceiptDetailsUiState(isLoading = false, receipt = buildReceipt()),
      onCancelRequested = onCancelRequested,
    )

    composeTestRule.onNodeWithText("Számla stornózása").performClick()
    composeTestRule.onNodeWithText("Folytatás").performClick()

    verify(onCancelRequested).invoke()
  }
}
