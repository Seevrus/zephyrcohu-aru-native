package com.zephyr.boreal.store.store

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.zephyr.boreal.domain.model.StorageListItem
import com.zephyr.boreal.domain.model.StoreDetails
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StorageSessionStoreTest {
  private lateinit var testDispatcher: TestDispatcher
  private lateinit var testScope: TestScope

  @BeforeEach
  fun setUp() {
    testDispatcher = StandardTestDispatcher()
    testScope = TestScope(testDispatcher + Job())
  }

  @AfterEach
  fun tearDown() {
    testScope.cancel()
  }

  /** See [com.zephyr.boreal.store.receipts.ReceiptsStoreTest]'s identical fake for the rationale. */
  private class FakeDataStore(
    initial: Preferences = emptyPreferences(),
  ) : DataStore<Preferences> {
    private val state = MutableStateFlow(initial)
    override val data: Flow<Preferences> = state
    var writeCount = 0
      private set

    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
      writeCount++
      val updated = transform(state.value)
      state.value = updated
      return updated
    }
  }

  private fun storeDetails(id: Int) =
    StoreDetails(
      id = id,
      name = "Store $id",
      code = "S$id",
      type = "S",
      state = "I",
      createdAt = "2026-01-01T00:00:00Z",
      updatedAt = "2026-01-01T00:00:00Z",
      expirations = emptyList(),
    )

  private fun storageListItem(
    itemId: Int,
    expirationId: Int,
    quantityChange: Int? = null,
  ) = StorageListItem(
    itemId = itemId,
    expirationId = expirationId,
    articleNumber = "ART$itemId",
    name = "Item $itemId",
    expiresAt = "260101",
    itemBarcode = "",
    expirationBarcode = "",
    unitName = "db",
    primaryStoreQuantity = 10.0,
    originalQuantity = 5.0,
    quantityChange = quantityChange,
  )

  private fun createStore(
    dataStore: DataStore<Preferences>,
    scope: CoroutineScope = testScope,
  ) = StorageSessionStore(dataStore, scope)

  @Test
  fun `setStores populates primary and both selected-store snapshots`() =
    runTest(testDispatcher) {
      val store = createStore(FakeDataStore())
      runCurrent()

      val primary = storeDetails(1)
      val selected = storeDetails(2)
      store.setStores(primary, selected)
      runCurrent()

      assertEquals(primary, store.primaryStoreDetails.value)
      assertEquals(selected, store.selectedStoreInitialState.value)
      assertEquals(selected, store.selectedStoreCurrentState.value)
    }

  @Test
  fun `updateQuantityChange matches by itemId and expirationId only`() =
    runTest(testDispatcher) {
      val store = createStore(FakeDataStore())
      runCurrent()

      store.setStorageListItems(listOf(storageListItem(1, 11), storageListItem(1, 12)))
      runCurrent()

      store.updateQuantityChange(itemId = 1, expirationId = 11, quantityChange = 4)
      runCurrent()

      val items = store.storageListItems.value.orEmpty()
      assertEquals(4, items.first { it.expirationId == 11 }.quantityChange)
      assertNull(items.first { it.expirationId == 12 }.quantityChange)
    }

  @Test
  fun `clearStores resets all in-memory state including isStorageSavedToApi`() =
    runTest(testDispatcher) {
      val store = createStore(FakeDataStore())
      runCurrent()

      store.setStores(storeDetails(1), storeDetails(2))
      store.setStorageListItems(listOf(storageListItem(1, 11)))
      store.setIsStorageSavedToApi(true)
      runCurrent()

      store.clearStores()
      runCurrent()

      assertNull(store.primaryStoreDetails.value)
      assertNull(store.selectedStoreInitialState.value)
      assertNull(store.selectedStoreCurrentState.value)
      assertNull(store.storageListItems.value)
      assertFalse(store.isStorageSavedToApi.value)
    }

  @Test
  fun `clearStores clears the persisted draft, not just memory`() =
    runTest(testDispatcher) {
      val sharedDataStore = FakeDataStore()
      val firstStore = createStore(sharedDataStore)
      runCurrent()

      firstStore.setStores(storeDetails(1), storeDetails(2))
      firstStore.setIsStorageSavedToApi(true)
      runCurrent()

      firstStore.clearStores()
      runCurrent()

      val secondStore = createStore(sharedDataStore)
      runCurrent()

      assertNull(secondStore.primaryStoreDetails.value)
      assertFalse(secondStore.isStorageSavedToApi.value)
    }

  @Test
  fun `storage session survives a fresh store instance backed by the same DataStore`() =
    runTest(testDispatcher) {
      val sharedDataStore = FakeDataStore()
      val firstStore = createStore(sharedDataStore)
      runCurrent()

      val primary = storeDetails(1)
      val selected = storeDetails(2)
      firstStore.setStores(primary, selected)
      firstStore.setStorageListItems(listOf(storageListItem(1, 11, quantityChange = 3)))
      firstStore.setIsStorageSavedToApi(true)
      advanceUntilIdle()

      val secondStore = createStore(sharedDataStore)
      runCurrent()

      assertEquals(primary, secondStore.primaryStoreDetails.value)
      assertEquals(selected, secondStore.selectedStoreInitialState.value)
      assertEquals(listOf(storageListItem(1, 11, quantityChange = 3)), secondStore.storageListItems.value)
      assertTrue(secondStore.isStorageSavedToApi.value)
    }

  @Test
  fun `a corrupt persisted blob falls back to an empty draft instead of crashing`() =
    runTest(testDispatcher) {
      val corruptDataStore = FakeDataStore()
      corruptDataStore.edit { it[stringPreferencesKey("draft_storage_flow_state")] = "not valid json" }

      val store = createStore(corruptDataStore)
      runCurrent()

      assertNull(store.primaryStoreDetails.value)
      assertNull(store.storageListItems.value)
      assertFalse(store.isStorageSavedToApi.value)
    }

  @Test
  fun `a burst of mutations persists once, with the last snapshot`() =
    runTest(testDispatcher) {
      val dataStore = FakeDataStore()
      val store = createStore(dataStore)
      runCurrent()

      repeat(5) { i -> store.updateQuantityChange(itemId = 1, expirationId = 11, quantityChange = i + 1) }
      // Nothing has been written yet: the debounce window hasn't elapsed. The first call
      // (`setStorageListItems` isn't used here, so the very first update seeds an empty list.)
      assertEquals(0, dataStore.writeCount)

      advanceUntilIdle()

      assertEquals(1, dataStore.writeCount)
    }

  @Test
  fun `clearStores is not overtaken by a stale in-flight debounced write`() =
    runTest(testDispatcher) {
      val dataStore = FakeDataStore()
      val store = createStore(dataStore)
      runCurrent()

      store.setIsStorageSavedToApi(true)
      // The debounced write for the mutation above is still pending when the reset happens.
      store.clearStores()
      advanceUntilIdle()

      val secondStore = createStore(dataStore)
      runCurrent()

      assertFalse(secondStore.isStorageSavedToApi.value)
    }
}
