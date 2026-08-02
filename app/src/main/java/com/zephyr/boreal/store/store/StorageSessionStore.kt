package com.zephyr.boreal.store.store

import com.zephyr.boreal.domain.model.StorageListItem
import com.zephyr.boreal.domain.model.StoreDetails
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageSessionStore
  @Inject
  constructor() {
    private val _primaryStoreDetails = MutableStateFlow<StoreDetails?>(null)
    val primaryStoreDetails: StateFlow<StoreDetails?> = _primaryStoreDetails.asStateFlow()

    // Track the initial state of the selected store at the start of the loading session
    private val _selectedStoreInitialState = MutableStateFlow<StoreDetails?>(null)
    val selectedStoreInitialState: StateFlow<StoreDetails?> = _selectedStoreInitialState.asStateFlow()

    // Track the active state of the selected store (expirations updated dynamically in-memory)
    private val _selectedStoreCurrentState = MutableStateFlow<StoreDetails?>(null)
    val selectedStoreCurrentState: StateFlow<StoreDetails?> = _selectedStoreCurrentState.asStateFlow()

    // The working item x expiration-batch list being edited on the select-items-from-store screen
    private val _storageListItems = MutableStateFlow<List<StorageListItem>?>(null)
    val storageListItems: StateFlow<List<StorageListItem>?> = _storageListItems.asStateFlow()

    /**
     * Initializes the storage session with newly loaded primary and selected store details.
     * The selected store's initial and current active state will both be set to this base.
     */
    fun setStores(
      primary: StoreDetails,
      selected: StoreDetails,
    ) {
      _primaryStoreDetails.value = primary
      _selectedStoreInitialState.value = selected
      _selectedStoreCurrentState.value = selected
    }

    /**
     * Sets the working item x expiration-batch list for the select-items-from-store screen.
     */
    fun setStorageListItems(items: List<StorageListItem>) {
      _storageListItems.value = items
    }

    /**
     * Updates the quantity change of a single working-list row, matched by item and expiration id.
     */
    fun updateQuantityChange(
      itemId: Int,
      expirationId: Int,
      quantityChange: Int?,
    ) {
      _storageListItems.update { items ->
        items?.map { item ->
          if (item.itemId == itemId && item.expirationId == expirationId) {
            item.copy(quantityChange = quantityChange)
          } else {
            item
          }
        }
      }
    }

    /**
     * Clears the storage session state entirely.
     */
    fun clearStores() {
      _primaryStoreDetails.value = null
      _selectedStoreInitialState.value = null
      _selectedStoreCurrentState.value = null
      _storageListItems.value = null
    }
  }
