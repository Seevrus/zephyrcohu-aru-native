package com.zephyr.boreal.ui.screens

import com.zephyr.boreal.data.repository.ApiResource
import com.zephyr.boreal.data.repository.PartnersRepository
import com.zephyr.boreal.data.repository.UserRepository
import com.zephyr.boreal.domain.model.LocationType
import com.zephyr.boreal.domain.model.Partner
import com.zephyr.boreal.domain.model.PartnerList
import com.zephyr.boreal.domain.model.Round
import com.zephyr.boreal.domain.model.User
import com.zephyr.boreal.store.receipts.ReceiptsStore
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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class SelectPartnerViewModelTest {
  private val partnersRepository: PartnersRepository = mock()
  private val userRepository: UserRepository = mock()
  private val receiptsStore: ReceiptsStore = mock()
  private val testDispatcher = StandardTestDispatcher()

  private val partnersFlow = MutableStateFlow<ApiResource<List<Partner>>>(ApiResource.Loading())
  private val partnerListsFlow = MutableStateFlow<ApiResource<List<PartnerList>>>(ApiResource.Loading())
  private val userFlow = MutableStateFlow<ApiResource<User?>>(ApiResource.Loading())

  @BeforeEach
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    whenever(partnersRepository.getPartners()).thenReturn(partnersFlow)
    whenever(partnersRepository.getPartnerLists(org.mockito.kotlin.any())).thenReturn(partnerListsFlow)
    whenever(userRepository.getCurrentUser()).thenReturn(userFlow)
  }

  @AfterEach
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `initial state is correct`() =
    runTest {
      val viewModel = SelectPartnerViewModel(partnersRepository, userRepository, receiptsStore)

      advanceUntilIdle()

      assertEquals(PartnerTab.ROUND_STORES, viewModel.uiState.value.selectedTab)
      assertEquals("", viewModel.uiState.value.searchQuery)
      assertTrue(viewModel.uiState.value.isLoading)
      assertTrue(
        viewModel.uiState.value.expandedPartnerIds
          .isEmpty(),
      )
      assertTrue(
        viewModel.uiState.value.partners
          .isEmpty(),
      )
    }

  @Test
  fun `onTabSelected updates state correctly`() =
    runTest {
      val partner1 = mockPartner(1, "Partner 1")
      val partner2 = mockPartner(2, "Partner 2")
      val partnerList = PartnerList(id = 10, partners = listOf(1), name = "List 1", createdAt = "", updatedAt = "")
      val user = mockUser(lastRoundPartnerListId = 10)

      partnersFlow.value = ApiResource.Success(listOf(partner1, partner2))
      partnerListsFlow.value = ApiResource.Success(listOf(partnerList))
      userFlow.value = ApiResource.Success(user)

      val viewModel = SelectPartnerViewModel(partnersRepository, userRepository, receiptsStore)
      val collectJob =
        backgroundScope.launch(kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)) {
          viewModel.uiState.collect { }
        }
      advanceUntilIdle()

      println("State: ${viewModel.uiState.value}")

      // Initial tab is ROUND_STORES, only partner1 is in the list
      assertEquals(PartnerTab.ROUND_STORES, viewModel.uiState.value.selectedTab)
      assertEquals(1, viewModel.uiState.value.partners.size)
      assertEquals(
        1,
        viewModel.uiState.value.partners
          .first()
          .id,
      )

      viewModel.onTabSelected(PartnerTab.ALL_STORES)
      advanceUntilIdle()

      assertEquals(PartnerTab.ALL_STORES, viewModel.uiState.value.selectedTab)
      assertEquals(2, viewModel.uiState.value.partners.size)

      collectJob.cancel()
    }

  @Test
  fun `onSearchQueryChanged filters partners`() =
    runTest {
      val partner1 = mockPartner(1, "Alpha Store", "1000", "CityA", "AddressA")
      val partner2 = mockPartner(2, "Beta Store", "2000", "CityB", "AddressB")

      partnersFlow.value = ApiResource.Success(listOf(partner1, partner2))
      partnerListsFlow.value = ApiResource.Success(emptyList()) // Doesn't matter for ALL_STORES
      userFlow.value = ApiResource.Success(null)

      val viewModel = SelectPartnerViewModel(partnersRepository, userRepository, receiptsStore)
      val collectJob =
        backgroundScope.launch(kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)) {
          viewModel.uiState.collect { }
        }
      viewModel.onTabSelected(PartnerTab.ALL_STORES)
      advanceUntilIdle()

      assertEquals(2, viewModel.uiState.value.partners.size)

      // Search by address
      viewModel.onSearchQueryChanged("addressa")
      advanceUntilIdle()
      assertEquals(1, viewModel.uiState.value.partners.size)
      assertEquals(
        1,
        viewModel.uiState.value.partners
          .first()
          .id,
      )

      // Search by city
      viewModel.onSearchQueryChanged("CityB")
      advanceUntilIdle()
      assertEquals(1, viewModel.uiState.value.partners.size)
      assertEquals(
        2,
        viewModel.uiState.value.partners
          .first()
          .id,
      )

      collectJob.cancel()
    }

  @Test
  fun `onTogglePartnerExpanded expands and collapses`() =
    runTest {
      val partner1 = mockPartner(1, "Partner 1")
      val partner2 = mockPartner(2, "Partner 2")

      partnersFlow.value = ApiResource.Success(listOf(partner1, partner2))
      partnerListsFlow.value = ApiResource.Success(emptyList())
      userFlow.value = ApiResource.Success(null)

      val viewModel = SelectPartnerViewModel(partnersRepository, userRepository, receiptsStore)
      val collectJob =
        backgroundScope.launch(kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)) {
          viewModel.uiState.collect { }
        }
      viewModel.onTabSelected(PartnerTab.ALL_STORES)
      advanceUntilIdle()

      assertTrue(
        viewModel.uiState.value.expandedPartnerIds
          .isEmpty(),
      )

      viewModel.onTogglePartnerExpanded(1)
      advanceUntilIdle()

      assertTrue(
        viewModel.uiState.value.expandedPartnerIds
          .contains(1),
      )
      assertEquals(1, viewModel.uiState.value.selectedPartnerId)

      // Toggle again to collapse
      viewModel.onTogglePartnerExpanded(1)
      advanceUntilIdle()

      assertTrue(
        viewModel.uiState.value.expandedPartnerIds
          .isEmpty(),
      )
      assertEquals(null, viewModel.uiState.value.selectedPartnerId)

      collectJob.cancel()
    }

  @Test
  fun `selectPartner sets partnerId on receipt and calls onSuccess`() =
    runTest {
      val partner = mockPartner(1, "Test Partner")
      partnersFlow.value = ApiResource.Success(listOf(partner))
      partnerListsFlow.value = ApiResource.Success(emptyList())
      userFlow.value = ApiResource.Success(null)

      val viewModel = SelectPartnerViewModel(partnersRepository, userRepository, receiptsStore)
      val collectJob =
        backgroundScope.launch(kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)) {
          viewModel.uiState.collect { }
        }
      viewModel.onTabSelected(PartnerTab.ALL_STORES)
      viewModel.onTogglePartnerExpanded(1)
      advanceUntilIdle()

      assertEquals(1, viewModel.uiState.value.selectedPartnerId)

      var successCalled = false
      viewModel.selectPartner { successCalled = true }

      val captor = argumentCaptor<com.zephyr.boreal.domain.model.DraftReceipt>()
      verify(receiptsStore).setCurrentReceipt(captor.capture())
      assertEquals(1, captor.firstValue.partnerId)
      assertEquals("P1", captor.firstValue.partnerCode)
      assertTrue(successCalled)

      collectJob.cancel()
    }

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
          com.zephyr.boreal.domain.model.PartnerLocation(
            name = "Loc",
            locationType = com.zephyr.boreal.domain.model.LocationType.DELIVERY,
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

  private fun mockUser(lastRoundPartnerListId: Int?): User =
    User(
      id = 1,
      userName = "test",
      state = com.zephyr.boreal.domain.model.UserState.IDLE,
      name = "Test User",
      phoneNumber = null,
      isDev = false,
      roles = emptyList(),
      storeInUseId = null,
      storeOwnedId = null,
      lastActive = "",
      createdAt = "",
      updatedAt = "",
      company = mock(),
      lastRound =
        lastRoundPartnerListId?.let {
          com.zephyr.boreal.domain.model.Round(
            id = 1,
            user =
              com.zephyr.boreal.domain.model
                .RoundUser(id = 1, userName = "test", name = "Test User"),
            store =
              com.zephyr.boreal.domain.model
                .RoundStore(id = 1, code = "s1", name = "Store"),
            partnerList =
              com.zephyr.boreal.domain.model
                .RoundPartnerList(id = it, name = "List"),
            yearCode = 26,
            roundStarted = "",
            roundFinished = null,
            lastSerialNumber = null,
          )
        },
    )
}
