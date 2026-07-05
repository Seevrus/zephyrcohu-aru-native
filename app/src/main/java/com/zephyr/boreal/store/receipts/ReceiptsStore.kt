package com.zephyr.boreal.store.receipts

import com.zephyr.boreal.domain.model.DraftOrder
import com.zephyr.boreal.domain.model.DraftReceipt
import com.zephyr.boreal.domain.model.Receipt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReceiptsStore
  @Inject
  constructor() {
    private val _receipts = MutableStateFlow<List<Receipt>>(emptyList())
    val receipts: StateFlow<List<Receipt>> = _receipts.asStateFlow()

    private val _currentReceipt = MutableStateFlow<DraftReceipt?>(null)
    val currentReceipt: StateFlow<DraftReceipt?> = _currentReceipt.asStateFlow()

    private val _selectedItems = MutableStateFlow<Map<Int, Map<Int, Double>>>(emptyMap())
    val selectedItems: StateFlow<Map<Int, Map<Int, Double>>> = _selectedItems.asStateFlow()

    private val _selectedOrderItems = MutableStateFlow<Map<Int, Double>>(emptyMap())
    val selectedOrderItems: StateFlow<Map<Int, Double>> = _selectedOrderItems.asStateFlow()

    private val _currentOrder = MutableStateFlow<DraftOrder?>(null)
    val currentOrder: StateFlow<DraftOrder?> = _currentOrder.asStateFlow()

    fun resetReceipts() {
      _receipts.value = emptyList()
      _currentReceipt.value = null
      _selectedItems.value = emptyMap()
      _selectedOrderItems.value = emptyMap()
      _currentOrder.value = null
    }

    fun setCurrentReceipt(receipt: DraftReceipt?) {
      _currentReceipt.value = receipt
    }

    fun setCurrentOrder(order: DraftOrder?) {
      _currentOrder.value = order
    }

    fun upsertSelectedItem(
      itemId: Int,
      expirationId: Int,
      quantity: Double?,
    ) {
      _selectedItems.update { current ->
        val itemMap = current[itemId]?.toMutableMap() ?: mutableMapOf()
        if (quantity == null || quantity <= 0) {
          itemMap.remove(expirationId)
        } else {
          itemMap[expirationId] = quantity
        }

        if (itemMap.isEmpty()) {
          current - itemId
        } else {
          current + (itemId to itemMap)
        }
      }
    }

    fun upsertOrderItem(
      itemId: Int,
      quantity: Double?,
    ) {
      _selectedOrderItems.update { current ->
        if (quantity == null || quantity <= 0) {
          current - itemId
        } else {
          current + (itemId to quantity)
        }
      }
    }

    fun updateCurrentReceipt(updateFn: (DraftReceipt) -> DraftReceipt) {
      _currentReceipt.update { current ->
        val active = current ?: DraftReceipt()
        updateFn(active)
      }
    }

    fun addReceipt(receipt: Receipt) {
      _receipts.update { it + receipt }
    }
  }
