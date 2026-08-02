package com.zephyr.boreal.store.store

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.zephyr.boreal.data.local.converters.PersistenceJson
import com.zephyr.boreal.domain.model.StorageListItem
import com.zephyr.boreal.domain.model.StoreDetails
import com.zephyr.boreal.store.core.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persisted across process death: nothing server-side recreates "changes staged but not yet
 * saved/unlocked", so this is a **record** per the persistence-durability convention (see
 * `rn-migration-tasks/00-overview.md`), not a cache. [isStorageSavedToApi] in particular lets the
 * storage flow resume directly at the summary screen if the app dies after a successful
 * `storage/load` save but before the `storage/unlock_from_user` call runs.
 */
@Singleton
class StorageSessionStore
  @Inject
  constructor(
    private val dataStore: DataStore<Preferences>,
    @param:ApplicationScope private val scope: CoroutineScope,
  ) {
    companion object {
      private const val TAG = "StorageSessionStore"
      val DRAFT_STORAGE_FLOW_STATE = stringPreferencesKey("draft_storage_flow_state")
      private const val DRAFT_PERSIST_DEBOUNCE_MS = 300L
    }

    private val json = PersistenceJson.instance

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

    // Set once `storage/load` has succeeded; lets the storage tile resume at the summary screen.
    private val _isStorageSavedToApi = MutableStateFlow(false)
    val isStorageSavedToApi: StateFlow<Boolean> = _isStorageSavedToApi.asStateFlow()

    /**
     * Single source of pending draft writes, collapsing a burst of mutations into one debounced
     * write. See [com.zephyr.boreal.store.receipts.ReceiptsStore] for the identical pattern.
     */
    private val draftSnapshot = MutableStateFlow<DraftStorageFlowState?>(null)

    init {
      scope.launch {
        val decoded = decodeDraftState(dataStore.data.first())
        _primaryStoreDetails.value = decoded.primaryStoreDetails
        _selectedStoreInitialState.value = decoded.selectedStoreInitialState
        _selectedStoreCurrentState.value = decoded.selectedStoreCurrentState
        _storageListItems.value = decoded.storageListItems
        _isStorageSavedToApi.value = decoded.isStorageSavedToApi
      }
      scope.launch {
        draftSnapshot
          .debounce(DRAFT_PERSIST_DEBOUNCE_MS)
          .collectLatest { snapshot ->
            if (snapshot != null) {
              dataStore.edit { it[DRAFT_STORAGE_FLOW_STATE] = json.encodeToString(snapshot) }
            }
          }
      }
    }

    private fun decodeDraftState(preferences: Preferences): DraftStorageFlowState {
      val raw = preferences[DRAFT_STORAGE_FLOW_STATE] ?: return DraftStorageFlowState()
      return try {
        json.decodeFromString(raw)
      } catch (e: SerializationException) {
        Log.w(TAG, "Corrupt persisted draft storage-flow state, falling back to empty draft", e)
        DraftStorageFlowState()
      }
    }

    private fun scheduleDraftPersist() {
      draftSnapshot.value =
        DraftStorageFlowState(
          primaryStoreDetails = _primaryStoreDetails.value,
          selectedStoreInitialState = _selectedStoreInitialState.value,
          selectedStoreCurrentState = _selectedStoreCurrentState.value,
          storageListItems = _storageListItems.value,
          isStorageSavedToApi = _isStorageSavedToApi.value,
        )
    }

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
      scheduleDraftPersist()
    }

    /**
     * Sets the working item x expiration-batch list for the select-items-from-store screen.
     */
    fun setStorageListItems(items: List<StorageListItem>) {
      _storageListItems.value = items
      scheduleDraftPersist()
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
      scheduleDraftPersist()
    }

    /** Set once `storage/load` succeeds, so the flow can resume at Summary after a crash. */
    fun setIsStorageSavedToApi(value: Boolean) {
      _isStorageSavedToApi.value = value
      scheduleDraftPersist()
    }

    /**
     * Clears the storage session state entirely, including the persisted blob. Cleared
     * synchronously (not via the debounced path) so a reset can't be overtaken by a stale pending
     * write, mirroring [com.zephyr.boreal.store.receipts.ReceiptsStore.resetSellFlowDraft].
     */
    suspend fun clearStores() {
      _primaryStoreDetails.value = null
      _selectedStoreInitialState.value = null
      _selectedStoreCurrentState.value = null
      _storageListItems.value = null
      _isStorageSavedToApi.value = false
      draftSnapshot.value = null
      dataStore.edit { it.remove(DRAFT_STORAGE_FLOW_STATE) }
    }
  }
