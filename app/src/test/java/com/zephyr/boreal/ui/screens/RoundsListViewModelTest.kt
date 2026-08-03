package com.zephyr.boreal.ui.screens

import com.zephyr.boreal.data.repository.ApiResource
import com.zephyr.boreal.data.repository.RoundsRepository
import com.zephyr.boreal.domain.model.Round
import com.zephyr.boreal.domain.model.RoundPartnerList
import com.zephyr.boreal.domain.model.RoundStore
import com.zephyr.boreal.domain.model.RoundUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class RoundsListViewModelTest {
  private val roundsRepository: RoundsRepository = mock()
  private val testDispatcher = StandardTestDispatcher()

  private val roundsFlow = MutableStateFlow<ApiResource<List<Round>>>(ApiResource.Loading())

  private fun buildRound(
    id: Int,
    storeName: String = "Kamion 1",
    roundStarted: String = "2026-01-05",
    roundFinished: String? = "2026-01-06",
  ) = Round(
    id = id,
    user = RoundUser(id = 1, userName = "user1", name = "Kovács Béla"),
    store = RoundStore(id = 2, code = "K1", name = storeName),
    partnerList = RoundPartnerList(id = 3, name = "Északi kör"),
    yearCode = 2026,
    roundStarted = roundStarted,
    roundFinished = roundFinished,
    lastSerialNumber = 50,
  )

  @BeforeEach
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    whenever(roundsRepository.getRounds()).thenReturn(roundsFlow)
  }

  @AfterEach
  fun tearDown() {
    Dispatchers.resetMain()
  }

  private fun createViewModel() = RoundsListViewModel(roundsRepository)

  @Test
  fun `isLoading is true until the repository emits`() =
    runTest {
      val viewModel = createViewModel()
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect() }
      advanceUntilIdle()

      assertTrue(viewModel.uiState.value.isLoading)
      assertTrue(
        viewModel.uiState.value.rounds
          .isEmpty(),
      )
      uiStateJob.cancel()
    }

  @Test
  fun `rounds are mapped to row ui models once the repository succeeds`() =
    runTest {
      roundsFlow.value = ApiResource.Success(listOf(buildRound(id = 1), buildRound(id = 2, storeName = "Kamion 2")))

      val viewModel = createViewModel()
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect() }
      advanceUntilIdle()

      val state = viewModel.uiState.value
      assertFalse(state.isLoading)
      assertEquals(2, state.rounds.size)
      assertEquals(1, state.rounds[0].id)
      assertEquals("Kamion 1", state.rounds[0].storeName)
      assertEquals("2026-01-06", state.rounds[0].roundFinished)
      uiStateJob.cancel()
    }

  @Test
  fun `a round with no finish date maps to a null roundFinished`() =
    runTest {
      roundsFlow.value = ApiResource.Success(listOf(buildRound(id = 1, roundFinished = null)))

      val viewModel = createViewModel()
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect() }
      advanceUntilIdle()

      assertNull(
        viewModel.uiState.value.rounds
          .single()
          .roundFinished,
      )
      uiStateJob.cancel()
    }

  @Test
  fun `an error with no cached data leaves the list empty`() =
    runTest {
      roundsFlow.value = ApiResource.Error("boom")

      val viewModel = createViewModel()
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect() }
      advanceUntilIdle()

      assertFalse(viewModel.uiState.value.isLoading)
      assertTrue(
        viewModel.uiState.value.rounds
          .isEmpty(),
      )
      uiStateJob.cancel()
    }
}
