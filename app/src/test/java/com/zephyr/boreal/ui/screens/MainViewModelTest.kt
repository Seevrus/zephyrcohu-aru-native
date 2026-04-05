package com.zephyr.boreal.ui.screens

import com.zephyr.boreal.store.user.StoredToken
import com.zephyr.boreal.store.user.UserSessionStore
import com.zephyr.boreal.store.user.UserState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {
  private val userSessionStore: UserSessionStore = mock()
  private val cacheMetadataDao: com.zephyr.boreal.data.local.dao.CacheMetadataDao = mock()
  private val testDispatcher = StandardTestDispatcher()

  @BeforeEach
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
  }

  @AfterEach
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `initially should be in Reconciling state and perform GC`() =
    runTest {
      val userStateFlow = MutableStateFlow(UserState())
      whenever(userSessionStore.userState).thenReturn(userStateFlow)

      val viewModel = MainViewModel(userSessionStore, cacheMetadataDao)

      assertEquals(AppStartState.Reconciling, viewModel.appState.value)
      // Since it's in viewModelScope.launch, it might have already started or finished.
      // In StandardTestDispatcher, we need to advance or runCurrent.
      runCurrent()
      verify(cacheMetadataDao).deleteOldEntries(any())
    }

  @Test
  fun `should transition to Ready(isLoggedIn = true) after delay if token is valid`() =
    runTest {
      val userStateFlow =
        MutableStateFlow(
          UserState(
            deviceId = "test-device",
            storedToken =
              StoredToken(
                token = "valid_token",
                isPasswordExpired = false,
                expiresAt = "2099-01-01T00:00:00Z",
              ),
          ),
        )
      whenever(userSessionStore.userState).thenReturn(userStateFlow)

      val viewModel = MainViewModel(userSessionStore, cacheMetadataDao)

      assertEquals(AppStartState.Reconciling, viewModel.appState.value)

      advanceTimeBy(MainViewModel.FONT_WARMUP_DELAY_MS + 100)

      val state = viewModel.appState.value
      assertTrue(state is AppStartState.Ready)
      assertEquals(true, (state as AppStartState.Ready).isLoggedIn)
    }

  @Test
  fun `should transition to Ready(isLoggedIn = false) after delay if no token`() =
    runTest {
      val userStateFlow = MutableStateFlow(UserState())
      whenever(userSessionStore.userState).thenReturn(userStateFlow)

      val viewModel = MainViewModel(userSessionStore, cacheMetadataDao)

      advanceTimeBy(MainViewModel.FONT_WARMUP_DELAY_MS + 100)

      val state = viewModel.appState.value
      assertTrue(state is AppStartState.Ready)
      assertEquals(false, (state as AppStartState.Ready).isLoggedIn)
    }

  @Test
  fun `should transition to Ready(isLoggedIn = false) after delay if password expired`() =
    runTest {
      val userStateFlow =
        MutableStateFlow(
          UserState(
            deviceId = "test-device",
            storedToken =
              StoredToken(
                token = "valid_token",
                isPasswordExpired = true,
                expiresAt = "2099-01-01T00:00:00Z",
              ),
          ),
        )
      whenever(userSessionStore.userState).thenReturn(userStateFlow)

      val viewModel = MainViewModel(userSessionStore, cacheMetadataDao)

      advanceTimeBy(MainViewModel.FONT_WARMUP_DELAY_MS + 100)

      val state = viewModel.appState.value
      assertTrue(state is AppStartState.Ready)
      assertEquals(false, (state as AppStartState.Ready).isLoggedIn)
    }
}
