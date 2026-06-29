package com.zephyr.boreal.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zephyr.boreal.domain.model.ReceiptItem
import com.zephyr.boreal.domain.model.ReceiptOtherItem
import com.zephyr.boreal.store.receipts.ReceiptsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReviewItemsUiState(
  val items: List<ReceiptItem> = emptyList(),
  val otherItems: List<ReceiptOtherItem> = emptyList(),
  val grossTotal: Double = 0.0,
  val expandedItemKeys: Set<String> = emptySet(),
  val expandedOtherItemIds: Set<Int> = emptySet(),
  val showCancelConfirmation: Boolean = false,
)

@HiltViewModel
class ReviewItemsViewModel
  @Inject
  constructor(
    private val receiptsStore: ReceiptsStore,
  ) : ViewModel() {
    private val _uiState = MutableStateFlow(ReviewItemsUiState())
    val uiState: StateFlow<ReviewItemsUiState> = _uiState.asStateFlow()

    init {
      viewModelScope.launch {
        receiptsStore.currentReceipt.collect { draft ->
          val items = draft?.items ?: emptyList()
          val otherItems = draft?.otherItems ?: emptyList()
          _uiState.update {
            it.copy(
              items = items,
              otherItems = otherItems,
              grossTotal = items.sumOf { item -> item.grossAmount } + otherItems.sumOf { item -> item.grossAmount },
            )
          }
        }
      }
    }

    fun onToggleExpanded(key: String) {
      _uiState.update { state ->
        val newKeys =
          if (state.expandedItemKeys.contains(key)) {
            state.expandedItemKeys - key
          } else {
            state.expandedItemKeys + key
          }
        state.copy(expandedItemKeys = newKeys)
      }
    }

    fun onToggleOtherItemExpanded(id: Int) {
      _uiState.update { state ->
        val newIds =
          if (state.expandedOtherItemIds.contains(id)) {
            state.expandedOtherItemIds - id
          } else {
            state.expandedOtherItemIds + id
          }
        state.copy(expandedOtherItemIds = newIds)
      }
    }

    fun removeItem(
      id: Int,
      expirationId: Int,
      onNavigateHome: () -> Unit,
    ) {
      receiptsStore.upsertSelectedItem(id, expirationId, null)
      receiptsStore.updateCurrentReceipt { draft ->
        draft.copy(
          items = draft.items.filter { !(it.id == id && it.expirationId == expirationId) },
        )
      }
      if (receiptsStore.currentReceipt.value
          ?.items
          .isNullOrEmpty()
      ) {
        receiptsStore.resetReceipts()
        onNavigateHome()
      }
    }

    fun removeOtherItem(id: Int) {
      receiptsStore.updateCurrentReceipt { draft ->
        draft.copy(otherItems = draft.otherItems.filter { it.id != id })
      }
    }

    fun showCancelDialog() {
      _uiState.update { it.copy(showCancelConfirmation = true) }
    }

    fun dismissCancelDialog() {
      _uiState.update { it.copy(showCancelConfirmation = false) }
    }

    fun cancelReceipt(onNavigateHome: () -> Unit) {
      receiptsStore.resetReceipts()
      onNavigateHome()
    }
  }
