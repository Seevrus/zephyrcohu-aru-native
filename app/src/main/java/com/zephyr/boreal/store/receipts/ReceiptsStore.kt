package com.zephyr.boreal.store.receipts

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

    fun resetReceipts() {
      _receipts.value = emptyList()
      _currentReceipt.value = null
    }

    fun setCurrentReceipt(receipt: DraftReceipt?) {
      _currentReceipt.value = receipt
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
