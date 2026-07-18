package com.zephyr.boreal.data.mapper

import com.zephyr.boreal.domain.model.DiscountType
import com.zephyr.boreal.domain.model.DraftReceiptItem
import com.zephyr.boreal.domain.model.InvoiceType
import com.zephyr.boreal.domain.model.Receipt
import com.zephyr.boreal.domain.model.ReceiptBuyer
import com.zephyr.boreal.domain.model.ReceiptItem
import com.zephyr.boreal.domain.model.ReceiptOtherItem
import com.zephyr.boreal.domain.model.ReceiptUser
import com.zephyr.boreal.domain.model.ReceiptVatAmount
import com.zephyr.boreal.domain.model.ReceiptVendor
import com.zephyr.boreal.domain.model.SelectedDiscount
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ReceiptMapperTest {
  private fun buildItem(
    quantity: Double = 5.0,
    netPrice: Double = 1000.0,
    selectedDiscounts: List<SelectedDiscount> = emptyList(),
  ) = DraftReceiptItem(
    id = 1,
    articleNumber = "ART-1",
    name = "Item 1",
    quantity = quantity,
    unitName = "db",
    netPrice = netPrice,
    netAmount = netPrice * quantity,
    vatRate = "27",
    vatAmount = 0.0,
    grossAmount = 0.0,
    discountName = null,
    expirationId = 1,
    cnCode = "CN001",
    expiresAt = "202607",
    selectedDiscounts = selectedDiscounts,
  )

  @Test
  fun `toDtos returns a single row identical to toDto when there are no discounts`() {
    val item = buildItem()

    val rows = item.toDtos()

    assertEquals(1, rows.size)
    assertEquals(item.toDto(), rows.first())
  }

  @Test
  fun `toDtos returns a single discounted row with no remainder when the discount covers the full quantity`() {
    val discount =
      SelectedDiscount(id = 1, name = "Fix kedvezmény", type = DiscountType.ABSOLUTE, quantity = 5.0, amount = 200.0)
    val item = buildItem(quantity = 5.0, netPrice = 1000.0, selectedDiscounts = listOf(discount))

    val rows = item.toDtos()

    assertEquals(1, rows.size)
    val row = rows.first()
    assertEquals(5.0, row.quantity)
    assertEquals(800.0, row.netPrice)
    assertEquals("Fix kedvezmény", row.discountName)
  }

  @Test
  fun `toDtos returns a discounted row plus a remainder row with no discountName for a partial discount`() {
    val discount =
      SelectedDiscount(id = 1, name = "Fix kedvezmény", type = DiscountType.ABSOLUTE, quantity = 2.0, amount = 200.0)
    val item = buildItem(quantity = 5.0, netPrice = 1000.0, selectedDiscounts = listOf(discount))

    val rows = item.toDtos()

    assertEquals(2, rows.size)
    val discountedRow = rows[0]
    assertEquals(2.0, discountedRow.quantity)
    assertEquals(800.0, discountedRow.netPrice)
    assertEquals("Fix kedvezmény", discountedRow.discountName)

    val remainderRow = rows[1]
    assertEquals(3.0, remainderRow.quantity)
    assertEquals(1000.0, remainderRow.netPrice)
    assertNull(remainderRow.discountName)
  }

  @Test
  fun `toDtos returns four rows for all three discount types plus a remainder`() {
    val absolute = SelectedDiscount(id = 1, name = "Fix", type = DiscountType.ABSOLUTE, quantity = 1.0, amount = 100.0)
    val percentage =
      SelectedDiscount(id = 2, name = "Százalékos", type = DiscountType.PERCENTAGE, quantity = 1.0, amount = 10.0)
    val freeForm =
      SelectedDiscount(id = 3, name = "Egyedi", type = DiscountType.FREE_FORM, quantity = 1.0, price = 500.0)
    val item = buildItem(quantity = 5.0, netPrice = 1000.0, selectedDiscounts = listOf(absolute, percentage, freeForm))

    val rows = item.toDtos()

    assertEquals(4, rows.size)
    assertEquals(listOf("Fix", "Százalékos", "Egyedi", null), rows.map { it.discountName })
    assertEquals(listOf(1.0, 1.0, 1.0, 2.0), rows.map { it.quantity })
  }

  @Suppress("LongMethod")
  private fun buildReceipt(): Receipt =
    Receipt(
      id = 42,
      companyId = 7,
      companyCode = "COMP-7",
      partnerId = 99,
      partnerCode = "PART-99",
      partnerSiteCode = "SITE-1",
      serialNumber = 123,
      yearCode = 2026,
      cancelSerialNumber = 45,
      cancelYearCode = 2025,
      vendor =
        ReceiptVendor(
          name = "Vendor Kft.",
          country = "HU",
          postalCode = "1011",
          city = "Budapest",
          address = "Fő utca 1.",
          felir = "FELIR-1",
          iban = "HU00000000000000000000",
          bankAccount = "10000000-00000000",
          vatNumber = "12345678-2-41",
        ),
      buyer =
        ReceiptBuyer(
          id = 5,
          name = "Buyer Kft.",
          country = "HU",
          postalCode = "1012",
          city = "Budapest",
          address = "Mellék utca 2.",
          deliveryName = "Buyer Delivery",
          deliveryCountry = "HU",
          deliveryPostalCode = "1013",
          deliveryCity = "Szentendre",
          deliveryAddress = "Kert utca 3.",
          iban = "HU11111111111111111111",
          bankAccount = "20000000-00000000",
          vatNumber = "87654321-2-41",
        ),
      invoiceDate = "2026-07-18",
      fulfillmentDate = "2026-07-19",
      invoiceType = InvoiceType.ELECTRONIC,
      paidDate = "2026-07-20",
      user =
        ReceiptUser(
          id = 3,
          userName = "jdoe",
          name = "John Doe",
          phoneNumber = "+36301234567",
        ),
      items =
        listOf(
          ReceiptItem(
            id = 1,
            articleNumber = "ART-1",
            name = "Item 1",
            quantity = 5.0,
            unitName = "db",
            netPrice = 1000.0,
            netAmount = 5000.0,
            vatRate = "27",
            vatAmount = 1350.0,
            grossAmount = 6350.0,
            discountName = "Fix kedvezmény",
            cnCode = "CN001",
            expiresAt = "202607",
          ),
        ),
      otherItems =
        listOf(
          ReceiptOtherItem(
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
          ),
        ),
      quantity = 6.0,
      netAmount = 5500.0,
      vatAmount = 1485.0,
      grossAmount = 6985.0,
      vatAmounts =
        listOf(
          ReceiptVatAmount(
            vatRate = "27",
            netAmount = 5500.0,
            vatAmount = 1485.0,
            grossAmount = 6985.0,
          ),
        ),
      roundAmount = 0.5,
      roundedAmount = 6985.5,
      lastDownloadedAt = "2026-07-18T10:00:00Z",
      createdAt = "2026-07-01T08:00:00Z",
      updatedAt = "2026-07-02T09:00:00Z",
    )

  @Test
  fun `Receipt round-trips through toEntity and toDomain unchanged`() {
    val receipt = buildReceipt()

    val roundTripped = receipt.toEntity().toDomain()

    assertEquals(receipt, roundTripped)
  }
}
