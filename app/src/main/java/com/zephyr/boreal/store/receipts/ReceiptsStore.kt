package com.zephyr.boreal.store.receipts

import com.zephyr.boreal.data.local.dao.ReceiptDao
import com.zephyr.boreal.data.mapper.toDomain
import com.zephyr.boreal.data.mapper.toEntity
import com.zephyr.boreal.domain.model.DraftOrder
import com.zephyr.boreal.domain.model.DraftReceipt
import com.zephyr.boreal.domain.model.Receipt
import com.zephyr.boreal.store.core.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReceiptsStore
  @Inject
  constructor(
    private val receiptDao: ReceiptDao,
    @param:ApplicationScope scope: CoroutineScope,
  ) {
    /**
     * Backed by Room, so [addReceipt] only writes to the database — this flow updates
     * asynchronously once Room's invalidation tracker re-runs the query on [scope]. Do not
     * assume `receipts.value` already reflects a receipt added earlier in the same coroutine;
     * read it before submitting, not right after.
     */
    val receipts: StateFlow<List<Receipt>> =
      receiptDao
        .getAllReceipts()
        .map { entities -> entities.map { it.toDomain() } }
        .stateIn(scope = scope, started = SharingStarted.Eagerly, initialValue = emptyList())

    private val _currentReceipt = MutableStateFlow<DraftReceipt?>(null)
    val currentReceipt: StateFlow<DraftReceipt?> = _currentReceipt.asStateFlow()

    private val _selectedItems = MutableStateFlow<Map<Int, Map<Int, Double>>>(emptyMap())
    val selectedItems: StateFlow<Map<Int, Map<Int, Double>>> = _selectedItems.asStateFlow()

    private val _selectedOrderItems = MutableStateFlow<Map<Int, Double>>(emptyMap())
    val selectedOrderItems: StateFlow<Map<Int, Double>> = _selectedOrderItems.asStateFlow()

    private val _currentOrder = MutableStateFlow<DraftOrder?>(null)
    val currentOrder: StateFlow<DraftOrder?> = _currentOrder.asStateFlow()

    /** [receipts] does not reflect this receipt until Room re-queries — see its KDoc. */
    suspend fun addReceipt(receipt: Receipt) {
      receiptDao.insertReceipt(receipt.toEntity())
    }

    suspend fun resetReceipts() {
      receiptDao.clearReceipts()
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
  }
