package com.zephyr.boreal.ui.screens

import com.zephyr.boreal.api.dto.response.CompanyDto
import com.zephyr.boreal.data.local.UserEntity
import com.zephyr.boreal.data.local.dao.UserDao
import com.zephyr.boreal.data.repository.ApiResource
import com.zephyr.boreal.data.repository.UserRepository
import com.zephyr.boreal.domain.model.UserState
import com.zephyr.boreal.network.ConnectivityObserver
import com.zephyr.boreal.store.user.LoginSettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {
  private val userRepository: UserRepository = mock()
  private val userDao: UserDao = mock()
  private val loginSettingsStore: LoginSettingsStore = mock()
  private val connectivityObserver: ConnectivityObserver = mock()
  private val testDispatcher = StandardTestDispatcher()

  private val isOnlineFlow = MutableStateFlow(true)
  private val settingsFlow =
    MutableStateFlow(
      com.zephyr.boreal.store.user
        .LoginSettingsState(),
    )
  private val userFlow = MutableStateFlow<UserEntity?>(null)

  @BeforeEach
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    whenever(connectivityObserver.isInternetReachable).thenReturn(isOnlineFlow)
    whenever(loginSettingsStore.loginSettingsState).thenReturn(settingsFlow)
    whenever(userDao.getUser()).thenReturn(userFlow)
  }

  @AfterEach
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `initially should load from settings if no user exists`() =
    runTest {
      settingsFlow.value =
        com.zephyr.boreal.store.user
          .LoginSettingsState("001", "abc")

      val viewModel = LoginViewModel(userRepository, userDao, loginSettingsStore, connectivityObserver)

      advanceUntilIdle()

      assertEquals("001", viewModel.uiState.value.companyCode)
      assertEquals("abc", viewModel.uiState.value.userName)
      assertFalse(viewModel.uiState.value.isReLogin)
    }

  @Test
  fun `initially should prefill from user if user exists`() =
    runTest {
      val user =
        UserEntity(
          id = 1,
          userName = "def@003",
          state = UserState.ON_ROUND,
          name = "Def",
          phoneNumber = null,
          isDev = false,
          roles = emptyList(),
          storeInUseId = null,
          storeOwnedId = null,
          lastActive = "",
          createdAt = "",
          updatedAt = "",
          company =
            CompanyDto(
              id = 3,
              code = "003",
              name = "Test",
              country = "HU",
              postalCode = "1000",
              city = "City",
              address = "Address",
              felir = "F",
              vatNumber = "V",
              iban = "I",
              bankAccount = "B",
            ),
        )
      userFlow.value = user

      val viewModel = LoginViewModel(userRepository, userDao, loginSettingsStore, connectivityObserver)

      advanceUntilIdle()

      assertEquals("003", viewModel.uiState.value.companyCode)
      assertEquals("def", viewModel.uiState.value.userName)
      assertTrue(viewModel.uiState.value.isReLogin)
      assertFalse(viewModel.uiState.value.isIdle)
    }

  @Test
  fun `login should handle success`() =
    runTest {
      whenever(userRepository.login(any(), any(), any())).thenReturn(ApiResource.Success(mock()))

      val viewModel = LoginViewModel(userRepository, userDao, loginSettingsStore, connectivityObserver)
      advanceUntilIdle()
      viewModel.onUserNameChange("usr")
      viewModel.onPasswordChange("pwd")

      var successCalled = false
      viewModel.login { successCalled = true }

      advanceUntilIdle()

      assertTrue(successCalled)
      assertFalse(viewModel.uiState.value.isLoading)
      assertEquals("", viewModel.uiState.value.password)
    }

  @Test
  fun `login should handle error mapping`() =
    runTest {
      val expectedError = "Hibás felhasználónév / jelszó!"
      whenever(userRepository.login(any(), any(), any())).thenReturn(ApiResource.Error(expectedError))

      val viewModel = LoginViewModel(userRepository, userDao, loginSettingsStore, connectivityObserver)
      advanceUntilIdle()
      viewModel.onUserNameChange("usr")
      viewModel.onPasswordChange("pwd")

      viewModel.login {}

      advanceUntilIdle()

      assertEquals(expectedError, viewModel.uiState.value.errorMessage)
      assertFalse(viewModel.uiState.value.isLoading)
      assertEquals("", viewModel.uiState.value.password)
    }
}
