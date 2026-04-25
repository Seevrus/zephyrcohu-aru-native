package com.zephyr.boreal.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zephyr.boreal.data.repository.ApiResource
import com.zephyr.boreal.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChangePasswordUiState(
  val password: String = "",
  val isLoading: Boolean = false,
  val successMessage: String? = null,
  val errorMessage: String? = null,
)

@HiltViewModel
class ChangePasswordViewModel
  @Inject
  constructor(
    private val userRepository: UserRepository,
  ) : ViewModel() {
    private val _uiState = MutableStateFlow(ChangePasswordUiState())
    val uiState: StateFlow<ChangePasswordUiState> = _uiState.asStateFlow()

    fun onPasswordChange(password: String) {
      val filtered = password.filter { it.isLetterOrDigit() }.take(10)
      _uiState.value = _uiState.value.copy(password = filtered, errorMessage = null)
    }

    fun changePassword() {
      val password = _uiState.value.password.trim()
      if (password.isEmpty()) return

      // Local validation
      val passwordRegex = Regex("^[a-zA-Z0-9]{10}$")
      if (!passwordRegex.matches(password)) {
        _uiState.value = _uiState.value.copy(errorMessage = "A választott jelszó nem felel meg a szabályoknak.")
        return
      }

      viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, successMessage = null)
        when (val result = userRepository.changePassword(password)) {
          is ApiResource.Success -> {
            _uiState.value =
              _uiState.value.copy(
                isLoading = false,
                successMessage = "Jelszó megváltoztatása sikeres.",
                password = "",
              )
          }
          is ApiResource.Error -> {
            _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = result.message)
          }
          is ApiResource.Loading -> {
            // Handled by isLoading = true
          }
        }
      }
    }
  }
