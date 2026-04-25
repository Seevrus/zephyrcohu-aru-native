package com.zephyr.boreal.ui.screens

import com.zephyr.boreal.data.repository.ApiResource
import com.zephyr.boreal.data.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class ChangePasswordViewModelTest {
  private val userRepository: UserRepository = mock()
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
  fun `onPasswordChange should update password and clear error`() =
    runTest {
      val viewModel = ChangePasswordViewModel(userRepository)
      viewModel.onPasswordChange("ABCDE12345")

      assertEquals("ABCDE12345", viewModel.uiState.value.password)
      assertNull(viewModel.uiState.value.errorMessage)
    }

  @Test
  fun `onPasswordChange should filter to alphanumeric and max 10 chars`() =
    runTest {
      val viewModel = ChangePasswordViewModel(userRepository)
      viewModel.onPasswordChange("abc 123!DEFGH")

      // Only "abc123DEFG" (10 chars, no spaces, no symbols)
      assertEquals("abc123DEFG", viewModel.uiState.value.password)
    }

  @Test
  fun `changePassword should show error if password is not 10 chars`() =
    runTest {
      val viewModel = ChangePasswordViewModel(userRepository)
      viewModel.onPasswordChange("SHORT")

      viewModel.changePassword()
      advanceUntilIdle()

      assertTrue(viewModel.uiState.value.errorMessage != null)
      assertFalse(viewModel.uiState.value.isLoading)
    }

  @Test
  fun `changePassword should handle success`() =
    runTest {
      whenever(userRepository.changePassword(any())).thenReturn(ApiResource.Success(Unit))
      val viewModel = ChangePasswordViewModel(userRepository)
      viewModel.onPasswordChange("1234567890")
      assertEquals("1234567890", viewModel.uiState.value.password)

      viewModel.changePassword()
      // Note: With StandardTestDispatcher, the loading state might not be visible
      // immediately if the coroutine hasn't started yet.

      advanceUntilIdle()

      assertFalse(viewModel.uiState.value.isLoading)
      assertNull(viewModel.uiState.value.errorMessage)
      assertTrue(
        viewModel.uiState.value.successMessage != null,
        "Success message should not be null, state is: ${viewModel.uiState.value}",
      )
    }

  @Test
  fun `changePassword should handle API error`() =
    runTest {
      val error = "Password already used"
      whenever(userRepository.changePassword(any())).thenReturn(ApiResource.Error(error))
      val viewModel = ChangePasswordViewModel(userRepository)
      viewModel.onPasswordChange("ValidPass1")

      viewModel.changePassword()
      advanceUntilIdle()

      assertFalse(viewModel.uiState.value.isLoading)
      assertEquals(error, viewModel.uiState.value.errorMessage)
      assertNull(viewModel.uiState.value.successMessage)
    }
}
