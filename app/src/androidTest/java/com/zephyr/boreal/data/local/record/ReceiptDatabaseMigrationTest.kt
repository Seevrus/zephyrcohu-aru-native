package com.zephyr.boreal.data.local.record

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.zephyr.boreal.domain.model.InvoiceType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * `ReceiptDatabase` is brand new (no installed apps have ever run version 1), so there is no
 * prior schema to migrate *from* here — unlike a typical Room migration test. Its job is to prove
 * the `exportSchema = true` + [MigrationTestHelper] machinery this task wires up for the first
 * time in this project actually works end to end: a rich row survives a real SQLite round trip
 * validated against the exported v1 schema. The *next* real schema change gets a genuine
 * `Migration` and this test becomes its template, not a stand-in for one today.
 */
@RunWith(AndroidJUnit4::class)
class ReceiptDatabaseMigrationTest {
  companion object {
    private const val TEST_DB = "receipt-migration-test"
  }

  @get:Rule
  val helper: MigrationTestHelper =
    MigrationTestHelper(
      InstrumentationRegistry.getInstrumentation(),
      ReceiptDatabase::class.java,
    )

  private val vendor =
    PersistedReceiptVendor(
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
    PersistedReceiptBuyer(
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

  private val item =
    PersistedReceiptItem(
      id = 1,
      articleNumber = "ART-1",
      name = "Item 1",
      quantity = 2.0,
      unitName = "db",
      netPrice = 500.0,
      netAmount = 1000.0,
      vatRate = "27",
      vatAmount = 270.0,
      grossAmount = 1270.0,
      discountName = "Fix kedvezmény",
      cnCode = "CN001",
      expiresAt = "2026-12-31",
    )

  private val otherItem =
    PersistedReceiptOtherItem(
      id = 2,
      articleNumber = "OTH-1",
      name = "Other item 1",
      quantity = 1.0,
      unitName = "db",
      netPrice = 500.0,
      netAmount = 500.0,
      vatRate = "27",
      vatAmount = 135.0,
      grossAmount = 635.0,
      comment = "Some comment",
    )

  private val vatAmount =
    PersistedReceiptVatAmount(
      vatRate = "27",
      netAmount = 1500.0,
      vatAmount = 405.0,
      grossAmount = 1905.0,
    )

  private val richEntity =
    ReceiptEntity(
      id = 1,
      companyId = 1,
      companyCode = "BOR",
      partnerId = 7,
      partnerCode = "P007",
      partnerSiteCode = "S001",
      serialNumber = 101,
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
      items = listOf(item),
      otherItems = listOf(otherItem),
      quantity = 2.0,
      netAmount = 1500.0,
      vatAmount = 405.0,
      grossAmount = 1905.0,
      vatAmounts = listOf(vatAmount),
      roundAmount = 0.5,
      roundedAmount = 1905.5,
      lastDownloadedAt = "2026-07-01T10:00:00Z",
      createdAt = "2026-07-01T08:00:00Z",
      updatedAt = "2026-07-02T09:00:00Z",
      isPrinted = true,
    )

  @Test
  fun aRichRowSurvivesTheExportedV1SchemaThroughARealSqliteRoundTrip() =
    runBlocking {
      // Create the database at the exported v1 schema, then close the raw connection so Room can
      // open it normally.
      helper.createDatabase(TEST_DB, 1).close()

      val db =
        Room
          .databaseBuilder(ApplicationProvider.getApplicationContext(), ReceiptDatabase::class.java, TEST_DB)
          .build()
      db.receiptDao().insertReceipt(richEntity)
      db.close()

      // Validates the live schema against the exported v1 schema — this is the assertion that
      // would fail if the entity/converters drifted from what MigrationTestHelper expects.
      helper.runMigrationsAndValidate(TEST_DB, 1, true)

      val reopened =
        Room
          .databaseBuilder(ApplicationProvider.getApplicationContext(), ReceiptDatabase::class.java, TEST_DB)
          .build()
      val persisted = reopened.receiptDao().findBySerialNumber(101).first()
      reopened.close()

      assertEquals(richEntity, persisted)
      assertNull(persisted?.cancelSerialNumber)
      assertEquals(true, persisted?.isPrinted)
    }
}
