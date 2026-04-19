package com.zephyr.boreal.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zephyr.boreal.data.local.dao.UserDao
import com.zephyr.boreal.data.repository.ApiResource
import com.zephyr.boreal.data.repository.UserRepository
import com.zephyr.boreal.domain.model.UserState
import com.zephyr.boreal.network.ConnectivityObserver
import com.zephyr.boreal.store.user.LoginSettingsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
  val companyCode: String = "002", // Default per requirement
  val userName: String = "",
  val password: String = "",
  val isLoading: Boolean = false,
  val errorMessage: String? = null,
  val isReLogin: Boolean = false,
  val isIdle: Boolean = true,
  val isInternetReachable: Boolean = true,
)

@HiltViewModel
class LoginViewModel
  @Inject
  constructor(
    private val userRepository: UserRepository,
    private val userDao: UserDao,
    private val loginSettingsStore: LoginSettingsStore,
    connectivityObserver: ConnectivityObserver,
  ) : ViewModel() {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> =
      combine(
        _uiState,
        connectivityObserver.isInternetReachable,
      ) { state, isOnline ->
        state.copy(isInternetReachable = isOnline)
      }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = LoginUiState(),
      )

    init {
      viewModelScope.launch {
        val userEntity = userDao.getUser().first()
        val settings = loginSettingsStore.loginSettingsState.first()

        if (userEntity != null) {
          // Splitting username from company code if necessary.
          // Based on repo implementation: userName = "$username@$company"
          val parts = userEntity.userName.split("@")
          val userName = parts.firstOrNull() ?: ""
          _uiState.update {
            it.copy(
              companyCode = settings.lastCompanyCode ?: userEntity.company.code,
              userName = userName,
              isReLogin = true,
              isIdle = userEntity.state == UserState.IDLE,
            )
          }
        } else {
          _uiState.update {
            it.copy(
              companyCode = settings.lastCompanyCode ?: "002",
              userName = settings.lastUsername ?: "",
              isReLogin = false,
              isIdle = true,
            )
          }
        }
      }
    }

    fun onCompanyCodeChange(code: String) {
      if (_uiState.value.isIdle) {
        _uiState.update { it.copy(companyCode = code, errorMessage = null) }
      }
    }

    fun onUserNameChange(name: String) {
      if (_uiState.value.isIdle) {
        _uiState.update { it.updateUserName(name) }
      }
    }

    private fun LoginUiState.updateUserName(name: String): LoginUiState {
      // Per instructions: max length 3
      val filteredName = name.take(3)
      return copy(userName = filteredName, errorMessage = null)
    }

    fun onPasswordChange(pwd: String) {
      // Per instructions: max length 10
      val filteredPwd = pwd.take(10)
      _uiState.update { it.copy(password = filteredPwd, errorMessage = null) }
    }

    fun login(onSuccess: () -> Unit) {
      val currentState = _uiState.value
      val trimmedUsername = currentState.userName.trim()
      val trimmedPassword = currentState.password.trim()

      if (trimmedUsername.isEmpty() || trimmedPassword.isEmpty()) return

      viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        val result =
          userRepository.login(
            company = currentState.companyCode,
            username = trimmedUsername,
            password = trimmedPassword,
          )

        when (result) {
          is ApiResource.Success -> {
            loginSettingsStore.updateSettings(
              companyCode = currentState.companyCode,
              username = trimmedUsername,
            )
            _uiState.update { it.copy(isLoading = false, password = "") }
            onSuccess()
          }
          is ApiResource.Error -> {
            _uiState.update { it.copy(isLoading = false, errorMessage = result.message, password = "") }
          }
          is ApiResource.Loading -> {
            // Handled by manual state update
          }
        }
      }
    }
  }
