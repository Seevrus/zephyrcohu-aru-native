package com.zephyr.boreal.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zephyr.boreal.data.repository.ApiResource
import com.zephyr.boreal.data.repository.ItemsRepository
import com.zephyr.boreal.data.repository.StoresRepository
import com.zephyr.boreal.domain.model.StorageListItem
import com.zephyr.boreal.store.store.StorageSessionStore
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
import kotlin.math.floor

private const val EXPIRES_AT_LENGTH = 6

/**
 * Replicates the RN `quantityHandler` bounds exactly: only a lower bound (returning more stock
 * than the selected store currently holds) is enforced. There is deliberately no upper bound
 * against [originalQuantity]'s counterpart primary-store quantity, since primary-store counts can
 * lag reality.
 */
fun resolveQuantityChange(
  rawText: String,
  originalQuantity: Double?,
): Int? {
  val minimum = -(originalQuantity ?: 0.0).toInt()
  val trimmed = rawText.trim().replace(',', '.')
  val floored = trimmed.toDoubleOrNull()?.let { floor(it).toInt() }
  return when {
    trimmed.isEmpty() || floored == null || floored == 0 -> null
    floored == -1 && minimum == 0 -> null
    floored <= minimum -> minimum
    else -> floored
  }
}

data class SelectItemsFromStoreUiState(
  val items: List<StorageListItem> = emptyList(),
  val searchQuery: String = "",
  val expandedExpirationIds: Set<Int> = emptySet(),
  val isLoading: Boolean = true,
  val isAbandoning: Boolean = false,
  val showAbandonConfirm: Boolean = false,
) {
  val displayItems: List<StorageListItem>
    get() =
      items
        .filter { item ->
          searchQuery.isBlank() ||
            item.name.contains(searchQuery, ignoreCase = true) ||
            item.expiresAt.startsWith(searchQuery) ||
            item.itemBarcode.startsWith(searchQuery) ||
            item.expirationBarcode.startsWith(searchQuery)
        }.sortedBy { it.name }

  val isAnyItemChanged: Boolean
    get() = items.any { it.quantityChange != null }
}

@HiltViewModel
class SelectItemsFromStoreViewModel
  @Inject
  constructor(
    private val itemsRepository: ItemsRepository,
    private val storesRepository: StoresRepository,
    private val storageSessionStore: StorageSessionStore,
  ) : ViewModel() {
    private val searchQueryFlow = MutableStateFlow("")
    private val expandedExpirationIdsFlow = MutableStateFlow<Set<Int>>(emptySet())
    private val isAbandoningFlow = MutableStateFlow(false)
    private val showAbandonConfirmFlow = MutableStateFlow(false)

    init {
      if (storageSessionStore.storageListItems.value == null) {
        viewModelScope.launch {
          val itemsResource = itemsRepository.getItems().first { it !is ApiResource.Loading }
          val catalogItems = itemsResource.getOrNull() ?: emptyList()
          val primary = storageSessionStore.primaryStoreDetails.value
          val selectedInitial = storageSessionStore.selectedStoreInitialState.value

          val storageListItems =
            catalogItems.flatMap { item ->
              item.expirations.map { exp ->
                StorageListItem(
                  itemId = item.id,
                  expirationId = exp.id,
                  articleNumber = item.articleNumber,
                  name = item.name,
                  expiresAt = exp.expiresAt.take(EXPIRES_AT_LENGTH),
                  itemBarcode = item.barcode.orEmpty(),
                  expirationBarcode = exp.barcode.orEmpty(),
                  unitName = item.unitName,
                  primaryStoreQuantity =
                    primary
                      ?.expirations
                      ?.find {
                        it.itemId == item.id && it.expirationId == exp.id
                      }?.quantity,
                  originalQuantity =
                    selectedInitial
                      ?.expirations
                      ?.find {
                        it.itemId == item.id && it.expirationId == exp.id
                      }?.quantity,
                )
              }
            }

          storageSessionStore.setStorageListItems(storageListItems)
        }
      }
    }

    val uiState: StateFlow<SelectItemsFromStoreUiState> =
      combine(
        storageSessionStore.storageListItems,
        searchQueryFlow,
        expandedExpirationIdsFlow,
        isAbandoningFlow,
        showAbandonConfirmFlow,
      ) { items, search, expandedIds, isAbandoning, showConfirm ->
        SelectItemsFromStoreUiState(
          items = items ?: emptyList(),
          searchQuery = search,
          expandedExpirationIds = expandedIds,
          isLoading = items == null,
          isAbandoning = isAbandoning,
          showAbandonConfirm = showConfirm,
        )
      }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = SelectItemsFromStoreUiState(),
      )

    fun onSearchQueryChanged(query: String) {
      searchQueryFlow.value = query
    }

    fun onToggleExpanded(expirationId: Int) {
      expandedExpirationIdsFlow.update { current ->
        if (current.contains(expirationId)) current - expirationId else current + expirationId
      }
    }

    fun onQuantityChanged(
      itemId: Int,
      expirationId: Int,
      quantityChange: Int?,
    ) {
      storageSessionStore.updateQuantityChange(itemId, expirationId, quantityChange)
    }

    fun requestAbandon(onAbandoned: () -> Unit) {
      if (uiState.value.isAnyItemChanged) {
        showAbandonConfirmFlow.value = true
      } else {
        abandon(onAbandoned)
      }
    }

    fun confirmAbandon(onAbandoned: () -> Unit) {
      showAbandonConfirmFlow.value = false
      abandon(onAbandoned)
    }

    fun dismissAbandonConfirm() {
      showAbandonConfirmFlow.value = false
    }

    private fun abandon(onAbandoned: () -> Unit) {
      viewModelScope.launch {
        isAbandoningFlow.value = true
        storesRepository.deselectStore()
        storageSessionStore.clearStores()
        isAbandoningFlow.value = false
        onAbandoned()
      }
    }

    private companion object {
      const val STOP_TIMEOUT_MS = 5000L
    }
  }
