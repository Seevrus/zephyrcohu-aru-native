package com.zephyr.boreal.store.receipts

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.zephyr.boreal.data.local.dao.ReceiptDao
import com.zephyr.boreal.data.mapper.toDomain
import com.zephyr.boreal.data.mapper.toEntity
import com.zephyr.boreal.domain.model.DraftOrder
import com.zephyr.boreal.domain.model.DraftReceipt
import com.zephyr.boreal.domain.model.Receipt
import com.zephyr.boreal.domain.model.TempSelection
import com.zephyr.boreal.store.core.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReceiptsStore
  @Inject
  constructor(
    private val receiptDao: ReceiptDao,
    private val dataStore: DataStore<Preferences>,
    @param:ApplicationScope private val scope: CoroutineScope,
  ) {
    companion object {
      private const val TAG = "ReceiptsStore"
      val DRAFT_SELL_FLOW_STATE = stringPreferencesKey("draft_sell_flow_state")

      /**
       * Max window in which a process death loses draft edits made after the last debounced
       * write landed. 300ms is the write-amplification/data-loss-window trade-off for a
       * per-keystroke draft (Select Items quantity changes).
       */
      private const val DRAFT_PERSIST_DEBOUNCE_MS = 300L
    }

    private val json = Json { ignoreUnknownKeys = true }

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
        .stateIn(scope = scope, started = SharingStarted.WhileSubscribed(5000), initialValue = emptyList())

    /** Looks the receipt up directly rather than scanning [receipts], so callers don't pay for
     * the full list and aren't affected by [receipts]' `WhileSubscribed` staleness window. */
    fun receiptBySerialNumber(serialNumber: Int): Flow<Receipt?> =
      receiptDao.findBySerialNumber(serialNumber).map { it?.toDomain() }

    /** Reads the database directly rather than [receipts], which can lag behind [addReceipt]. */
    suspend fun nextAvailableSerialNumber(firstAvailableSerialNumber: Int): Int =
      (receiptDao.getMaxSerialNumber() ?: (firstAvailableSerialNumber - 1)) + 1

    private val _currentReceipt = MutableStateFlow<DraftReceipt?>(null)
    val currentReceipt: StateFlow<DraftReceipt?> = _currentReceipt.asStateFlow()

    private val _selectedItems = MutableStateFlow<Map<Int, Map<Int, Double>>>(emptyMap())
    val selectedItems: StateFlow<Map<Int, Map<Int, Double>>> = _selectedItems.asStateFlow()

    private val _selectedOrderItems = MutableStateFlow<Map<Int, Double>>(emptyMap())
    val selectedOrderItems: StateFlow<Map<Int, Double>> = _selectedOrderItems.asStateFlow()

    private val _currentOrder = MutableStateFlow<DraftOrder?>(null)
    val currentOrder: StateFlow<DraftOrder?> = _currentOrder.asStateFlow()

    private val _otherItemSelections = MutableStateFlow<Map<Int, TempSelection>>(emptyMap())
    val otherItemSelections: StateFlow<Map<Int, TempSelection>> = _otherItemSelections.asStateFlow()

    /**
     * Single source of pending draft writes. Mutators update this (via [scheduleDraftPersist])
     * instead of launching their own [dataStore] write, so a burst of edits collapses into one
     * debounced write instead of racing independent coroutines — see [DRAFT_PERSIST_DEBOUNCE_MS].
     */
    private val draftSnapshot = MutableStateFlow<DraftSellFlowState?>(null)

    init {
      scope.launch {
        val decoded = decodeDraftState(dataStore.data.first())
        _currentReceipt.value = decoded.currentReceipt
        _currentOrder.value = decoded.currentOrder
        _selectedItems.value = decoded.selectedItems
        _selectedOrderItems.value = decoded.selectedOrderItems
        _otherItemSelections.value = decoded.otherItemSelections
      }
      scope.launch {
        draftSnapshot
          .debounce(DRAFT_PERSIST_DEBOUNCE_MS)
          .collectLatest { snapshot ->
            if (snapshot != null) {
              dataStore.edit { it[DRAFT_SELL_FLOW_STATE] = json.encodeToString(snapshot) }
            }
          }
      }
    }

    private fun decodeDraftState(preferences: Preferences): DraftSellFlowState {
      val raw = preferences[DRAFT_SELL_FLOW_STATE] ?: return DraftSellFlowState()
      return try {
        json.decodeFromString(raw)
      } catch (e: SerializationException) {
        Log.w(TAG, "Corrupt persisted draft sell-flow state, falling back to empty draft", e)
        DraftSellFlowState()
      }
    }

    private fun scheduleDraftPersist() {
      draftSnapshot.value =
        DraftSellFlowState(
          currentReceipt = _currentReceipt.value,
          currentOrder = _currentOrder.value,
          selectedItems = _selectedItems.value,
          selectedOrderItems = _selectedOrderItems.value,
          otherItemSelections = _otherItemSelections.value,
        )
    }

    /** [receipts] does not reflect this receipt until Room re-queries — see its KDoc. */
    suspend fun addReceipt(receipt: Receipt) {
      receiptDao.insertReceipt(receipt.toEntity())
    }

    suspend fun markReceiptPrinted(id: Int) {
      receiptDao.updatePrintedState(id, true)
    }

    suspend fun resetReceipts() {
      receiptDao.clearReceipts()
      _currentReceipt.value = null
      _selectedItems.value = emptyMap()
      _selectedOrderItems.value = emptyMap()
      _currentOrder.value = null
      _otherItemSelections.value = emptyMap()
      draftSnapshot.value = null
      dataStore.edit { it.remove(DRAFT_SELL_FLOW_STATE) }
    }

    /**
     * Clears only the in-progress sell-flow draft, unlike [resetReceipts] which also wipes
     * persisted receipt history via [ReceiptDao.clearReceipts]. Use this when ending a sell
     * session after a receipt has already been finalized and must be kept (e.g. leaving the
     * Summary screen), where clearing history would delete the receipt just created.
     */
    suspend fun resetSellFlowDraft() {
      _currentReceipt.value = null
      _selectedItems.value = emptyMap()
      _selectedOrderItems.value = emptyMap()
      _currentOrder.value = null
      _otherItemSelections.value = emptyMap()
      draftSnapshot.value = null
      dataStore.edit { it.remove(DRAFT_SELL_FLOW_STATE) }
    }

    fun setCurrentReceipt(receipt: DraftReceipt?) {
      _currentReceipt.value = receipt
      scheduleDraftPersist()
    }

    fun setCurrentOrder(order: DraftOrder?) {
      _currentOrder.value = order
      scheduleDraftPersist()
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
      scheduleDraftPersist()
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
      scheduleDraftPersist()
    }

    fun updateCurrentReceipt(updateFn: (DraftReceipt) -> DraftReceipt) {
      _currentReceipt.update { current ->
        val active = current ?: DraftReceipt()
        updateFn(active)
      }
      scheduleDraftPersist()
    }

    fun setOtherItemSelections(selections: Map<Int, TempSelection>) {
      _otherItemSelections.value = selections
      scheduleDraftPersist()
    }
  }
