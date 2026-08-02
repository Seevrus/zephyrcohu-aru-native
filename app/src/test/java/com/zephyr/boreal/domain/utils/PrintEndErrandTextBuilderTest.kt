package com.zephyr.boreal.domain.utils

import com.zephyr.boreal.domain.model.InvoiceType
import com.zephyr.boreal.domain.model.Receipt
import com.zephyr.boreal.domain.model.ReceiptBuyer
import com.zephyr.boreal.domain.model.ReceiptVendor
import com.zephyr.boreal.domain.model.Round
import com.zephyr.boreal.domain.model.RoundPartnerList
import com.zephyr.boreal.domain.model.RoundStore
import com.zephyr.boreal.domain.model.RoundUser
import com.zephyr.boreal.domain.model.User
import com.zephyr.boreal.domain.model.UserState
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class PrintEndErrandTextBuilderTest {
  private val today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy.MM.dd."))
  private val todayRowFormat = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy.MM.dd"))

  private val user =
    User(
      id = 1,
      userName = "user1",
      state = UserState.ON_ROUND,
      name = "Kovács Béla",
      phoneNumber = null,
      isDev = false,
      roles = emptyList(),
      storeInUseId = 2,
      storeOwnedId = null,
      lastActive = "",
      createdAt = "",
      updatedAt = "",
      company = mock(),
      lastRound = null,
    )

  private val round =
    Round(
      id = 17,
      user = RoundUser(id = 1, userName = "user1", name = "Kovács Béla"),
      store = RoundStore(id = 2, code = "K1", name = "Kamion 1"),
      partnerList = RoundPartnerList(id = 3, name = "Északi kör"),
      yearCode = 2026,
      roundStarted = "2026-01-05",
      roundFinished = "2026-01-06",
      lastSerialNumber = 50,
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

  private fun buildBuyer(
    name: String = "Partner Ltd.",
    vatNumber: String? = "87654321-1-13",
  ) = ReceiptBuyer(
    id = 7,
    name = name,
    country = "HU",
    postalCode = "2000",
    city = "Szentendre",
    address = "Kossuth utca 2.",
    deliveryName = "",
    deliveryCountry = "",
    deliveryPostalCode = "",
    deliveryCity = "",
    deliveryAddress = "",
    iban = null,
    bankAccount = null,
    vatNumber = vatNumber,
  )

  private fun buildReceipt(
    serialNumber: Int,
    cancelSerialNumber: Int? = null,
    cancelYearCode: Int? = null,
    invoiceType: InvoiceType = InvoiceType.PAPER,
    buyerName: String = "Partner Ltd.",
    buyerVatNumber: String? = "87654321-1-13",
    quantity: Double = 1.0,
    roundedAmount: Double = 1000.0,
    isCash: Boolean = true,
  ) = Receipt(
    id = serialNumber,
    companyId = 1,
    companyCode = "BOR",
    partnerId = 7,
    partnerCode = "P007",
    partnerSiteCode = "S001",
    serialNumber = serialNumber,
    yearCode = 26,
    cancelSerialNumber = cancelSerialNumber,
    cancelYearCode = cancelYearCode,
    vendor = vendor,
    buyer = buildBuyer(buyerName, buyerVatNumber),
    invoiceDate = "2026-01-05",
    fulfillmentDate = if (isCash) "2026-01-05" else "2026-01-20",
    invoiceType = invoiceType,
    paidDate = "2026-01-05",
    user = null,
    quantity = quantity,
    netAmount = roundedAmount,
    vatAmount = 0.0,
    grossAmount = roundedAmount,
    roundAmount = 0.0,
    roundedAmount = roundedAmount,
    lastDownloadedAt = null,
    createdAt = "2026-01-05T00:00:00Z",
    updatedAt = "2026-01-05T00:00:00Z",
  )

  @Test
  fun `header block includes today, user, store, round and date range`() {
    val text = PrintEndErrandTextBuilder.build(emptyList(), round, user)

    assertTrue(text.contains("{b}KÖRELSZÁMOLÁS | $today{/b}"))
    assertTrue(text.contains("Üzletkötő: user1   Kovács Béla"))
    assertTrue(text.contains("Raktár   : K1      Kamion 1"))
    assertTrue(text.contains("Kör      : 17      Északi kör"))
    assertTrue(text.contains("Kördátum : 2026.01.05 .. 2026.01.06"))
  }

  @Test
  fun `header falls back to today when roundFinished is null`() {
    val ongoingRound = round.copy(roundFinished = null)

    val text = PrintEndErrandTextBuilder.build(emptyList(), ongoingRound, user)

    assertTrue(text.contains("Kördátum : 2026.01.05 .. $todayRowFormat"))
  }

  @Test
  fun `a normal paper receipt renders a K row with the vat number and buyer name`() {
    val receipt = buildReceipt(serialNumber = 42, buyerVatNumber = "87654321-1-13")

    val text = PrintEndErrandTextBuilder.build(listOf(receipt), round, user)

    assertTrue(text.contains("         42 K 87654321-1-13 Partner Ltd.                       1000"))
  }

  @Test
  fun `an electronic receipt renders an A row and a missing vat number renders a dash`() {
    val receipt = buildReceipt(serialNumber = 5, invoiceType = InvoiceType.ELECTRONIC, buyerVatNumber = null)

    val text = PrintEndErrandTextBuilder.build(listOf(receipt), round, user)

    assertTrue(text.contains("          5 Á -             Partner Ltd.                       1000"))
  }

  @Test
  fun `a canceled receipt renders two rows, the cancel row negated and signed`() {
    val receipt = buildReceipt(serialNumber = 10, cancelSerialNumber = 11, cancelYearCode = 26, roundedAmount = 500.0)

    val text = PrintEndErrandTextBuilder.build(listOf(receipt), round, user)

    assertTrue(text.contains("         10 K 87654321-1-13 Partner Ltd.                        500"))
    assertTrue(text.contains("         11 K 87654321-1-13 Partner Ltd.                       -500"))
  }

  @Test
  fun `rows sort numerically by serial number, not lexicographically`() {
    val ten = buildReceipt(serialNumber = 10)
    val two = buildReceipt(serialNumber = 2)

    val text = PrintEndErrandTextBuilder.build(listOf(ten, two), round, user)

    val indexOfTwo = text.indexOf("          2 K")
    val indexOfTen = text.indexOf("         10 K")
    assertTrue(indexOfTwo in 0..<indexOfTen)
  }

  @Test
  fun `an over-long buyer name is truncated with an ellipsis`() {
    val receipt = buildReceipt(serialNumber = 1, buyerName = "Nagyon Hosszú Partner Cégnév Kft. Bt.")

    val text = PrintEndErrandTextBuilder.build(listOf(receipt), round, user)

    assertTrue(text.contains(TextPadding.padRightSide("Nagyon Hosszú Partner Cégnév Kft. Bt.", 29)))
    assertTrue(TextPadding.padRightSide("Nagyon Hosszú Partner Cégnév Kft. Bt.", 29).endsWith("..."))
  }

  private fun totalsLine(
    label: String,
    amount: Int,
  ) = "${TextPadding.padRightSide(label, 57)} ${TextPadding.padLeftSide(amount.toString(), 9)}"

  @Test
  fun `totals exclude a canceled receipt's contribution entirely`() {
    val canceled = buildReceipt(serialNumber = 20, cancelSerialNumber = 21, cancelYearCode = 26, roundedAmount = 700.0)
    val normal = buildReceipt(serialNumber = 22, roundedAmount = 300.0, quantity = 2.0)

    val text = PrintEndErrandTextBuilder.build(listOf(canceled, normal), round, user)

    assertTrue(text.contains(totalsLine("Összesen (3 db számla)", 300)))
    assertTrue(
      text.contains(
        "Számlázott készlet összesen: ${TextPadding.padLeftSide("2", 15)} karton, " +
          "${TextPadding.padLeftSide("300", 11)} Ft",
      ),
    )
  }

  @Test
  fun `cash total only accumulates receipts with zero payment days`() {
    val cash = buildReceipt(serialNumber = 30, roundedAmount = 400.0, isCash = true)
    val credit = buildReceipt(serialNumber = 31, roundedAmount = 900.0, isCash = false)

    val text = PrintEndErrandTextBuilder.build(listOf(cash, credit), round, user)

    assertTrue(text.contains(totalsLine("Összesen (2 db számla)", 1300)))
    assertTrue(text.contains(totalsLine("Készpénzes számlák összesen", 400)))
  }

  @Test
  fun `numberOfReceipts counts a canceled receipt as two`() {
    val canceled = buildReceipt(serialNumber = 40, cancelSerialNumber = 41, cancelYearCode = 26)

    val text = PrintEndErrandTextBuilder.build(listOf(canceled), round, user)

    assertTrue(text.contains("Összesen (2 db számla)"))
  }

  @Test
  fun `footer contains the handover signature block and quantity summary`() {
    val text = PrintEndErrandTextBuilder.build(emptyList(), round, user)

    assertTrue(text.contains("A számlákat és a készpénzt:\n\n\n\n"))
    assertTrue(text.contains("átvettem"))
    assertTrue(text.contains("átadtam"))
    assertTrue(text.contains("Számlázott készlet összesen:"))
  }
}
