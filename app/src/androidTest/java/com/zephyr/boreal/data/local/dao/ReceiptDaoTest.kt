package com.zephyr.boreal.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.zephyr.boreal.api.dto.response.ReceiptBuyerDto
import com.zephyr.boreal.api.dto.response.ReceiptVendorDto
import com.zephyr.boreal.data.local.BorealDatabase
import com.zephyr.boreal.data.local.ReceiptEntity
import com.zephyr.boreal.domain.model.InvoiceType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReceiptDaoTest {
  private lateinit var database: BorealDatabase
  private lateinit var dao: ReceiptDao

  @Before
  fun setUp() {
    database =
      Room
        .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), BorealDatabase::class.java)
        .build()
    dao = database.receiptDao()
  }

  @After
  fun tearDown() {
    database.close()
  }

  private val vendor =
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

  private val buyer =
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

  private fun buildEntity(
    id: Int,
    serialNumber: Int = 100 + id,
    cancelSerialNumber: Int? = null,
  ) = ReceiptEntity(
    id = id,
    companyId = 1,
    companyCode = "BOR",
    partnerId = 7,
    partnerCode = "P007",
    partnerSiteCode = "S001",
    serialNumber = serialNumber,
    yearCode = 2026,
    cancelSerialNumber = cancelSerialNumber,
    cancelYearCode = null,
    vendor = vendor,
    buyer = buyer,
    invoiceDate = "2026-07-01",
    fulfillmentDate = "2026-07-09",
    invoiceType = InvoiceType.PAPER,
    paidDate = "2026-07-09",
    user = null,
    items = emptyList(),
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
  fun getAllReceiptsReturnsRowsNewestFirstById() =
    runBlocking {
      dao.insertReceipt(buildEntity(id = 1))
      dao.insertReceipt(buildEntity(id = 3))
      dao.insertReceipt(buildEntity(id = 2))

      val result = dao.getAllReceipts().first()

      assertEquals(listOf(3, 2, 1), result.map { it.id })
    }

  @Test
  fun findBySerialNumberReturnsTheMatchingReceipt() =
    runBlocking {
      dao.insertReceipt(buildEntity(id = 1, serialNumber = 101))
      dao.insertReceipt(buildEntity(id = 2, serialNumber = 102))

      assertEquals(102, dao.findBySerialNumber(102).first()?.serialNumber)
    }

  @Test
  fun findBySerialNumberReturnsNullWhenNoReceiptMatches() =
    runBlocking {
      dao.insertReceipt(buildEntity(id = 1, serialNumber = 101))

      assertNull(dao.findBySerialNumber(999).first())
    }

  @Test
  fun getMaxSerialNumberReturnsNullOnAnEmptyTable() =
    runBlocking {
      assertNull(dao.getMaxSerialNumber())
    }

  @Test
  fun getMaxSerialNumberReturnsTheMaxAcrossSerialAndCancelSerialNumber() =
    runBlocking {
      dao.insertReceipt(buildEntity(id = 1, serialNumber = 101, cancelSerialNumber = null))
      dao.insertReceipt(buildEntity(id = 2, serialNumber = 102, cancelSerialNumber = 150))

      assertEquals(150, dao.getMaxSerialNumber())
    }
}
