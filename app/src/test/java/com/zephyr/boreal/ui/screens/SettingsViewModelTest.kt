package com.zephyr.boreal.ui.screens

import com.zephyr.boreal.data.repository.ApiResource
import com.zephyr.boreal.data.repository.UserRepository
import com.zephyr.boreal.domain.model.Company
import com.zephyr.boreal.domain.model.User
import com.zephyr.boreal.domain.model.UserState
import com.zephyr.boreal.store.user.StoredToken
import com.zephyr.boreal.store.user.UserSessionStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
  private val userSessionStore: UserSessionStore = mock()
  private val userRepository: UserRepository = mock()
  private val testDispatcher = StandardTestDispatcher()

  private val userStateFlow =
    MutableStateFlow(
      com.zephyr.boreal.store.user
        .UserState(),
    )
  private val currentUserFlow = MutableStateFlow<ApiResource<User>>(ApiResource.Loading())

  @BeforeEach
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    whenever(userSessionStore.userState).thenReturn(userStateFlow)
    whenever(userRepository.getCurrentUser()).thenReturn(currentUserFlow)
  }

  @AfterEach
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `initially should have default state`() =
    runTest {
      val viewModel = SettingsViewModel(userSessionStore, userRepository)

      assertEquals(SettingsState(), viewModel.state.value)
    }

  @Test
  fun `should be logged in when token is valid and user is available`() =
    runTest {
      val viewModel = SettingsViewModel(userSessionStore, userRepository)

      val user = mockUser(UserState.IDLE)
      val token = StoredToken("valid", false, "2099-01-01T00:00:00Z")

      userStateFlow.value =
        com.zephyr.boreal.store.user
          .UserState(storedToken = token)
      currentUserFlow.value = ApiResource.Success(user)

      runCurrent()

      assertTrue(viewModel.state.value.isLoggedIn)
      assertTrue(viewModel.state.value.isIdle)
      assertFalse(viewModel.state.value.isLoading)
    }

  @Test
  fun `should not be logged in when token is expired`() =
    runTest {
      val viewModel = SettingsViewModel(userSessionStore, userRepository)

      val user = mockUser(UserState.IDLE)
      val token = StoredToken("valid", false, "2000-01-01T00:00:00Z")

      userStateFlow.value =
        com.zephyr.boreal.store.user
          .UserState(storedToken = token)
      currentUserFlow.value = ApiResource.Success(user)

      runCurrent()

      assertFalse(viewModel.state.value.isLoggedIn)
    }

  @Test
  fun `should reflect loading state from repository`() =
    runTest {
      val viewModel = SettingsViewModel(userSessionStore, userRepository)

      currentUserFlow.value = ApiResource.Loading()

      runCurrent()

      assertTrue(viewModel.state.value.isLoading)
    }

  @Test
  fun `should reflect idle state from user`() =
    runTest {
      val viewModel = SettingsViewModel(userSessionStore, userRepository)

      val user = mockUser(UserState.ON_ROUND)
      val token = StoredToken("valid", false, "2099-01-01T00:00:00Z")

      userStateFlow.value =
        com.zephyr.boreal.store.user
          .UserState(storedToken = token)
      currentUserFlow.value = ApiResource.Success(user)

      runCurrent()

      assertTrue(viewModel.state.value.isLoggedIn)
      assertFalse(viewModel.state.value.isIdle)
    }

  private fun mockUser(state: UserState) =
    User(
      id = 1,
      userName = "test",
      state = state,
      name = "Test User",
      phoneNumber = null,
      isDev = false,
      roles = emptyList(),
      storeInUseId = null,
      storeOwnedId = null,
      lastActive = "",
      createdAt = "",
      updatedAt = "",
      company =
        Company(
          id = 1,
          code = "002",
          name = "Test Company",
          country = "HU",
          postalCode = "1000",
          city = "City",
          address = "Address",
          felir = "AA0000000",
          vatNumber = "123",
          iban = "HU1234567890",
          bankAccount = "456",
        ),
    )
}
