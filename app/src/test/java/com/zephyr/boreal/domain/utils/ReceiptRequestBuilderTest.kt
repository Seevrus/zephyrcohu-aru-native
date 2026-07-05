package com.zephyr.boreal.domain.utils

import com.zephyr.boreal.domain.model.Company
import com.zephyr.boreal.domain.model.DraftReceipt
import com.zephyr.boreal.domain.model.DraftReceiptItem
import com.zephyr.boreal.domain.model.InvoiceType
import com.zephyr.boreal.domain.model.ReceiptBuyer
import com.zephyr.boreal.domain.model.ReceiptOtherItem
import com.zephyr.boreal.domain.model.ReceiptVatAmount
import com.zephyr.boreal.domain.model.StoreDetails
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ReceiptRequestBuilderTest {
  private val company =
    Company(
      id = 1,
      name = "Boreal Kft.",
      code = "BOR",
      country = "HU",
      postalCode = "1000",
      city = "Budapest",
      address = "Fő utca 1.",
      felir = "FELIR1",
      vatNumber = "12345678-1-42",
      iban = "HU00",
      bankAccount = "11111111",
    )

  private val store =
    StoreDetails(
      id = 5,
      name = "Store 5",
      code = "S5",
      type = "VAN",
      state = "ACTIVE",
      firstAvailableSerialNumber = 100,
      lastAvailableSerialNumber = 999,
      yearCode = 2026,
      createdAt = "2026-01-01T00:00:00Z",
      updatedAt = "2026-01-01T00:00:00Z",
      expirations = emptyList(),
    )

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

  private val item =
    DraftReceiptItem(
      id = 1,
      articleNumber = "ART-1",
      name = "Item 1",
      quantity = 2.0,
      unitName = "db",
      netPrice = 100.0,
      netAmount = 200.0,
      vatRate = "27",
      vatAmount = 54.0,
      grossAmount = 254.0,
      discountName = null,
      expirationId = 3,
      cnCode = "CN001",
      expiresAt = "2026-12-31",
    )

  private val otherItem =
    ReceiptOtherItem(
      id = 10,
      articleNumber = "OTH-10",
      name = "Other 10",
      quantity = 1.0,
      unitName = "db",
      netPrice = 50.0,
      netAmount = 50.0,
      vatRate = "27",
      vatAmount = 13.5,
      grossAmount = 63.5,
      comment = "note",
    )

  private val draft =
    DraftReceipt(
      partnerId = 7,
      partnerCode = "P007",
      partnerSiteCode = "S001",
      buyer = buyer,
      paymentDays = 8,
      invoiceType = InvoiceType.PAPER,
      items = listOf(item),
      otherItems = listOf(otherItem),
    )

  private val totals =
    ReceiptTotals(
      quantity = 3.0,
      netAmount = 250.0,
      vatAmount = 67.5,
      grossAmount = 317.5,
      vatAmounts = listOf(ReceiptVatAmount(vatRate = "27", netAmount = 250.0, vatAmount = 67.5, grossAmount = 317.5)),
    )

  @Test
  fun `buildCreateReceiptRequest maps draft company store and totals into the request dto`() {
    val request =
      buildCreateReceiptRequest(
        draft = draft,
        company = company,
        store = store,
        serialNumber = 101,
        totals = totals,
        dates = ReceiptDates(invoiceDate = "2026-07-01", fulfillmentDate = "2026-07-09", paidDate = "2026-07-09"),
        rounding = AmountRounding(roundAmount = 2.5, roundedAmount = 320.0),
      )

    assertEquals(7, request.partnerId)
    assertEquals("P007", request.partnerCode)
    assertEquals("S001", request.partnerSiteCode)
    assertEquals(101, request.serialNumber)
    assertEquals(2026, request.yearCode)
    assertEquals(InvoiceType.PAPER, request.invoiceType)
    assertEquals(8, request.paymentDays)
    assertEquals("2026-07-01", request.invoiceDate)
    assertEquals("2026-07-09", request.fulfillmentDate)
    assertEquals("2026-07-09", request.paidDate)

    assertEquals("Boreal Kft.", request.vendor.name)
    assertEquals(company.vatNumber, request.vendor.vatNumber)

    assertEquals(7, request.buyer.id)
    assertEquals("Partner Ltd.", request.buyer.name)

    assertEquals(1, request.items.size)
    assertEquals("ART-1", request.items.first().articleNumber)
    assertEquals(3, request.items.first().expirationId)
    assertEquals(1, request.otherItems?.size)
    assertEquals("note", request.otherItems?.first()?.comment)

    assertEquals(3.0, request.quantity)
    assertEquals(250.0, request.netAmount)
    assertEquals(67.5, request.vatAmount)
    assertEquals(317.5, request.grossAmount)
    assertEquals(1, request.vatAmounts.size)
    assertEquals(2.5, request.roundAmount)
    assertEquals(320.0, request.roundedAmount)
  }
}
