package com.zephyr.boreal.domain.utils

import com.zephyr.boreal.domain.model.Company
import com.zephyr.boreal.domain.model.InvoiceType
import com.zephyr.boreal.domain.model.Partner
import com.zephyr.boreal.domain.model.Receipt
import com.zephyr.boreal.domain.model.ReceiptBuyer
import com.zephyr.boreal.domain.model.ReceiptItem
import com.zephyr.boreal.domain.model.ReceiptOtherItem
import com.zephyr.boreal.domain.model.ReceiptVatAmount
import com.zephyr.boreal.domain.model.ReceiptVendor
import com.zephyr.boreal.domain.model.User
import com.zephyr.boreal.domain.model.UserState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PrintReceiptTextBuilderTest {
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
      phoneNumber = "+36301234567",
    )

  private val user =
    User(
      id = 1,
      userName = "teszt",
      state = UserState.ON_ROUND,
      name = "Teszt Elek",
      phoneNumber = null,
      isDev = false,
      roles = emptyList(),
      storeInUseId = null,
      storeOwnedId = null,
      lastActive = "2026-07-01T00:00:00Z",
      createdAt = "2026-07-01T00:00:00Z",
      updatedAt = "2026-07-01T00:00:00Z",
      company = company,
      lastRound = null,
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

  private val buyer =
    ReceiptBuyer(
      id = 7,
      name = "Partner Ltd.",
      country = "HU",
      postalCode = "2000",
      city = "Szentendre",
      address = "Kossuth utca 2.",
      deliveryName = "Partner Ltd. bolt",
      deliveryCountry = "HU",
      deliveryPostalCode = "2000",
      deliveryCity = "Szentendre",
      deliveryAddress = "Kossuth utca 4.",
      iban = null,
      bankAccount = null,
      vatNumber = "87654321-1-13",
    )

  private fun buildItem(
    vatRate: String = "27",
    discountName: String? = null,
  ) = ReceiptItem(
    id = 1,
    articleNumber = "ART-1",
    name = "Alma",
    quantity = 2.0,
    unitName = "db",
    netPrice = 500.0,
    netAmount = 1000.0,
    vatRate = vatRate,
    vatAmount = 270.0,
    grossAmount = 1270.0,
    discountName = discountName,
    cnCode = "080810",
    expiresAt = "261231",
  )

  private fun buildReceipt(
    isPrinted: Boolean = false,
    isCancel: Boolean = false,
    items: List<ReceiptItem> = listOf(buildItem()),
    otherItems: List<ReceiptOtherItem> = emptyList(),
    vatAmounts: List<ReceiptVatAmount> = listOf(ReceiptVatAmount("27", 1000.0, 270.0, 1270.0)),
  ) = Receipt(
    id = 1,
    companyId = 1,
    companyCode = "BOR",
    partnerId = 7,
    partnerCode = "P007",
    partnerSiteCode = "S001",
    serialNumber = 42,
    yearCode = 26,
    cancelSerialNumber = if (isCancel) 43 else null,
    cancelYearCode = if (isCancel) 26 else null,
    vendor = vendor,
    buyer = buyer,
    invoiceDate = "2026-07-01",
    fulfillmentDate = "2026-07-01",
    invoiceType = InvoiceType.PAPER,
    paidDate = "2026-07-01",
    user = null,
    items = items,
    otherItems = otherItems,
    quantity = 2.0,
    netAmount = 1000.0,
    vatAmount = 270.0,
    grossAmount = 1270.0,
    vatAmounts = vatAmounts,
    roundAmount = 0.0,
    roundedAmount = 1270.0,
    lastDownloadedAt = null,
    createdAt = "2026-07-01T00:00:00Z",
    updatedAt = "2026-07-01T00:00:00Z",
    isPrinted = isPrinted,
  )

  private fun buildPartner(
    invoiceType: InvoiceType = InvoiceType.PAPER,
    invoiceCopies: Int = 2,
    paymentDays: Int = 0,
  ) = Partner(
    id = 7,
    code = "P007",
    siteCode = "S001",
    name = "Partner Ltd.",
    invoiceType = invoiceType,
    invoiceCopies = invoiceCopies,
    paymentDays = paymentDays,
    createdAt = "2026-01-01T00:00:00Z",
    updatedAt = "2026-01-01T00:00:00Z",
  )

  @Test
  fun `copyCount defaults to 3 for an ad-hoc receipt with no partner`() {
    assertEquals(3, PrintReceiptTextBuilder.copyCount(null, buildReceipt()))
  }

  @Test
  fun `copyCount is 2 for electronic invoice partners`() {
    val partner = buildPartner(invoiceType = InvoiceType.ELECTRONIC, invoiceCopies = 5)
    assertEquals(2, PrintReceiptTextBuilder.copyCount(partner, buildReceipt()))
  }

  @Test
  fun `copyCount is 2 once the receipt was already printed, regardless of partner invoiceCopies`() {
    val partner = buildPartner(invoiceType = InvoiceType.PAPER, invoiceCopies = 5)
    assertEquals(2, PrintReceiptTextBuilder.copyCount(partner, buildReceipt(isPrinted = true)))
  }

  @Test
  fun `copyCount falls back to the partner invoiceCopies for a fresh paper invoice`() {
    val partner = buildPartner(invoiceType = InvoiceType.PAPER, invoiceCopies = 4)
    assertEquals(4, PrintReceiptTextBuilder.copyCount(partner, buildReceipt(isPrinted = false)))
  }

  @Test
  fun `receiptFormatLabel is elektronikus for electronic partners, papir alapu otherwise`() {
    assertEquals("elektronikus", PrintReceiptTextBuilder.receiptFormatLabel(buildPartner(InvoiceType.ELECTRONIC)))
    assertEquals("papír alapú", PrintReceiptTextBuilder.receiptFormatLabel(buildPartner(InvoiceType.PAPER)))
    assertEquals("papír alapú", PrintReceiptTextBuilder.receiptFormatLabel(null))
  }

  @Test
  fun `build renders the correct number of copy blocks separated by dashed dividers`() {
    val partner = buildPartner(invoiceCopies = 3)
    val text = PrintReceiptTextBuilder.build(isCancel = false, user = user, receipt = buildReceipt(), partner = partner)

    // Each of the 3 copies renders its own "SZÁMLA" header.
    assertEquals(3, Regex("SZÁMLA\n------").findAll(text).count())
  }

  @Test
  fun `build labels the first copy as Eredeti and later prints as Masolat once isPrinted is true`() {
    val partner = buildPartner(invoiceCopies = 2, invoiceType = InvoiceType.PAPER)
    val fresh = PrintReceiptTextBuilder.build(false, user, buildReceipt(isPrinted = false), partner)
    val reprint = PrintReceiptTextBuilder.build(false, user, buildReceipt(isPrinted = true), partner)

    assertTrue(fresh.contains("Eredeti: 1./2 példány"))
    assertTrue(reprint.contains("Másolat"))
    assertFalse(reprint.contains("Eredeti:"))
  }

  @Test
  fun `build renders the STORNO title and negated amounts for cancel receipts`() {
    val partner = buildPartner()
    val text =
      PrintReceiptTextBuilder.build(
        isCancel = true,
        user = user,
        receipt = buildReceipt(isCancel = true),
        partner = partner,
      )

    assertTrue(text.contains("STORNO SZÁMLA"))
    // Title references the ORIGINAL invoice being cancelled (42/26); the storno document's own
    // number (43/26) is the one shown on the header line right below the title.
    assertTrue(text.contains("(Stornozott számla száma: 42/26)"))
    assertTrue(text.contains("43/26"))
    assertTrue(text.contains("(visszajáró)"))
    assertFalse(text.contains("(fizetendő)"))
  }

  @Test
  fun `build has exactly one blank line before the item-table divider when delivery data is present`() {
    val text = PrintReceiptTextBuilder.build(false, user, buildReceipt(), buildPartner())

    assertEquals(1, blankLinesBeforeFirstDivider(text))
  }

  @Test
  fun `build has exactly two blank lines before the item-table divider when delivery data is empty`() {
    val receiptWithoutDelivery = buildReceipt().copy(buyer = buyer.copy(deliveryName = ""))
    val text = PrintReceiptTextBuilder.build(false, user, receiptWithoutDelivery, buildPartner())

    assertEquals(2, blankLinesBeforeFirstDivider(text))
  }

  private fun blankLinesBeforeFirstDivider(text: String): Int {
    val dividerIndex = text.indexOf("=".repeat(67))
    val before = text.substring(0, dividerIndex)
    val trailingNewlines = before.length - before.trimEnd('\n').length
    return trailingNewlines - 1
  }

  @Test
  fun `build shows the rounding line for ad-hoc partners but not for wire-transfer partners`() {
    val cashReceipt = buildReceipt()
    val adhocText = PrintReceiptTextBuilder.build(false, user, cashReceipt, null)
    val wireTransferText = PrintReceiptTextBuilder.build(false, user, cashReceipt, buildPartner(paymentDays = 30))

    assertTrue(adhocText.contains("Kerekítés (5 forintra) összege"))
    assertFalse(wireTransferText.contains("Kerekítés (5 forintra) összege"))
  }

  @Test
  fun `build renders a discount comment line and the item name`() {
    val discountedItem = buildItem(discountName = "Akció -10%")
    val text = PrintReceiptTextBuilder.build(false, user, buildReceipt(items = listOf(discountedItem)), buildPartner())

    assertTrue(text.contains("Alma"))
    assertTrue(text.contains("Megjegyzés: Akció -10%"))
  }

  @Test
  fun `build renders other items alongside regular items`() {
    val otherItem =
      ReceiptOtherItem(
        id = 1,
        articleNumber = "OTHER-1",
        name = "Egyedi tétel",
        quantity = 1.0,
        unitName = "db",
        netPrice = 300.0,
        netAmount = 300.0,
        vatRate = "27",
        vatAmount = 81.0,
        grossAmount = 381.0,
        comment = "Megjegyzés szöveg",
      )
    val text =
      PrintReceiptTextBuilder.build(
        false,
        user,
        buildReceipt(otherItems = listOf(otherItem)),
        buildPartner(),
      )

    assertTrue(text.contains("Egyedi tétel"))
  }

  @Test
  fun `build never exceeds the 67 character row width contract for fixed-width lines`() {
    val text = PrintReceiptTextBuilder.build(false, user, buildReceipt(), buildPartner())
    val dashLine = "-".repeat(67)
    val equalsLine = "=".repeat(67)

    assertTrue(text.contains(dashLine))
    assertTrue(text.contains(equalsLine))
    text.lines().forEach { line ->
      assertTrue(line.length <= 71, "Line unexpectedly long (${line.length} chars): \"$line\"")
    }
  }
}
