package com.zephyr.boreal.store.receipts

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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class ReceiptsStoreTest {
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
    runTest {
      val entity1 = buildReceiptEntity(1)
      val entity2 = buildReceiptEntity(2)
      val receiptDao: ReceiptDao = mock()
      whenever(receiptDao.getAllReceipts()).thenReturn(flowOf(listOf(entity1, entity2)))

      val store = ReceiptsStore(receiptDao, backgroundScope)
      runCurrent()

      assertEquals(listOf(entity1.toDomain(), entity2.toDomain()), store.receipts.value)
    }

  @Test
  fun `addReceipt inserts the receipt mapped to an entity`() =
    runTest {
      val receiptDao: ReceiptDao = mock()
      whenever(receiptDao.getAllReceipts()).thenReturn(flowOf(emptyList()))
      val store = ReceiptsStore(receiptDao, backgroundScope)
      runCurrent()

      val receipt = buildReceipt(id = 42)
      store.addReceipt(receipt)

      val captor = argumentCaptor<ReceiptEntity>()
      verify(receiptDao).insertReceipt(captor.capture())
      assertEquals(receipt.toEntity(), captor.firstValue)
    }

  @Test
  fun `resetReceipts clears receiptDao and the in-memory draft state`() =
    runTest {
      val receiptDao: ReceiptDao = mock()
      whenever(receiptDao.getAllReceipts()).thenReturn(flowOf(emptyList()))
      val store = ReceiptsStore(receiptDao, backgroundScope)
      runCurrent()

      store.setCurrentReceipt(DraftReceipt(partnerId = 7))
      store.upsertSelectedItem(itemId = 1, expirationId = 1, quantity = 2.0)
      store.upsertOrderItem(itemId = 1, quantity = 3.0)
      store.setCurrentOrder(DraftOrder(partnerId = 7, orderedAt = "2026-07-01 08:00:00", items = emptyList()))

      assertEquals(7, store.currentReceipt.value?.partnerId)
      assertTrue(store.selectedItems.value.isNotEmpty())
      assertTrue(store.selectedOrderItems.value.isNotEmpty())
      assertEquals(7, store.currentOrder.value?.partnerId)

      store.resetReceipts()

      verify(receiptDao).clearReceipts()
      assertNull(store.currentReceipt.value)
      assertTrue(store.selectedItems.value.isEmpty())
      assertTrue(store.selectedOrderItems.value.isEmpty())
      assertNull(store.currentOrder.value)
    }
}
