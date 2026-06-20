package com.zephyr.boreal.ui.screens

import com.zephyr.boreal.data.repository.ApiResource
import com.zephyr.boreal.data.repository.PartnersRepository
import com.zephyr.boreal.domain.model.LocationType
import com.zephyr.boreal.domain.model.PartnerLocation
import com.zephyr.boreal.domain.model.TaxPayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class SearchPartnerNavViewModelTest {
  private val partnersRepository: PartnersRepository = mock()
  private val testDispatcher = StandardTestDispatcher()
  private val taxPayerFlow = MutableStateFlow<ApiResource<List<TaxPayer>>>(ApiResource.Loading())

  @BeforeEach
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    whenever(partnersRepository.searchTaxNumber(any(), any())).thenReturn(taxPayerFlow)
  }

  @AfterEach
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `initial state has empty taxNumber and no results`() =
    runTest {
      val viewModel = SearchPartnerNavViewModel(partnersRepository)
      advanceUntilIdle()

      assertEquals("", viewModel.uiState.value.taxNumber)
      assertTrue(
        viewModel.uiState.value.results
          .isEmpty(),
      )
      assertNull(viewModel.uiState.value.error)
      assertFalse(viewModel.uiState.value.isSearching)
    }

  @Test
  fun `search does not fire for fewer than 8 digits`() =
    runTest {
      val viewModel = SearchPartnerNavViewModel(partnersRepository)
      backgroundScope.launch(kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)) {
        viewModel.uiState.collect { }
      }

      viewModel.onTaxNumberChanged("1234567")
      advanceUntilIdle()

      verify(partnersRepository, never()).searchTaxNumber(any(), any())
      assertEquals("1234567", viewModel.uiState.value.taxNumber)
    }

  @Test
  fun `search fires when exactly 8 digits entered`() =
    runTest {
      val viewModel = SearchPartnerNavViewModel(partnersRepository)
      backgroundScope.launch(kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)) {
        viewModel.uiState.collect { }
      }

      viewModel.onTaxNumberChanged("12345678")
      taxPayerFlow.value = ApiResource.Success(emptyList())
      advanceUntilIdle()

      verify(partnersRepository).searchTaxNumber("12345678", false)
    }

  @Test
  fun `non-digit characters are stripped from tax number input`() =
    runTest {
      val viewModel = SearchPartnerNavViewModel(partnersRepository)
      backgroundScope.launch(kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)) {
        viewModel.uiState.collect { }
      }

      viewModel.onTaxNumberChanged("1234-5678")
      advanceUntilIdle()

      assertEquals("12345678", viewModel.uiState.value.taxNumber)
    }

  @Test
  fun `successful search populates results`() =
    runTest {
      val taxpayer = mockTaxPayer(1, "12345678")
      val viewModel = SearchPartnerNavViewModel(partnersRepository)
      val collectJob =
        backgroundScope.launch(kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)) {
          viewModel.uiState.collect { }
        }

      viewModel.onTaxNumberChanged("12345678")
      taxPayerFlow.value = ApiResource.Success(listOf(taxpayer))
      advanceUntilIdle()

      assertEquals(1, viewModel.uiState.value.results.size)
      assertNull(viewModel.uiState.value.error)
      collectJob.cancel()
    }

  @Test
  fun `error from repository surfaces in error state`() =
    runTest {
      val viewModel = SearchPartnerNavViewModel(partnersRepository)
      val collectJob =
        backgroundScope.launch(kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)) {
          viewModel.uiState.collect { }
        }

      viewModel.onTaxNumberChanged("12345678")
      taxPayerFlow.value = ApiResource.Error("Network error")
      advanceUntilIdle()

      assertNotNull(viewModel.uiState.value.error)
      collectJob.cancel()
    }

  @Test
  fun `onTaxPayerSelected calls navigate callback with correct args`() =
    runTest {
      val taxpayer = mockTaxPayer(1, "12345678")
      val viewModel = SearchPartnerNavViewModel(partnersRepository)
      val collectJob =
        backgroundScope.launch(kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)) {
          viewModel.uiState.collect { }
        }

      viewModel.onTaxNumberChanged("12345678")
      taxPayerFlow.value = ApiResource.Success(listOf(taxpayer))
      advanceUntilIdle()

      var navigatedTaxNumber: String? = null
      var navigatedIndex: Int? = null
      viewModel.onTaxPayerSelected(0) { tn, idx ->
        navigatedTaxNumber = tn
        navigatedIndex = idx
      }

      assertEquals("12345678", navigatedTaxNumber)
      assertEquals(0, navigatedIndex)
      collectJob.cancel()
    }

  private fun mockTaxPayer(
    id: Int,
    vatNumber: String,
  ): TaxPayer =
    TaxPayer(
      id = id,
      vatNumber = vatNumber,
      locations =
        mapOf(
          "D" to
            PartnerLocation(
              name = "Test Partner",
              locationType = LocationType.DELIVERY,
              country = "HU",
              postalCode = "1000",
              city = "Budapest",
              address = "Test Street 1",
              createdAt = "",
              updatedAt = "",
            ),
        ),
    )
}
