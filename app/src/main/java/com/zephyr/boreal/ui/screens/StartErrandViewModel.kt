package com.zephyr.boreal.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zephyr.boreal.api.dto.request.StartRoundRequestDataDto
import com.zephyr.boreal.data.repository.ApiResource
import com.zephyr.boreal.data.repository.ItemsRepository
import com.zephyr.boreal.data.repository.PartnersRepository
import com.zephyr.boreal.data.repository.RoundsRepository
import com.zephyr.boreal.data.repository.StoresRepository
import com.zephyr.boreal.data.repository.UserRepository
import com.zephyr.boreal.domain.model.PartnerList
import com.zephyr.boreal.domain.model.Store
import com.zephyr.boreal.domain.model.StoreType
import com.zephyr.boreal.domain.model.User
import com.zephyr.boreal.domain.model.UserState
import com.zephyr.boreal.network.ConnectivityObserver
import com.zephyr.boreal.store.receipts.ReceiptsStore
import com.zephyr.boreal.store.store.StoreSessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class StartErrandUiState(
  val stores: List<Store> = emptyList(),
  val partnerLists: List<PartnerList> = emptyList(),
  val selectedStoreId: Int? = null,
  val selectedPartnerListId: Int? = null,
  val selectedDate: Date = Date(),
  val isLoading: Boolean = false,
  val isStartingRound: Boolean = false,
  val errorMessage: String? = null,
  val isInternetReachable: Boolean = true,
  val currentUser: User? = null,
) {
  val isConfirmEnabled: Boolean
    get() = selectedStoreId != null && selectedPartnerListId != null && !isStartingRound

  val displayStores: List<Store>
    get() =
      stores
        .filter { store ->
          (store.owner == null || store.owner.id == currentUser?.id) &&
            store.type != StoreType.PRIMARY &&
            store.state == UserState.IDLE
        }.sortedBy { it.name }

  val displayPartnerLists: List<PartnerList>
    get() = partnerLists.sortedBy { it.name }

  val formattedDate: String
    get() = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDate)
}

@HiltViewModel
class StartErrandViewModel
  @Inject
  constructor(
    private val roundsRepository: RoundsRepository,
    private val storesRepository: StoresRepository,
    private val partnersRepository: PartnersRepository,
    private val userRepository: UserRepository,
    private val itemsRepository: ItemsRepository,
    private val storeSessionStore: StoreSessionStore,
    private val receiptsStore: ReceiptsStore,
    connectivityObserver: ConnectivityObserver,
  ) : ViewModel() {
    private val _uiState = MutableStateFlow(StartErrandUiState())
    val uiState: StateFlow<StartErrandUiState> =
      combine(
        _uiState,
        connectivityObserver.isInternetReachable,
      ) { state, isOnline ->
        state.copy(isInternetReachable = isOnline)
      }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = StartErrandUiState(),
      )

    init {
      loadInitialData()
    }

    private fun loadInitialData() {
      viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true) }

        // Fetch current user for filtering (standard repository check/caching is fine)
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

        // Fetch stores (force network refetch)
        launch {
          storesRepository.getStores(forceRefresh = true).collect { result ->
            when (result) {
              is ApiResource.Success -> {
                _uiState.update { it.copy(stores = result.data) }
                updateDefaultStore()
              }
              is ApiResource.Error -> {
                _uiState.update { it.copy(errorMessage = result.message) }
              }
              is ApiResource.Loading -> {}
            }
          }
        }

        // Fetch partner lists (force network refetch)
        launch {
          partnersRepository.getPartnerLists(forceRefresh = true).collect { result ->
            when (result) {
              is ApiResource.Success -> {
                _uiState.update { it.copy(partnerLists = result.data, isLoading = false) }
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
        val defaultStoreId = state.stores.find { it.owner?.id == currentUser.id }?.id
        state.copy(selectedStoreId = defaultStoreId)
      }
    }

    fun onStoreSelected(storeId: Int) {
      _uiState.update { it.copy(selectedStoreId = storeId) }
    }

    fun onPartnerListSelected(partnerListId: Int) {
      _uiState.update { it.copy(selectedPartnerListId = partnerListId) }
    }

    fun onDateSelected(date: Date) {
      _uiState.update { it.copy(selectedDate = date) }
    }

    fun startRound(onSuccess: () -> Unit) {
      val state = _uiState.value
      val storeId = state.selectedStoreId ?: return
      val partnerListId = state.selectedPartnerListId ?: return

      viewModelScope.launch {
        _uiState.update { it.copy(isStartingRound = true, errorMessage = null) }

        val request =
          StartRoundRequestDataDto(
            storeId = storeId,
            partnerListId = partnerListId,
            roundStarted = state.formattedDate,
          )

        val result = roundsRepository.startRound(request)

        when (result) {
          is ApiResource.Success -> {
            // Reset receipts
            receiptsStore.resetReceipts()

            // Fetch and set store details
            val storeDetailsResult = storesRepository.getStoreDetails(storeId)
            if (storeDetailsResult is ApiResource.Success) {
              storeSessionStore.setStore(storeDetailsResult.data)
            }

            // Precaching
            launch { itemsRepository.getItems().first() }
            launch { itemsRepository.getOtherItems().first() }
            launch { partnersRepository.getPartners().first() }
            launch { itemsRepository.getPriceLists() }

            _uiState.update { it.copy(isStartingRound = false) }
            onSuccess()
          }
          is ApiResource.Error -> {
            _uiState.update { it.copy(isStartingRound = false, errorMessage = result.message) }
          }
          is ApiResource.Loading -> {}
        }
      }
    }
  }
