package com.zephyr.boreal.data.mapper

import com.zephyr.boreal.domain.model.InvoiceType
import com.zephyr.boreal.domain.model.Receipt
import com.zephyr.boreal.domain.model.ReceiptBuyer
import com.zephyr.boreal.domain.model.ReceiptVendor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class RoundMapperTest {
  @Test
  @Suppress("LongMethod")
  fun `toRoundReceiptDto correctly maps domain model properties to DTO`() {
    val receipt =
      Receipt(
        id = 123,
        companyId = 1,
        companyCode = "CC",
        partnerId = 2,
        partnerCode = "PC",
        partnerSiteCode = "PSC",
        serialNumber = 42,
        yearCode = 2026,
        cancelSerialNumber = 43,
        cancelYearCode = 2026,
        originalCopiesPrinted = 1,
        vendor = ReceiptVendor("V", "HU", "1000", "Budapest", "A", "F", "I", "B", "VAT"),
        invoiceDate = "2026-05-30",
        fulfillmentDate = "2026-05-30",
        invoiceType = InvoiceType.PAPER,
        paidDate = "2026-05-30",
        user = null,
        buyer =
          ReceiptBuyer(
            id = 1,
            name = "Test Buyer",
            country = "HU",
            postalCode = "1000",
            city = "Budapest",
            address = "A",
            deliveryName = "DN",
            deliveryCountry = "DC",
            deliveryPostalCode = "DPC",
            deliveryCity = "DC",
            deliveryAddress = "DA",
            iban = null,
            bankAccount = null,
            vatNumber = "12345678-1-11",
          ),
        quantity = 5.0,
        netAmount = 1000.0,
        vatAmount = 270.0,
        grossAmount = 1270.0,
        roundAmount = 0.0,
        roundedAmount = 1270.0,
        lastDownloadedAt = null,
        createdAt = "",
        updatedAt = "",
      )

    val dto = receipt.toRoundReceiptDto()

    assertEquals(123, dto.id)
    assertEquals(42, dto.serialNumber)
    assertEquals(2026, dto.yearCode)
    assertEquals(43, dto.cancelSerialNumber)
    assertEquals(2026, dto.cancelYearCode)
    assertEquals(InvoiceType.PAPER, dto.invoiceType)
    assertEquals(0, dto.paymentDays) // Same day invoice/fulfillment -> 0 days
    assertEquals("Test Buyer", dto.buyer.name)
    assertEquals("12345678-1-11", dto.buyer.vatNumber)
    assertEquals(5.0, dto.quantity)
    assertEquals(1000.0, dto.netAmount)
    assertEquals(270.0, dto.vatAmount)
    assertEquals(1270.0, dto.grossAmount)
    assertEquals(0.0, dto.roundAmount)
    assertEquals(1270.0, dto.roundedAmount)
  }

  @Test
  fun `toRoundReceiptDto handles null cancellation fields correctly`() {
    val receipt =
      Receipt(
        id = 123,
        companyId = 1,
        companyCode = "CC",
        partnerId = 2,
        partnerCode = "PC",
        partnerSiteCode = "PSC",
        serialNumber = 42,
        yearCode = 2026,
        cancelSerialNumber = null,
        cancelYearCode = null,
        originalCopiesPrinted = 1,
        vendor = ReceiptVendor("V", "HU", "1000", "Budapest", "A", "F", "I", "B", "VAT"),
        invoiceDate = "2026-05-30",
        fulfillmentDate = "2026-05-30",
        invoiceType = InvoiceType.PAPER,
        paidDate = "2026-05-30",
        user = null,
        buyer =
          ReceiptBuyer(
            id = 1,
            name = "Test Buyer",
            country = "HU",
            postalCode = "1000",
            city = "Budapest",
            address = "A",
            deliveryName = "DN",
            deliveryCountry = "DC",
            deliveryPostalCode = "DPC",
            deliveryCity = "DC",
            deliveryAddress = "DA",
            iban = null,
            bankAccount = null,
            vatNumber = null,
          ),
        quantity = 5.0,
        netAmount = 100.0,
        vatAmount = 27.0,
        grossAmount = 127.0,
        roundAmount = 0.0,
        roundedAmount = 127.0,
        lastDownloadedAt = null,
        createdAt = "",
        updatedAt = "",
      )

    val dto = receipt.toRoundReceiptDto()

    assertNull(dto.cancelSerialNumber)
    assertNull(dto.cancelYearCode)
    assertNull(dto.buyer.vatNumber)
  }
}
