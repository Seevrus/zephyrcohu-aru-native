package com.zephyr.boreal.ui.screens

import androidx.lifecycle.SavedStateHandle
import com.zephyr.boreal.data.repository.ApiResource
import com.zephyr.boreal.data.repository.PartnersRepository
import com.zephyr.boreal.domain.model.DraftReceipt
import com.zephyr.boreal.domain.model.InvoiceType
import com.zephyr.boreal.domain.model.LocationType
import com.zephyr.boreal.domain.model.PartnerLocation
import com.zephyr.boreal.domain.model.TaxPayer
import com.zephyr.boreal.store.receipts.ReceiptsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class AddPartnerViewModelTest {
  private val partnersRepository: PartnersRepository = mock()
  private val receiptsStore: ReceiptsStore = mock()
  private val testDispatcher = StandardTestDispatcher()

  @BeforeEach
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
  }

  @AfterEach
  fun tearDown() {
    Dispatchers.resetMain()
  }

  private fun viewModel(
    taxNumber: String? = null,
    selectedIndex: Int = -1,
  ): AddPartnerViewModel {
    val handle =
      SavedStateHandle(
        buildMap {
          if (taxNumber != null) put("taxNumber", taxNumber)
          put("selectedIndex", selectedIndex)
        },
      )
    return AddPartnerViewModel(handle, partnersRepository, receiptsStore)
  }

  private fun mockTaxPayer(
    vatNumber: String,
    deliveryName: String,
  ): TaxPayer =
    TaxPayer(
      id = 1,
      vatNumber = vatNumber,
      locations =
        mapOf(
          "C" to
            PartnerLocation(
              name = deliveryName,
              locationType = LocationType.CENTRAL,
              country = "HU",
              postalCode = "1000",
              city = "Budapest",
              address = "HQ Street 1",
              createdAt = "",
              updatedAt = "",
            ),
          "D" to
            PartnerLocation(
              name = deliveryName,
              locationType = LocationType.DELIVERY,
              country = "HU",
              postalCode = "2000",
              city = "Debrecen",
              address = "Delivery Road 5",
              createdAt = "",
              updatedAt = "",
            ),
        ),
    )

  @Test
  fun `initial state is blank when no nav args`() =
    runTest {
      val vm = viewModel()
      advanceUntilIdle()

      assertEquals("", vm.uiState.value.taxNumber)
      assertEquals("", vm.uiState.value.name)
    }

  @Test
  fun `form is pre-filled from cached tax payer when nav args provided`() =
    runTest {
      val taxpayer = mockTaxPayer("12345678-2-41", "NAV Partner")
      whenever(partnersRepository.searchTaxNumber("12345678", false))
        .thenReturn(flowOf(ApiResource.Success(listOf(taxpayer))))

      val vm = viewModel(taxNumber = "12345678", selectedIndex = 0)
      val job =
        backgroundScope.launch(kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)) {
          vm.uiState.collect { }
        }
      advanceUntilIdle()

      assertEquals("12345678-2-41", vm.uiState.value.taxNumber)
      assertEquals("NAV Partner", vm.uiState.value.name)
      assertEquals("2000", vm.uiState.value.deliveryPostalCode)
      assertEquals("Debrecen", vm.uiState.value.deliveryCity)
      assertEquals("Delivery Road 5", vm.uiState.value.deliveryAddress)
      assertEquals("1000", vm.uiState.value.centralPostalCode)
      job.cancel()
    }

  @Test
  fun `onSubmit with missing required fields sets errors and does not write receipt`() =
    runTest {
      val vm = viewModel()
      advanceUntilIdle()

      vm.onSubmit {}

      verify(receiptsStore, never()).setCurrentReceipt(org.mockito.kotlin.any())
      assertNotNull(vm.uiState.value.taxNumberError)
      assertNotNull(vm.uiState.value.nameError)
      assertNotNull(vm.uiState.value.deliveryPostalCodeError)
    }

  @Test
  fun `onSubmit with invalid tax number format sets taxNumberError`() =
    runTest {
      val vm = viewModel()
      vm.onFieldChanged(AddPartnerField.TAX_NUMBER, "12345678")
      vm.onFieldChanged(AddPartnerField.NAME, "Test Partner")
      vm.onFieldChanged(AddPartnerField.DELIVERY_POSTAL_CODE, "1000")
      vm.onFieldChanged(AddPartnerField.DELIVERY_CITY, "Budapest")
      vm.onFieldChanged(AddPartnerField.DELIVERY_ADDRESS, "Street 1")
      advanceUntilIdle()

      vm.onSubmit {}

      verify(receiptsStore, never()).setCurrentReceipt(org.mockito.kotlin.any())
      assertNotNull(vm.uiState.value.taxNumberError)
    }

  @Test
  fun `onSubmit with partial central address sets centralAddress error`() =
    runTest {
      val vm = viewModel()
      vm.onFieldChanged(AddPartnerField.TAX_NUMBER, "12345678-2-41")
      vm.onFieldChanged(AddPartnerField.NAME, "Test Partner")
      vm.onFieldChanged(AddPartnerField.DELIVERY_POSTAL_CODE, "1000")
      vm.onFieldChanged(AddPartnerField.DELIVERY_CITY, "Budapest")
      vm.onFieldChanged(AddPartnerField.DELIVERY_ADDRESS, "Street 1")
      vm.onFieldChanged(AddPartnerField.CENTRAL_POSTAL_CODE, "2000")
      advanceUntilIdle()

      vm.onSubmit {}

      verify(receiptsStore, never()).setCurrentReceipt(org.mockito.kotlin.any())
      assertNotNull(vm.uiState.value.centralAddressError)
    }

  @Test
  fun `onSubmit valid form writes ad-hoc DraftReceipt and calls onSuccess`() =
    runTest {
      val vm = viewModel()
      vm.onFieldChanged(AddPartnerField.TAX_NUMBER, "12345678-2-41")
      vm.onFieldChanged(AddPartnerField.NAME, "Test Partner")
      vm.onFieldChanged(AddPartnerField.DELIVERY_POSTAL_CODE, "1000")
      vm.onFieldChanged(AddPartnerField.DELIVERY_CITY, "Budapest")
      vm.onFieldChanged(AddPartnerField.DELIVERY_ADDRESS, "Street 1")
      advanceUntilIdle()

      var successCalled = false
      vm.onSubmit { successCalled = true }

      val captor = argumentCaptor<DraftReceipt>()
      verify(receiptsStore).setCurrentReceipt(captor.capture())

      val receipt = captor.firstValue
      assertNull(receipt.partnerId)
      assertEquals("", receipt.partnerCode)
      assertEquals("Test Partner", receipt.buyer?.name)
      assertEquals("12345678-2-41", receipt.buyer?.vatNumber)
      assertEquals(InvoiceType.PAPER, receipt.invoiceType)
      assertEquals(0, receipt.paymentDays)
      assertTrue(successCalled)
    }
}
