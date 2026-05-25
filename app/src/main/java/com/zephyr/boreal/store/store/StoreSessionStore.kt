package com.zephyr.boreal.store.store

import com.zephyr.boreal.domain.model.ReceiptItem
import com.zephyr.boreal.domain.model.StoreDetails
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StoreSessionStore
  @Inject
  constructor() {
    // Track the initial state of the store at the start of the round
    private val _selectedStoreInitialState = MutableStateFlow<StoreDetails?>(null)
    val selectedStoreInitialState: StateFlow<StoreDetails?> = _selectedStoreInitialState.asStateFlow()

    // Track the active state of the store (expirations updated dynamically in-memory)
    private val _selectedStoreCurrentState = MutableStateFlow<StoreDetails?>(null)
    val selectedStoreCurrentState: StateFlow<StoreDetails?> = _selectedStoreCurrentState.asStateFlow()

    // Convenient alias for general consumption
    val selectedStore: StateFlow<StoreDetails?> = _selectedStoreCurrentState

    /**
     * Initializes the store state with a newly loaded StoreDetails object.
     * Both the initial and current active state will be set to this base.
     */
    fun setStore(storeDetails: StoreDetails) {
      _selectedStoreInitialState.value = storeDetails
      _selectedStoreCurrentState.value = storeDetails
    }

    /**
     * Decrements expiration quantities from the current store based on items sold.
     * @param soldQuantities Maps itemId -> (expirationId -> quantitySold)
     */
    fun sellItems(soldQuantities: Map<Int, Map<Int, Double>>) {
      _selectedStoreCurrentState.update { current ->
        current?.copy(
          expirations =
            current.expirations.map { expiration ->
              val soldQuantity = soldQuantities[expiration.itemId]?.get(expiration.expirationId) ?: 0.0
              if (soldQuantity > 0.0) {
                expiration.copy(quantity = maxOf(0.0, expiration.quantity - soldQuantity))
              } else {
                expiration
              }
            },
        )
      }
    }

    /**
     * Restores expiration quantities to the current store from a canceled receipt.
     * @param canceledItems List of ReceiptItem from the canceled transaction
     */
    fun restoreCanceledItems(canceledItems: List<ReceiptItem>) {
      val canceledMap =
        canceledItems
          .groupBy { it.id to it.expirationId }
          .mapValues { (_, items) -> items.sumOf { it.quantity } }

      _selectedStoreCurrentState.update { current ->
        current?.copy(
          expirations =
            current.expirations.map { expiration ->
              val restoreQuantity = canceledMap[expiration.itemId to expiration.expirationId] ?: 0.0
              if (restoreQuantity > 0.0) {
                expiration.copy(quantity = expiration.quantity + restoreQuantity)
              } else {
                expiration
              }
            },
        )
      }
    }

    /**
     * Updates an expiration's quantity directly. Useful for manual storage adjustments/loads.
     * @param itemId The product/item ID
     * @param expirationId The specific batch expiration ID
     * @param quantityChange The double delta to apply (positive or negative)
     */
    fun updateExpirationQuantity(
      itemId: Int,
      expirationId: Int,
      quantityChange: Double,
    ) {
      _selectedStoreCurrentState.update { current ->
        current?.copy(
          expirations =
            current.expirations.map { expiration ->
              if (expiration.itemId == itemId && expiration.expirationId == expirationId) {
                expiration.copy(quantity = maxOf(0.0, expiration.quantity + quantityChange))
              } else {
                expiration
              }
            },
        )
      }
    }

    /**
     * Resets only the active mutable state back to the initial round snapshot.
     */
    fun resetSelectedStoreCurrentState() {
      _selectedStoreCurrentState.value = _selectedStoreInitialState.value
    }

    /**
     * Clears the store state entirely.
     */
    fun clearStore() {
      _selectedStoreInitialState.value = null
      _selectedStoreCurrentState.value = null
    }
  }
