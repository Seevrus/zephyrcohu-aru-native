package com.zephyr.boreal.ui.screens

import androidx.lifecycle.SavedStateHandle
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
class RoundDetailsViewModelTest {
  private val roundsRepository: RoundsRepository = mock()
  private val testDispatcher = StandardTestDispatcher()

  private val roundsFlow = MutableStateFlow<ApiResource<List<Round>>>(ApiResource.Loading())

  private fun buildRound(
    id: Int,
    lastSerialNumber: Int? = 50,
  ) = Round(
    id = id,
    user = RoundUser(id = 1, userName = "user1", name = "Kovács Béla"),
    store = RoundStore(id = 2, code = "K1", name = "Kamion 1"),
    partnerList = RoundPartnerList(id = 3, name = "Északi kör"),
    yearCode = 2026,
    roundStarted = "2026-01-05",
    roundFinished = "2026-01-06",
    lastSerialNumber = lastSerialNumber,
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

  private fun createViewModel(roundId: Int = 1) =
    RoundDetailsViewModel(
      savedStateHandle = SavedStateHandle(mapOf("roundId" to roundId)),
      roundsRepository = roundsRepository,
      defaultDispatcher = testDispatcher,
    )

  @Test
  fun `isLoading is true until the repository emits`() =
    runTest {
      val viewModel = createViewModel()
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect() }
      advanceUntilIdle()

      assertTrue(viewModel.uiState.value.isLoading)
      assertNull(viewModel.uiState.value.round)
      uiStateJob.cancel()
    }

  @Test
  fun `the round matching the nav arg id is selected out of the fetched list`() =
    runTest {
      roundsFlow.value = ApiResource.Success(listOf(buildRound(id = 1), buildRound(id = 2)))

      val viewModel = createViewModel(roundId = 2)
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect() }
      advanceUntilIdle()

      assertFalse(viewModel.uiState.value.isLoading)
      assertEquals(
        2,
        viewModel.uiState.value.round
          ?.id,
      )
      uiStateJob.cancel()
    }

  @Test
  fun `no round matches the nav arg id leaves round null`() =
    runTest {
      roundsFlow.value = ApiResource.Success(listOf(buildRound(id = 1)))

      val viewModel = createViewModel(roundId = 999)
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect() }
      advanceUntilIdle()

      assertFalse(viewModel.uiState.value.isLoading)
      assertNull(viewModel.uiState.value.round)
      assertNull(viewModel.uiState.value.textToPrint)
      uiStateJob.cancel()
    }

  @Test
  fun `textToPrint is built when the round has a lastSerialNumber`() =
    runTest {
      roundsFlow.value = ApiResource.Success(listOf(buildRound(id = 1, lastSerialNumber = 50)))

      val viewModel = createViewModel(roundId = 1)
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect() }
      advanceUntilIdle()

      assertTrue(
        viewModel.uiState.value.textToPrint!!
          .contains("KÖRELSZÁMOLÁS"),
      )
      assertTrue(
        viewModel.uiState.value.suggestedFileName
          .isNotEmpty(),
      )
      uiStateJob.cancel()
    }

  @Test
  fun `textToPrint is null when the round has no lastSerialNumber`() =
    runTest {
      roundsFlow.value = ApiResource.Success(listOf(buildRound(id = 1, lastSerialNumber = null)))

      val viewModel = createViewModel(roundId = 1)
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect() }
      advanceUntilIdle()

      assertNull(viewModel.uiState.value.textToPrint)
      uiStateJob.cancel()
    }
}
