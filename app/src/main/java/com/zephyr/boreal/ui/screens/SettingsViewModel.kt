package com.zephyr.boreal.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zephyr.boreal.data.repository.ApiResource
import com.zephyr.boreal.data.repository.UserRepository
import com.zephyr.boreal.domain.model.UserState
import com.zephyr.boreal.network.ConnectivityObserver
import com.zephyr.boreal.store.user.UserSessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsState(
  val isLoggedIn: Boolean = false,
  val isIdle: Boolean = false,
  val isPasswordExpired: Boolean = false,
  val isLoading: Boolean = false,
  val isInternetReachable: Boolean = true,
)

@HiltViewModel
class SettingsViewModel
  @Inject
  constructor(
    private val userSessionStore: UserSessionStore,
    private val userRepository: UserRepository,
    private val connectivityObserver: ConnectivityObserver,
  ) : ViewModel() {
    private val isLoggingOut = MutableStateFlow(false)
    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
      viewModelScope.launch {
        combine(
          userSessionStore.userState,
          userRepository.getCurrentUser(),
          connectivityObserver.isInternetReachable,
          isLoggingOut,
        ) { userState, resource, isOnline, loggingOut ->
          val user = resource.getOrNull()
          val storedToken = userState.storedToken
          val token = storedToken?.token

          val isLoggedIn = token != null && user != null && !storedToken.isTokenExpired
          val isIdle = user?.state == UserState.IDLE
          val isPasswordExpired = storedToken?.isPasswordExpired == true
          val isLoading = resource is ApiResource.Loading || loggingOut

          SettingsState(
            isLoggedIn = isLoggedIn,
            isIdle = isIdle,
            isPasswordExpired = isPasswordExpired,
            isLoading = isLoading,
            isInternetReachable = isOnline,
          )
        }.collect { newState ->
          _state.value = newState
        }
      }
    }

    fun logout(onSuccess: () -> Unit) {
      viewModelScope.launch {
        isLoggingOut.value = true
        userRepository.logout()
        isLoggingOut.value = false
        onSuccess()
      }
    }
  }
