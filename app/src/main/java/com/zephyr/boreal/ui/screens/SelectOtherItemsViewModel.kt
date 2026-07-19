package com.zephyr.boreal.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zephyr.boreal.data.repository.ApiResource
import com.zephyr.boreal.data.repository.ItemsRepository
import com.zephyr.boreal.domain.model.OtherItem
import com.zephyr.boreal.domain.model.ReceiptOtherItem
import com.zephyr.boreal.domain.model.TempSelection
import com.zephyr.boreal.domain.utils.AmountCalculator
import com.zephyr.boreal.store.receipts.ReceiptsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

data class SelectOtherItemsUiState(
  val isLoading: Boolean = true,
  val searchQuery: String = "",
  val items: List<OtherItem> = emptyList(),
  val selections: Map<Int, TempSelection> = emptyMap(),
  val netTotal: Double = 0.0,
  val grossTotal: Double = 0.0,
  val canAccept: Boolean = false,
  val expandedItemIds: Set<Int> = emptySet(),
)

@HiltViewModel
class SelectOtherItemsViewModel
  @Inject
  constructor(
    private val itemsRepository: ItemsRepository,
    private val receiptsStore: ReceiptsStore,
  ) : ViewModel() {
    private val catalogFlow = MutableStateFlow<ApiResource<List<OtherItem>>>(ApiResource.Loading())

    private val _uiState = MutableStateFlow(SelectOtherItemsUiState())
    val uiState: StateFlow<SelectOtherItemsUiState> = _uiState.asStateFlow()

    init {
      val persisted = receiptsStore.otherItemSelections.value
      val initialSelections =
        persisted.ifEmpty {
          val existing = receiptsStore.currentReceipt.value?.otherItems ?: emptyList()
          existing.associate { item ->
            item.id to
              TempSelection(
                netPrice = item.netPrice,
                quantity = item.quantity.roundToInt(),
                comment = item.comment?.takeIf { it.isNotBlank() },
              )
          }
        }
      _uiState.update { it.copy(selections = initialSelections) }

      viewModelScope.launch {
        itemsRepository.getOtherItems().collect { resource ->
          catalogFlow.value = resource
          updateItems()
        }
      }
    }

    private fun computeFilteredItems(
      allItems: List<OtherItem>,
      searchQuery: String,
    ): List<OtherItem> =
      allItems
        .filter { it.name.contains(searchQuery, ignoreCase = true) }
        .sortedBy { it.name }

    private fun computeTotals(
      allItems: List<OtherItem>,
      selections: Map<Int, TempSelection>,
    ): Pair<Double, Double> {
      var netTotal = 0.0
      var grossTotal = 0.0
      selections.forEach { (itemId, sel) ->
        if ((sel.quantity ?: 0) > 0) {
          val item = allItems.find { it.id == itemId }
          if (item != null) {
            val effectivePrice = sel.netPrice ?: item.netPrice
            val amounts = AmountCalculator.calculateAmounts(effectivePrice, sel.quantity!!.toDouble(), item.vatRate)
            netTotal += amounts.netAmount
            grossTotal += amounts.grossAmount
          }
        }
      }
      return netTotal to grossTotal
    }

    private fun updateItems() {
      val resource = catalogFlow.value
      val allItems = resource.getOrNull() ?: emptyList()
      val current = _uiState.value
      val filtered = computeFilteredItems(allItems, current.searchQuery)
      val (netTotal, grossTotal) = computeTotals(allItems, current.selections)
      _uiState.update { state ->
        state.copy(
          isLoading = resource.isLoading,
          items = filtered,
          netTotal = netTotal,
          grossTotal = grossTotal,
          canAccept = state.selections.any { (_, sel) -> (sel.quantity ?: 0) > 0 },
        )
      }
    }

    private fun updateSelectionsAndTotals(newSelections: Map<Int, TempSelection>) {
      val allItems = catalogFlow.value.getOrNull() ?: emptyList()
      val (netTotal, grossTotal) = computeTotals(allItems, newSelections)
      _uiState.update { state ->
        state.copy(
          selections = newSelections,
          netTotal = netTotal,
          grossTotal = grossTotal,
          canAccept = newSelections.any { (_, sel) -> (sel.quantity ?: 0) > 0 },
        )
      }
      receiptsStore.setOtherItemSelections(newSelections)
    }

    fun onSearchQueryChanged(query: String) {
      val allItems = catalogFlow.value.getOrNull() ?: emptyList()
      val filtered = computeFilteredItems(allItems, query)
      _uiState.update { state ->
        state.copy(
          searchQuery = query,
          items = filtered,
        )
      }
    }

    fun onToggleExpanded(itemId: Int) {
      _uiState.update { state ->
        val newIds =
          if (state.expandedItemIds.contains(itemId)) {
            state.expandedItemIds - itemId
          } else {
            state.expandedItemIds + itemId
          }
        state.copy(expandedItemIds = newIds)
      }
    }

    fun onQuantityChanged(
      itemId: Int,
      quantity: Int?,
    ) {
      val existing = _uiState.value.selections[itemId] ?: TempSelection()
      val newSelections = _uiState.value.selections + (itemId to existing.copy(quantity = quantity))
      updateSelectionsAndTotals(newSelections)
    }

    fun onNetPriceChanged(
      itemId: Int,
      netPrice: Double?,
    ) {
      val existing = _uiState.value.selections[itemId] ?: TempSelection()
      val newSelections = _uiState.value.selections + (itemId to existing.copy(netPrice = netPrice))
      updateSelectionsAndTotals(newSelections)
    }

    fun onCommentChanged(
      itemId: Int,
      comment: String?,
    ) {
      val sanitized = comment?.takeIf { it.isNotBlank() }
      val existing = _uiState.value.selections[itemId] ?: TempSelection()
      val newSelections = _uiState.value.selections + (itemId to existing.copy(comment = sanitized))
      _uiState.update { state -> state.copy(selections = newSelections) }
      receiptsStore.setOtherItemSelections(newSelections)
    }

    fun confirmHandler(onSuccess: () -> Unit) {
      val catalog = catalogFlow.value.getOrNull() ?: return
      val selections = _uiState.value.selections

      val receiptOtherItems =
        selections
          .filter { (_, sel) -> (sel.quantity ?: 0) > 0 }
          .mapNotNull { (itemId, sel) ->
            val item = catalog.find { it.id == itemId } ?: return@mapNotNull null
            val effectivePrice = sel.netPrice ?: item.netPrice
            val qty = sel.quantity!!.toDouble()
            val amounts = AmountCalculator.calculateAmounts(effectivePrice, qty, item.vatRate)
            ReceiptOtherItem(
              id = item.id,
              articleNumber = item.articleNumber,
              name = item.name,
              quantity = qty,
              unitName = item.unitName,
              netPrice = effectivePrice,
              netAmount = amounts.netAmount,
              vatRate = item.vatRate,
              vatAmount = amounts.vatAmount,
              grossAmount = amounts.grossAmount,
              comment = sel.comment,
            )
          }

      receiptsStore.updateCurrentReceipt { draft ->
        draft.copy(otherItems = receiptOtherItems)
      }
      onSuccess()
    }
  }
