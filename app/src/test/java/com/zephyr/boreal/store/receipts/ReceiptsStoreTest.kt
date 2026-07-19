package com.zephyr.boreal.store.receipts

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.zephyr.boreal.api.dto.response.ReceiptBuyerDto
import com.zephyr.boreal.api.dto.response.ReceiptItemDto
import com.zephyr.boreal.api.dto.response.ReceiptVendorDto
import com.zephyr.boreal.data.local.ReceiptEntity
import com.zephyr.boreal.data.local.dao.ReceiptDao
import com.zephyr.boreal.data.mapper.toDomain
import com.zephyr.boreal.data.mapper.toEntity
import com.zephyr.boreal.domain.model.DraftOrder
import com.zephyr.boreal.domain.model.DraftReceipt
import com.zephyr.boreal.domain.model.InvoiceType
import com.zephyr.boreal.domain.model.Receipt
import com.zephyr.boreal.domain.model.ReceiptBuyer
import com.zephyr.boreal.domain.model.ReceiptItem
import com.zephyr.boreal.domain.model.ReceiptVendor
import com.zephyr.boreal.domain.model.TempSelection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class ReceiptsStoreTest {
  @TempDir
  lateinit var tempDir: File

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

  private fun createTestDataStore(
    scope: CoroutineScope = testScope,
    fileName: String = "test_receipts_${System.nanoTime()}.preferences_pb",
  ): DataStore<Preferences> =
    PreferenceDataStoreFactory.create(
      scope = scope,
      produceFile = { File(tempDir, fileName) },
    )

  /**
   * `androidx.datastore:datastore-preferences:1.2.1`'s file-based storage fails with
   * "Unable to rename ... likely multiple instances of DataStore" on Windows whenever a
   * *second* real write targets an already-existing file (its rename-based atomic swap doesn't
   * overwrite on Windows, unlike POSIX `rename()` on the real Android runtime this code
   * actually ships on). Verified via an isolated repro with zero [ReceiptsStore] involvement —
   * not a bug in this store's concurrency handling. Tests that need more than one write against
   * the same persisted state use this in-memory fake instead of a real file-backed DataStore.
   */
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

  private val buyer =
    ReceiptBuyer(
      id = 7,
      name = "Partner Ltd.",
      country = "HU",
      postalCode = "2000",
      city = "Szentendre",
      address = "Kossuth utca 2.",
      deliveryName = "Partner Ltd.",
      deliveryCountry = "HU",
      deliveryPostalCode = "2000",
      deliveryCity = "Szentendre",
      deliveryAddress = "Kossuth utca 2.",
      iban = null,
      bankAccount = null,
      vatNumber = "87654321-1-13",
    )

  private val buyerDto =
    ReceiptBuyerDto(
      id = 7,
      name = "Partner Ltd.",
      country = "HU",
      postalCode = "2000",
      city = "Szentendre",
      address = "Kossuth utca 2.",
      deliveryName = "Partner Ltd.",
      deliveryCountry = "HU",
      deliveryPostalCode = "2000",
      deliveryCity = "Szentendre",
      deliveryAddress = "Kossuth utca 2.",
      iban = null,
      bankAccount = null,
      vatNumber = "87654321-1-13",
    )

  private val vendor =
    ReceiptVendor(
      name = "Boreal Kft.",
      country = "HU",
      postalCode = "1000",
      city = "Budapest",
      address = "Fő utca 1.",
      felir = "FELIR1",
      iban = "HU00",
      bankAccount = "11111111",
      vatNumber = "12345678-1-42",
    )

  private val vendorDto =
    ReceiptVendorDto(
      name = "Boreal Kft.",
      country = "HU",
      postalCode = "1000",
      city = "Budapest",
      address = "Fő utca 1.",
      felir = "FELIR1",
      iban = "HU00",
      bankAccount = "11111111",
      vatNumber = "12345678-1-42",
    )

  private fun buildReceiptItem(id: Int) =
    ReceiptItem(
      id = id,
      articleNumber = "ART-$id",
      name = "Item $id",
      quantity = 2.0,
      unitName = "db",
      netPrice = 500.0,
      netAmount = 1000.0,
      vatRate = "27%",
      vatAmount = 270.0,
      grossAmount = 1270.0,
      discountName = null,
      cnCode = "CN001",
      expiresAt = "2026-12-31",
    )

  private fun buildReceiptItemDto(id: Int) =
    ReceiptItemDto(
      id = id,
      articleNumber = "ART-$id",
      name = "Item $id",
      quantity = 2.0,
      unitName = "db",
      netPrice = 500.0,
      netAmount = 1000.0,
      vatRate = "27%",
      vatAmount = 270.0,
      grossAmount = 1270.0,
      discountName = null,
      cnCode = "CN001",
      expiresAt = "2026-12-31",
    )

  private fun buildReceipt(id: Int) =
    Receipt(
      id = id,
      companyId = 1,
      companyCode = "BOR",
      partnerId = 7,
      partnerCode = "P007",
      partnerSiteCode = "S001",
      serialNumber = 100 + id,
      yearCode = 2026,
      cancelSerialNumber = null,
      cancelYearCode = null,
      vendor = vendor,
      buyer = buyer,
      invoiceDate = "2026-07-01",
      fulfillmentDate = "2026-07-09",
      invoiceType = InvoiceType.PAPER,
      paidDate = "2026-07-09",
      user = null,
      items = listOf(buildReceiptItem(id)),
      otherItems = emptyList(),
      quantity = 2.0,
      netAmount = 1000.0,
      vatAmount = 270.0,
      grossAmount = 1270.0,
      vatAmounts = emptyList(),
      roundAmount = 0.0,
      roundedAmount = 1270.0,
      lastDownloadedAt = null,
      createdAt = "2026-07-01T00:00:00Z",
      updatedAt = "2026-07-01T00:00:00Z",
    )

  private fun buildReceiptEntity(id: Int) =
    ReceiptEntity(
      id = id,
      companyId = 1,
      companyCode = "BOR",
      partnerId = 7,
      partnerCode = "P007",
      partnerSiteCode = "S001",
      serialNumber = 100 + id,
      yearCode = 2026,
      cancelSerialNumber = null,
      cancelYearCode = null,
      vendor = vendorDto,
      buyer = buyerDto,
      invoiceDate = "2026-07-01",
      fulfillmentDate = "2026-07-09",
      invoiceType = InvoiceType.PAPER,
      paidDate = "2026-07-09",
      user = null,
      items = listOf(buildReceiptItemDto(id)),
      otherItems = emptyList(),
      quantity = 2.0,
      netAmount = 1000.0,
      vatAmount = 270.0,
      grossAmount = 1270.0,
      vatAmounts = emptyList(),
      roundAmount = 0.0,
      roundedAmount = 1270.0,
      lastDownloadedAt = null,
      createdAt = "2026-07-01T00:00:00Z",
      updatedAt = "2026-07-01T00:00:00Z",
    )

  @Test
  fun `receipts reflects receiptDao entities mapped to domain`() =
    runTest(testDispatcher) {
      val entity1 = buildReceiptEntity(1)
      val entity2 = buildReceiptEntity(2)
      val receiptDao: ReceiptDao = mock()
      whenever(receiptDao.getAllReceipts()).thenReturn(flowOf(listOf(entity1, entity2)))

      val store = ReceiptsStore(receiptDao, createTestDataStore(), testScope)
      runCurrent()

      assertEquals(listOf(entity1.toDomain(), entity2.toDomain()), store.receipts.value)
    }

  @Test
  fun `addReceipt inserts the receipt mapped to an entity`() =
    runTest(testDispatcher) {
      val receiptDao: ReceiptDao = mock()
      whenever(receiptDao.getAllReceipts()).thenReturn(flowOf(emptyList()))
      val store = ReceiptsStore(receiptDao, createTestDataStore(), testScope)
      runCurrent()

      val receipt = buildReceipt(id = 42)
      store.addReceipt(receipt)

      val captor = argumentCaptor<ReceiptEntity>()
      verify(receiptDao).insertReceipt(captor.capture())
      assertEquals(receipt.toEntity(), captor.firstValue)
    }

  @Test
  fun `resetReceipts clears receiptDao and the in-memory draft state`() =
    runTest(testDispatcher) {
      val receiptDao: ReceiptDao = mock()
      whenever(receiptDao.getAllReceipts()).thenReturn(flowOf(emptyList()))
      val store = ReceiptsStore(receiptDao, FakeDataStore(), testScope)
      runCurrent()

      store.setCurrentReceipt(DraftReceipt(partnerId = 7))
      store.upsertSelectedItem(itemId = 1, expirationId = 1, quantity = 2.0)
      store.upsertOrderItem(itemId = 1, quantity = 3.0)
      store.setCurrentOrder(DraftOrder(partnerId = 7, orderedAt = "2026-07-01 08:00:00", items = emptyList()))
      store.setOtherItemSelections(mapOf(1 to TempSelection(netPrice = 500.0, quantity = 2, comment = "note")))
      runCurrent()

      assertEquals(7, store.currentReceipt.value?.partnerId)
      assertTrue(store.selectedItems.value.isNotEmpty())
      assertTrue(store.selectedOrderItems.value.isNotEmpty())
      assertEquals(7, store.currentOrder.value?.partnerId)
      assertTrue(store.otherItemSelections.value.isNotEmpty())

      store.resetReceipts()
      runCurrent()

      verify(receiptDao).clearReceipts()
      assertNull(store.currentReceipt.value)
      assertTrue(store.selectedItems.value.isEmpty())
      assertTrue(store.selectedOrderItems.value.isEmpty())
      assertNull(store.currentOrder.value)
      assertTrue(store.otherItemSelections.value.isEmpty())
    }

  @Test
  fun `draft receipt, order, and selections survive a fresh store instance backed by the same DataStore`() =
    runTest(testDispatcher) {
      val receiptDao: ReceiptDao = mock()
      whenever(receiptDao.getAllReceipts()).thenReturn(flowOf(emptyList()))
      val sharedDataStore = FakeDataStore()

      val firstStore = ReceiptsStore(receiptDao, sharedDataStore, testScope)
      runCurrent()
      firstStore.setCurrentReceipt(DraftReceipt(partnerId = 9))
      firstStore.upsertSelectedItem(itemId = 1, expirationId = 1, quantity = 4.0)
      firstStore.upsertOrderItem(itemId = 2, quantity = 5.0)
      firstStore.setCurrentOrder(DraftOrder(partnerId = 9, orderedAt = "2026-07-01 08:00:00", items = emptyList()))
      firstStore.setOtherItemSelections(mapOf(3 to TempSelection(netPrice = 800.0, quantity = 1, comment = "note")))
      runCurrent()

      val secondStore = ReceiptsStore(receiptDao, sharedDataStore, testScope)
      runCurrent()

      assertEquals(9, secondStore.currentReceipt.value?.partnerId)
      assertEquals(mapOf(1 to mapOf(1 to 4.0)), secondStore.selectedItems.value)
      assertEquals(mapOf(2 to 5.0), secondStore.selectedOrderItems.value)
      assertEquals(9, secondStore.currentOrder.value?.partnerId)
      assertEquals(
        mapOf(3 to TempSelection(netPrice = 800.0, quantity = 1, comment = "note")),
        secondStore.otherItemSelections.value,
      )
    }

  @Test
  fun `resetReceipts clears the persisted draft, not just memory`() =
    runTest(testDispatcher) {
      val receiptDao: ReceiptDao = mock()
      whenever(receiptDao.getAllReceipts()).thenReturn(flowOf(emptyList()))
      val sharedDataStore = FakeDataStore()

      val firstStore = ReceiptsStore(receiptDao, sharedDataStore, testScope)
      runCurrent()
      firstStore.setCurrentReceipt(DraftReceipt(partnerId = 9))
      runCurrent()

      firstStore.resetReceipts()
      runCurrent()

      val secondStore = ReceiptsStore(receiptDao, sharedDataStore, testScope)
      runCurrent()

      assertNull(secondStore.currentReceipt.value)
    }

  @Test
  fun `a corrupt persisted blob falls back to an empty draft instead of crashing`() =
    runTest(testDispatcher) {
      val receiptDao: ReceiptDao = mock()
      whenever(receiptDao.getAllReceipts()).thenReturn(flowOf(emptyList()))
      val corruptDataStore = FakeDataStore()
      corruptDataStore.edit { it[stringPreferencesKey("draft_sell_flow_state")] = "not valid json" }

      val store = ReceiptsStore(receiptDao, corruptDataStore, testScope)
      runCurrent()

      assertNull(store.currentReceipt.value)
      assertTrue(store.selectedItems.value.isEmpty())
    }
}
