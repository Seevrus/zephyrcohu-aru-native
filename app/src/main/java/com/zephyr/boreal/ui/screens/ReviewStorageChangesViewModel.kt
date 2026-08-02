package com.zephyr.boreal.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zephyr.boreal.api.dto.request.ExpirationChangeDto
import com.zephyr.boreal.api.dto.request.SaveSelectedItemsRequestDataDto
import com.zephyr.boreal.api.dto.request.SaveSelectedItemsRequestDto
import com.zephyr.boreal.data.repository.ApiResource
import com.zephyr.boreal.data.repository.ItemsRepository
import com.zephyr.boreal.domain.model.StorageListItem
import com.zephyr.boreal.domain.model.StoreDetails
import com.zephyr.boreal.network.ConnectivityObserver
import com.zephyr.boreal.store.store.StorageSessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val STOP_TIMEOUT_MS = 5000L

data class ReviewStorageChangesUiState(
  val changedItems: List<StorageListItem> = emptyList(),
  val up: Int = 0,
  val down: Int = 0,
  val isInternetReachable: Boolean = true,
  val isSaving: Boolean = false,
  val isError: Boolean = false,
  val showConfirmAlert: Boolean = false,
  val reallyUnexpectedBlocker: Boolean = false,
) {
  val canConfirm: Boolean
    get() = isInternetReachable && !reallyUnexpectedBlocker
}

@HiltViewModel
class ReviewStorageChangesViewModel
  @Inject
  constructor(
    private val itemsRepository: ItemsRepository,
    private val storageSessionStore: StorageSessionStore,
    connectivityObserver: ConnectivityObserver,
  ) : ViewModel() {
    private val isSavingFlow = MutableStateFlow(false)
    private val isErrorFlow = MutableStateFlow(false)
    private val showConfirmAlertFlow = MutableStateFlow(false)

    private data class SessionInputs(
      val items: List<StorageListItem>?,
      val primaryStoreDetails: StoreDetails?,
      val isInternetReachable: Boolean,
    )

    private data class LocalInputs(
      val isSaving: Boolean,
      val isError: Boolean,
      val showConfirmAlert: Boolean,
    )

    private val sessionInputsFlow =
      combine(
        storageSessionStore.storageListItems,
        storageSessionStore.primaryStoreDetails,
        connectivityObserver.isInternetReachable,
      ) { items, primaryStoreDetails, isInternetReachable ->
        SessionInputs(items, primaryStoreDetails, isInternetReachable)
      }

    private val localInputsFlow =
      combine(isSavingFlow, isErrorFlow, showConfirmAlertFlow) { isSaving, isError, showConfirmAlert ->
        LocalInputs(isSaving, isError, showConfirmAlert)
      }

    val uiState: StateFlow<ReviewStorageChangesUiState> =
      combine(sessionInputsFlow, localInputsFlow) { session, local ->
        val changedItems =
          session.items
            .orEmpty()
            .filter { it.quantityChange != null }
            .sortedBy { it.name }
        var up = 0
        var down = 0
        changedItems.forEach { item ->
          val change = item.quantityChange ?: 0
          if (change > 0) up += change else down -= change
        }

        ReviewStorageChangesUiState(
          changedItems = changedItems,
          up = up,
          down = down,
          isInternetReachable = session.isInternetReachable,
          isSaving = local.isSaving,
          isError = local.isError,
          showConfirmAlert = local.showConfirmAlert,
          reallyUnexpectedBlocker = session.primaryStoreDetails == null,
        )
      }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = ReviewStorageChangesUiState(),
      )

    fun requestConfirm() {
      if (uiState.value.canConfirm) {
        showConfirmAlertFlow.value = true
      }
    }

    fun dismissConfirm() {
      showConfirmAlertFlow.value = false
    }

    fun confirmSend(onSaved: () -> Unit) {
      viewModelScope.launch {
        showConfirmAlertFlow.value = false
        isErrorFlow.value = false
        isSavingFlow.value = true

        val changedItems = uiState.value.changedItems
        val result =
          if (changedItems.isEmpty()) {
            ApiResource.Success(Unit)
          } else {
            itemsRepository.saveSelectedItems(
              SaveSelectedItemsRequestDto(
                SaveSelectedItemsRequestDataDto(
                  changedItems.map { it.toExpirationChangeDto() },
                ),
              ),
            )
          }

        when (result) {
          is ApiResource.Success -> {
            storageSessionStore.setIsStorageSavedToApi(true)
            isSavingFlow.value = false
            onSaved()
          }
          is ApiResource.Error -> {
            isSavingFlow.value = false
            isErrorFlow.value = true
          }
          is ApiResource.Loading -> Unit
        }
      }
    }

    private fun StorageListItem.toExpirationChangeDto(): ExpirationChangeDto {
      val starting = originalQuantity ?: 0.0
      val change = (quantityChange ?: 0).toDouble()
      return ExpirationChangeDto(
        expirationId = expirationId,
        startingQuantity = starting,
        quantityChange = change,
        finalQuantity = starting + change,
      )
    }
  }
