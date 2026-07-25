package com.zephyr.boreal.domain.utils

import com.zephyr.boreal.domain.model.InvoiceType
import com.zephyr.boreal.domain.model.Receipt
import com.zephyr.boreal.domain.model.ReceiptBuyer
import com.zephyr.boreal.domain.model.ReceiptUser
import com.zephyr.boreal.domain.model.ReceiptVendor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DevPrintFileNameBuilderTest {
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

  private fun buildBuyer(name: String) =
    ReceiptBuyer(
      id = 7,
      name = name,
      country = "HU",
      postalCode = "1000",
      city = "Budapest",
      address = "Kossuth utca 2.",
      deliveryName = "",
      deliveryCountry = "",
      deliveryPostalCode = "",
      deliveryCity = "",
      deliveryAddress = "",
      iban = null,
      bankAccount = null,
      vatNumber = null,
    )

  private fun buildReceipt(
    serialNumber: Int,
    buyerName: String,
    invoiceDate: String,
  ) = Receipt(
    id = serialNumber,
    companyId = 1,
    companyCode = "BOR",
    partnerId = 7,
    partnerCode = "P007",
    partnerSiteCode = "S1",
    serialNumber = serialNumber,
    yearCode = 2026,
    cancelSerialNumber = null,
    cancelYearCode = null,
    vendor = vendor,
    buyer = buildBuyer(buyerName),
    invoiceDate = invoiceDate,
    fulfillmentDate = invoiceDate,
    invoiceType = InvoiceType.PAPER,
    paidDate = invoiceDate,
    user = ReceiptUser(id = 1, userName = "teszt", name = "Teszt Elek", phoneNumber = "123"),
    quantity = 1.0,
    netAmount = 100.0,
    vatAmount = 27.0,
    grossAmount = 127.0,
    roundAmount = 0.0,
    roundedAmount = 127.0,
    lastDownloadedAt = null,
    createdAt = "2026-01-01T00:00:00Z",
    updatedAt = "2026-01-01T00:00:00Z",
  )

  @Test
  fun `build produces partnerName_invoiceDate_serialNumber txt`() {
    val receipt = buildReceipt(serialNumber = 42, buyerName = "Partner Ltd.", invoiceDate = "2026-01-15")

    assertEquals("Partner Ltd._2026-01-15_42.txt", DevPrintFileNameBuilder.build(receipt))
  }

  @Test
  fun `build sanitizes illegal filename characters from the buyer name`() {
    val receipt = buildReceipt(serialNumber = 1, buyerName = "Kft/Bt: \"Test\"", invoiceDate = "2026-01-01")

    assertEquals("Kft_Bt_ _Test__2026-01-01_1.txt", DevPrintFileNameBuilder.build(receipt))
  }

  @Test
  fun `build uses the ad-hoc buyer display name the same way the print body does`() {
    val receipt = buildReceipt(serialNumber = 5, buyerName = "Alkalmi Vevő", invoiceDate = "2026-02-02")

    assertEquals("Alkalmi Vevő_2026-02-02_5.txt", DevPrintFileNameBuilder.build(receipt))
  }

  @Test
  fun `a storno receipt uses its own distinct serial number producing a distinct filename`() {
    val original = buildReceipt(serialNumber = 10, buyerName = "Partner Ltd.", invoiceDate = "2026-03-03")
    val cancel = buildReceipt(serialNumber = 11, buyerName = "Partner Ltd.", invoiceDate = "2026-03-03")

    assertEquals("Partner Ltd._2026-03-03_10.txt", DevPrintFileNameBuilder.build(original))
    assertEquals("Partner Ltd._2026-03-03_11.txt", DevPrintFileNameBuilder.build(cancel))
  }
}
