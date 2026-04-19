package com.zephyr.boreal.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zephyr.boreal.data.local.dao.CacheMetadataDao
import com.zephyr.boreal.data.repository.ApiResource
import com.zephyr.boreal.data.repository.UserRepository
import com.zephyr.boreal.domain.model.canUseApp
import com.zephyr.boreal.network.ConnectivityObserver
import com.zephyr.boreal.store.user.UserSessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AppStartState {
  object Reconciling : AppStartState()

  data class Ready(
    val isLoggedIn: Boolean,
    val canUseApp: Boolean? = null,
    val userName: String? = null,
    val isInternetReachable: Boolean = true,
  ) : AppStartState()
}

@HiltViewModel
class MainViewModel
  @Inject
  constructor(
    private val userSessionStore: UserSessionStore,
    private val cacheMetadataDao: CacheMetadataDao,
    private val userRepository: UserRepository,
    private val connectivityObserver: ConnectivityObserver,
  ) : ViewModel() {
    companion object {
      const val FONT_WARMUP_DELAY_MS = 1000L
      const val GC_THRESHOLD_MS = 86400000L
    }

    private val _appState = MutableStateFlow<AppStartState>(AppStartState.Reconciling)
    val appState: StateFlow<AppStartState> = _appState.asStateFlow()

    init {
      viewModelScope.launch {
        // Run garbage collection on start
        performGarbageCollection()

        // Wait for font warmup (this replaces the UI delay)
        delay(FONT_WARMUP_DELAY_MS)

        launch {
          combine(
            userSessionStore.userState,
            connectivityObserver.isInternetReachable,
          ) { userState, isOnline ->
            userState to isOnline
          }.collect { (userState, isOnline) ->
            val isLoggedIn =
              userState.storedToken?.token != null &&
                userState.storedToken.isPasswordExpired != true

            // Only update isLoggedIn if not overriding user data
            _appState.value =
              when (val currentState = _appState.value) {
                is AppStartState.Ready -> currentState.copy(isLoggedIn = isLoggedIn, isInternetReachable = isOnline)
                AppStartState.Reconciling ->
                  AppStartState.Ready(
                    isLoggedIn = isLoggedIn,
                    isInternetReachable = isOnline,
                  )
              }
          }
        }

        launch {
          userRepository.getCurrentUser().collect { resource ->
            val user = (resource as? ApiResource.Success)?.data

            _appState.value =
              when (val currentState = _appState.value) {
                is AppStartState.Ready ->
                  currentState.copy(
                    canUseApp = user?.canUseApp,
                    userName = user?.userName,
                  )
                AppStartState.Reconciling ->
                  AppStartState.Ready(
                    isLoggedIn = false,
                    canUseApp = user?.canUseApp,
                    userName = user?.userName,
                    isInternetReachable = connectivityObserver.isInternetReachable.value,
                  )
              }
          }
        }
      }
    }

    private suspend fun performGarbageCollection() {
      // Clear specific cache entries older than 24 hours (24 * 60 * 60 * 1000 ms = 86400000 ms)
      val threshold = System.currentTimeMillis() - GC_THRESHOLD_MS
      cacheMetadataDao.deleteOldEntries(threshold)
    }
  }
