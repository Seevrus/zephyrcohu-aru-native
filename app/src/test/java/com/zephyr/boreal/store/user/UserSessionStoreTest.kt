package com.zephyr.boreal.store.user

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UserSessionStoreTest {
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

    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
      val updated = transform(state.value)
      state.value = updated
      return updated
    }
  }

  private fun createStore(dataStore: FakeDataStore = FakeDataStore()) = UserSessionStore(dataStore, testScope)

  @Test
  fun `isPendingResetAfterPrint defaults to false`() =
    runTest(testDispatcher) {
      val store = createStore()

      assertFalse(store.isPendingResetAfterPrint.first())
    }

  @Test
  fun `setPendingResetAfterPrint persists true`() =
    runTest(testDispatcher) {
      val store = createStore()

      store.setPendingResetAfterPrint(true)

      assertTrue(store.isPendingResetAfterPrint.first())
    }

  @Test
  fun `setPendingResetAfterPrint false clears a previously set flag`() =
    runTest(testDispatcher) {
      val store = createStore()
      store.setPendingResetAfterPrint(true)

      store.setPendingResetAfterPrint(false)

      assertFalse(store.isPendingResetAfterPrint.first())
    }

  @Test
  fun `clearSession also clears the pending reset flag`() =
    runTest(testDispatcher) {
      val store = createStore()
      store.setPendingResetAfterPrint(true)

      store.clearSession()

      assertFalse(store.isPendingResetAfterPrint.first())
    }
}
