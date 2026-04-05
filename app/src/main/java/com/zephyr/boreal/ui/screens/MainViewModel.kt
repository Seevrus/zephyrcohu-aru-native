package com.zephyr.boreal.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zephyr.boreal.data.local.dao.CacheMetadataDao
import com.zephyr.boreal.store.user.UserSessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AppStartState {
  object Reconciling : AppStartState()

  data class Ready(
    val isLoggedIn: Boolean,
  ) : AppStartState()
}

@HiltViewModel
class MainViewModel
  @Inject
  constructor(
    private val userSessionStore: UserSessionStore,
    private val cacheMetadataDao: CacheMetadataDao,
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

        userSessionStore.userState.collect { userState ->
          val isLoggedIn =
            userState.storedToken?.token != null &&
              userState.storedToken.isPasswordExpired != true
          _appState.value = AppStartState.Ready(isLoggedIn = isLoggedIn)
        }
      }
    }

    private suspend fun performGarbageCollection() {
      // Clear specific cache entries older than 24 hours (24 * 60 * 60 * 1000 ms = 86400000 ms)
      val threshold = System.currentTimeMillis() - GC_THRESHOLD_MS
      cacheMetadataDao.deleteOldEntries(threshold)
    }
  }
