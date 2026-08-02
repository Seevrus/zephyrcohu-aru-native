package com.zephyr.boreal.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zephyr.boreal.data.repository.ApiResource
import com.zephyr.boreal.data.repository.StoresRepository
import com.zephyr.boreal.data.repository.UserRepository
import com.zephyr.boreal.domain.model.Store
import com.zephyr.boreal.domain.model.StoreType
import com.zephyr.boreal.domain.model.User
import com.zephyr.boreal.domain.model.UserState
import com.zephyr.boreal.domain.model.canLoadAnyStore
import com.zephyr.boreal.network.ConnectivityObserver
import com.zephyr.boreal.store.store.StorageSessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SelectStoreUiState(
  val stores: List<Store> = emptyList(),
  val selectedStoreId: Int? = null,
  val currentUser: User? = null,
  val isLoading: Boolean = false,
  val isSubmitting: Boolean = false,
  val errorMessage: String? = null,
  val isInternetReachable: Boolean = true,
) {
  val displayStores: List<Store>
    get() =
      stores
        .filter { store ->
          store.type != StoreType.PRIMARY &&
            store.state == UserState.IDLE &&
            (currentUser?.canLoadAnyStore == true || store.owner == null || store.owner.id == currentUser?.id)
        }.sortedBy { it.name }

  val canSubmit: Boolean
    get() = selectedStoreId != null && !isSubmitting
}

@HiltViewModel
class SelectStoreViewModel
  @Inject
  constructor(
    private val storesRepository: StoresRepository,
    private val userRepository: UserRepository,
    private val storageSessionStore: StorageSessionStore,
    connectivityObserver: ConnectivityObserver,
  ) : ViewModel() {
    private val _uiState = MutableStateFlow(SelectStoreUiState())
    val uiState: StateFlow<SelectStoreUiState> =
      combine(
        _uiState,
        connectivityObserver.isInternetReachable,
      ) { state, isOnline ->
        state.copy(isInternetReachable = isOnline)
      }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = SelectStoreUiState(),
      )

    init {
      loadInitialData()
    }

    private fun loadInitialData() {
      viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true) }

        launch {
          userRepository.getCurrentUser().collect { result ->
            when (result) {
              is ApiResource.Success -> {
                _uiState.update { it.copy(currentUser = result.data) }
                updateDefaultStore()
              }
              is ApiResource.Error -> {
                _uiState.update { it.copy(errorMessage = result.message) }
              }
              is ApiResource.Loading -> {}
            }
          }
        }

        launch {
          storesRepository.getStores(forceRefresh = true).collect { result ->
            when (result) {
              is ApiResource.Success -> {
                _uiState.update { it.copy(stores = result.data, isLoading = false) }
                updateDefaultStore()
              }
              is ApiResource.Error -> {
                _uiState.update { it.copy(errorMessage = result.message, isLoading = false) }
              }
              is ApiResource.Loading -> {}
            }
          }
        }
      }
    }

    private fun updateDefaultStore() {
      _uiState.update { state ->
        if (state.selectedStoreId != null) return@update state
        val currentUser = state.currentUser ?: return@update state
        val defaultStoreId =
          state.displayStores.find { it.owner?.id == currentUser.id }?.id
        state.copy(selectedStoreId = defaultStoreId)
      }
    }

    fun onStoreSelected(storeId: Int) {
      _uiState.update { it.copy(selectedStoreId = storeId) }
    }

    fun submit(onSuccess: () -> Unit) {
      val state = _uiState.value
      val storeId = state.selectedStoreId ?: return
      val primaryStoreId = state.stores.find { it.type == StoreType.PRIMARY }?.id

      if (primaryStoreId == null) {
        _uiState.update { it.copy(errorMessage = "A központi raktár nem található.") }
        return
      }

      viewModelScope.launch {
        _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }

        when (val lockResult = storesRepository.selectStore(storeId)) {
          is ApiResource.Success -> {
            val primaryDetailsResult = storesRepository.getStoreDetails(primaryStoreId)
            val selectedDetailsResult = storesRepository.getStoreDetails(storeId)

            if (primaryDetailsResult is ApiResource.Success && selectedDetailsResult is ApiResource.Success) {
              storageSessionStore.setStores(primaryDetailsResult.data, selectedDetailsResult.data)
              _uiState.update { it.copy(isSubmitting = false) }
              onSuccess()
            } else {
              val errorMessage =
                (primaryDetailsResult as? ApiResource.Error)?.message
                  ?: (selectedDetailsResult as? ApiResource.Error)?.message
              _uiState.update { it.copy(isSubmitting = false, errorMessage = errorMessage) }
            }
          }
          is ApiResource.Error -> {
            _uiState.update { it.copy(isSubmitting = false, errorMessage = lockResult.message) }
          }
          is ApiResource.Loading -> {}
        }
      }
    }
  }
