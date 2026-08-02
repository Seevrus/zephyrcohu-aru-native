package com.zephyr.boreal.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zephyr.boreal.data.repository.StoresRepository
import com.zephyr.boreal.data.repository.UserRepository
import com.zephyr.boreal.di.DefaultDispatcher
import com.zephyr.boreal.domain.utils.DevPrintFileNameBuilder
import com.zephyr.boreal.domain.utils.PrintStorageTextBuilder
import com.zephyr.boreal.network.ConnectivityObserver
import com.zephyr.boreal.store.print.PrintSettingsStore
import com.zephyr.boreal.store.store.StorageSessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class StorageSummaryUiState(
  val isLoading: Boolean = false,
  val isInternetReachable: Boolean = true,
  val textToPrint: String? = null,
  val suggestedFileName: String = "",
)

@HiltViewModel
class StorageSummaryViewModel
  @Inject
  constructor(
    private val storesRepository: StoresRepository,
    private val userRepository: UserRepository,
    private val storageSessionStore: StorageSessionStore,
    private val printSettingsStore: PrintSettingsStore,
    connectivityObserver: ConnectivityObserver,
    @param:DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
  ) : ViewModel() {
    private val _uiState = MutableStateFlow(StorageSummaryUiState())
    val uiState: StateFlow<StorageSummaryUiState> = _uiState.asStateFlow()

    private var hasFinished = false

    init {
      viewModelScope.launch {
        connectivityObserver.isInternetReachable.collect { isOnline ->
          _uiState.update { it.copy(isInternetReachable = isOnline) }
        }
      }
      viewModelScope.launch {
        val user = userRepository.getCurrentUser().first().getOrNull()
        val storeDetails = storageSessionStore.selectedStoreInitialState.value
        val storageItems = storageSessionStore.storageListItems.value.orEmpty()
        val printAll = printSettingsStore.printSettingsState.value.printFullStorageList

        val textToPrint =
          if (user != null && storeDetails != null) {
            withContext(defaultDispatcher) {
              PrintStorageTextBuilder.build(printAll, storageItems, storeDetails, user)
            }
          } else {
            null
          }
        val suggestedFileName = storeDetails?.let { DevPrintFileNameBuilder.buildForStorage(it) } ?: ""

        _uiState.update { it.copy(textToPrint = textToPrint, suggestedFileName = suggestedFileName) }
      }
    }

    fun onLeave(onNavigateHome: () -> Unit) {
      if (hasFinished) {
        return
      }
      hasFinished = true

      viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true) }
        storesRepository.deselectStore()
        userRepository.refreshCurrentUser()
        storageSessionStore.clearStores()
        onNavigateHome()
      }
    }
  }
