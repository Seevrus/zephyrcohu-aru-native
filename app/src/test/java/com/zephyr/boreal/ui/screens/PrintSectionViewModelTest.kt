package com.zephyr.boreal.ui.screens

import com.zephyr.boreal.data.repository.ApiResource
import com.zephyr.boreal.data.repository.PartnersRepository
import com.zephyr.boreal.data.repository.UserRepository
import com.zephyr.boreal.domain.model.Company
import com.zephyr.boreal.domain.model.InvoiceType
import com.zephyr.boreal.domain.model.Partner
import com.zephyr.boreal.domain.model.Receipt
import com.zephyr.boreal.domain.model.ReceiptBuyer
import com.zephyr.boreal.domain.model.ReceiptVendor
import com.zephyr.boreal.domain.model.User
import com.zephyr.boreal.domain.model.UserState
import com.zephyr.boreal.store.receipts.ReceiptsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class PrintSectionViewModelTest {
  private val receiptsStore: ReceiptsStore = mock()
  private val partnersRepository: PartnersRepository = mock()
  private val userRepository: UserRepository = mock()

  private val testDispatcher = StandardTestDispatcher()

  private val company =
    Company(
      id = 1,
      name = "Boreal Kft.",
      code = "BOR",
      country = "HU",
      postalCode = "1000",
      city = "Budapest",
      address = "Fő utca 1.",
      felir = "FELIR1",
      vatNumber = "12345678-1-42",
      iban = "HU00",
      bankAccount = "11111111",
    )

  private val user =
    User(
      id = 1,
      userName = "teszt",
      state = UserState.ON_ROUND,
      name = "Teszt Elek",
      phoneNumber = null,
      isDev = false,
      roles = emptyList(),
      storeInUseId = null,
      storeOwnedId = null,
      lastActive = "2026-07-01T00:00:00Z",
      createdAt = "2026-07-01T00:00:00Z",
      updatedAt = "2026-07-01T00:00:00Z",
      company = company,
      lastRound = null,
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

  private fun buildBuyer() =
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

  private fun buildReceipt(
    serialNumber: Int = 101,
    partnerCode: String = "P007",
    isPrinted: Boolean = false,
  ) = Receipt(
    id = 1,
    companyId = 1,
    companyCode = "BOR",
    partnerId = 7,
    partnerCode = partnerCode,
    partnerSiteCode = "S001",
    serialNumber = serialNumber,
    yearCode = 26,
    cancelSerialNumber = null,
    cancelYearCode = null,
    vendor = vendor,
    buyer = buildBuyer(),
    invoiceDate = "2026-07-01",
    fulfillmentDate = "2026-07-01",
    invoiceType = InvoiceType.PAPER,
    paidDate = "2026-07-01",
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
    isPrinted = isPrinted,
  )

  private val partner =
    Partner(
      id = 7,
      code = "P007",
      siteCode = "S001",
      name = "Partner Ltd.",
      invoiceType = InvoiceType.PAPER,
      invoiceCopies = 3,
      paymentDays = 0,
      createdAt = "2026-01-01T00:00:00Z",
      updatedAt = "2026-01-01T00:00:00Z",
    )

  @BeforeEach
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
  }

  @AfterEach
  fun tearDown() {
    Dispatchers.resetMain()
  }

  private fun stubReceipt(
    serialNumber: Int,
    receipt: Receipt?,
  ) {
    whenever(receiptsStore.receiptBySerialNumber(serialNumber)).thenReturn(flowOf(receipt))
  }

  private fun createViewModel() = PrintSectionViewModel(receiptsStore, partnersRepository, userRepository)

  @Test
  fun `initialize resolves the receipt by serial number and builds printable text`() =
    runTest(testDispatcher) {
      stubReceipt(102, buildReceipt(serialNumber = 102))
      whenever(userRepository.getCurrentUser()).thenReturn(flowOf(ApiResource.Success(user)))
      whenever(partnersRepository.getPartners()).thenReturn(flowOf(ApiResource.Success(listOf(partner))))

      val viewModel = createViewModel()
      viewModel.initialize(isCancel = false, receiptSerialNumber = 102)
      advanceUntilIdle()

      assertEquals(false, viewModel.uiState.value.isLoading)
      assertEquals("papír alapú", viewModel.uiState.value.receiptFormat)
      assertEquals(3, viewModel.uiState.value.copyCount)
      assert(
        viewModel.uiState.value.receiptToPrint
          ?.contains("102/26") == true,
      )
      assertEquals("Partner Ltd._2026-07-01_102.txt", viewModel.uiState.value.suggestedFileName)
    }

  @Test
  fun `canPrint is false when no user is available`() =
    runTest(testDispatcher) {
      stubReceipt(101, buildReceipt())
      whenever(userRepository.getCurrentUser()).thenReturn(flowOf(ApiResource.Success(null)))
      whenever(partnersRepository.getPartners()).thenReturn(flowOf(ApiResource.Success(listOf(partner))))

      val viewModel = createViewModel()
      viewModel.initialize(isCancel = false, receiptSerialNumber = 101)
      advanceUntilIdle()

      assertNull(viewModel.uiState.value.receiptToPrint)
    }

  @Test
  fun `canPrint is false when the receipt is not found`() =
    runTest(testDispatcher) {
      stubReceipt(999, null)
      whenever(userRepository.getCurrentUser()).thenReturn(flowOf(ApiResource.Success(user)))
      whenever(partnersRepository.getPartners()).thenReturn(flowOf(ApiResource.Success(listOf(partner))))

      val viewModel = createViewModel()
      viewModel.initialize(isCancel = false, receiptSerialNumber = 999)
      advanceUntilIdle()

      assertNull(viewModel.uiState.value.receiptToPrint)
      assertEquals(0, viewModel.uiState.value.copyCount)
    }

  @Test
  fun `canPrint is false when the partner is missing and the receipt is not ad-hoc`() =
    runTest(testDispatcher) {
      stubReceipt(101, buildReceipt(partnerCode = "P007"))
      whenever(userRepository.getCurrentUser()).thenReturn(flowOf(ApiResource.Success(user)))
      whenever(partnersRepository.getPartners()).thenReturn(flowOf(ApiResource.Success(emptyList())))

      val viewModel = createViewModel()
      viewModel.initialize(isCancel = false, receiptSerialNumber = 101)
      advanceUntilIdle()

      assertNull(viewModel.uiState.value.receiptToPrint)
    }

  @Test
  fun `canPrint is true for an ad-hoc receipt even when no matching partner exists`() =
    runTest(testDispatcher) {
      stubReceipt(101, buildReceipt(partnerCode = ""))
      whenever(userRepository.getCurrentUser()).thenReturn(flowOf(ApiResource.Success(user)))
      whenever(partnersRepository.getPartners()).thenReturn(flowOf(ApiResource.Success(emptyList())))

      val viewModel = createViewModel()
      viewModel.initialize(isCancel = false, receiptSerialNumber = 101)
      advanceUntilIdle()

      assert(viewModel.uiState.value.receiptToPrint != null)
      assertEquals(3, viewModel.uiState.value.copyCount)
    }

  @Test
  fun `onPrintSuccess marks the receipt printed exactly once`() =
    runTest(testDispatcher) {
      stubReceipt(101, buildReceipt(isPrinted = false))
      whenever(userRepository.getCurrentUser()).thenReturn(flowOf(ApiResource.Success(user)))
      whenever(partnersRepository.getPartners()).thenReturn(flowOf(ApiResource.Success(listOf(partner))))

      val viewModel = createViewModel()
      viewModel.initialize(isCancel = false, receiptSerialNumber = 101)
      advanceUntilIdle()

      viewModel.onPrintSuccess()
      advanceUntilIdle()
      viewModel.onPrintSuccess()
      advanceUntilIdle()

      verify(receiptsStore, times(1)).markReceiptPrinted(1)
    }

  @Test
  fun `onPrintSuccess is a no-op when the receipt is already printed`() =
    runTest(testDispatcher) {
      stubReceipt(101, buildReceipt(isPrinted = true))
      whenever(userRepository.getCurrentUser()).thenReturn(flowOf(ApiResource.Success(user)))
      whenever(partnersRepository.getPartners()).thenReturn(flowOf(ApiResource.Success(listOf(partner))))

      val viewModel = createViewModel()
      viewModel.initialize(isCancel = false, receiptSerialNumber = 101)
      advanceUntilIdle()

      viewModel.onPrintSuccess()
      advanceUntilIdle()

      verify(receiptsStore, never()).markReceiptPrinted(any())
    }
}
