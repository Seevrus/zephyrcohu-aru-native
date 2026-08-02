package com.zephyr.boreal.domain.utils

import com.zephyr.boreal.domain.model.StorageListItem
import com.zephyr.boreal.domain.model.StoreDetails
import com.zephyr.boreal.domain.model.User
import com.zephyr.boreal.domain.model.UserState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class PrintStorageTextBuilderTest {
  private val today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy.MM.dd."))

  private val user =
    User(
      id = 1,
      userName = "user1",
      state = UserState.LOADING,
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

  private val storeDetails =
    StoreDetails(
      id = 2,
      name = "Kamion 1",
      code = "K1",
      type = "S",
      state = "L",
      createdAt = "",
      updatedAt = "",
      expirations = emptyList(),
    )

  private fun item(
    name: String,
    originalQuantity: Double?,
    quantityChange: Int?,
    articleNumber: String = "ART1",
    expiresAt: String = "260101",
    itemBarcode: String = "",
    expirationBarcode: String = "",
    unitName: String = "db",
  ) = StorageListItem(
    itemId = 1,
    expirationId = 1,
    articleNumber = articleNumber,
    name = name,
    expiresAt = expiresAt,
    itemBarcode = itemBarcode,
    expirationBarcode = expirationBarcode,
    unitName = unitName,
    primaryStoreQuantity = 100.0,
    originalQuantity = originalQuantity,
    quantityChange = quantityChange,
  )

  @Test
  fun `header block includes today, store and user`() {
    val text = PrintStorageTextBuilder.build(printAll = true, storageItems = emptyList(), storeDetails, user)

    assertTrue(text.contains("{b}RAKJEGYZÉK | $today{/b}"))
    assertTrue(text.contains("Rakodta: user1 Kovács Béla"))
    assertTrue(text.contains("Raktár : K1 Kamion 1"))
  }

  @Test
  fun `printAll false only prints items with a nonzero quantityChange`() {
    val items =
      listOf(
        item("Changed", originalQuantity = 5.0, quantityChange = 2),
        item("Unchanged", originalQuantity = 5.0, quantityChange = null),
        item("ZeroChange", originalQuantity = 5.0, quantityChange = 0),
      )

    val text = PrintStorageTextBuilder.build(printAll = false, storageItems = items, storeDetails, user)

    assertTrue(text.contains("Changed"))
    assertFalse(text.contains("Unchanged"))
    assertFalse(text.contains("ZeroChange"))
  }

  @Test
  fun `printAll true prints every item regardless of quantityChange`() {
    val items =
      listOf(
        item("Changed", originalQuantity = 5.0, quantityChange = 2),
        item("Unchanged", originalQuantity = 5.0, quantityChange = null),
      )

    val text = PrintStorageTextBuilder.build(printAll = true, storageItems = items, storeDetails, user)

    assertTrue(text.contains("Changed"))
    assertTrue(text.contains("Unchanged"))
  }

  @Test
  fun `totals section sums over all items even when printAll is false`() {
    val items =
      listOf(
        item("Changed", originalQuantity = 5.0, quantityChange = 2),
        item("ZeroChange", originalQuantity = 3.0, quantityChange = null),
      )

    val text = PrintStorageTextBuilder.build(printAll = false, storageItems = items, storeDetails, user)

    // Only "Changed" is printed as a row, but the summary counts both items (2 termék) and
    // both originalQuantity contributions (5 + 3 = 8, current 7 + 3 = 10), matching the RN quirk.
    val summaryLine1 =
      "${"Összesen (rakodva 2 termék)".padEnd(44)} ${"8".padStart(5)} ${"+2".padStart(6)}"
    val summaryLine2 = "${"0".padStart(57)} ${"10".padStart(5)}"
    assertTrue(text.contains(summaryLine1))
    assertTrue(text.contains(summaryLine2))
  }

  @Test
  fun `item row columns are padded to the exact RN widths`() {
    val items =
      listOf(item("Alma", originalQuantity = 12.0, quantityChange = 3, articleNumber = "A1", expiresAt = "260101"))

    val text = PrintStorageTextBuilder.build(printAll = true, storageItems = items, storeDetails, user)
    val lines = text.lines()

    val firstRowLine = lines.first { it.startsWith("001.") }
    val expectedFirstLine =
      "001. ${"A1".padEnd(6)} ${"260101".padEnd(7)} ${"".padEnd(13)}"
    assertEquals(expectedFirstLine, firstRowLine)

    val secondRowLine = lines[lines.indexOf(firstRowLine) + 1]
    val expectedSecondLine =
      "${"Alma".padEnd(37)} ${"db".padEnd(6)} ${"12".padStart(5)} ${"+3".padStart(6)} ${"15".padStart(5)} [ ]"
    assertEquals(expectedSecondLine, secondRowLine)
  }

  @Test
  fun `quantityChange sign formatting matches RN - only positive gets a plus, zero and negative unsigned`() {
    val positive = item("Pos", originalQuantity = 1.0, quantityChange = 5)
    val negative = item("Neg", originalQuantity = 1.0, quantityChange = -5)

    val text =
      PrintStorageTextBuilder.build(
        printAll = true,
        storageItems = listOf(positive, negative),
        storeDetails,
        user,
      )

    assertTrue(text.contains("    +5"))
    assertTrue(text.contains("    -5"))
    assertFalse(text.contains("+-5"))
  }

  @Test
  fun `row index beyond 999 has no trailing period`() {
    val items = (1..1000).map { item(name = "Item$it", originalQuantity = 1.0, quantityChange = 1) }

    val text = PrintStorageTextBuilder.build(printAll = true, storageItems = items, storeDetails, user)

    assertTrue(text.contains("\n1000 "))
    assertFalse(text.contains("1000."))
  }

  @Test
  fun `items are sorted using Hungarian locale collation`() {
    val items =
      listOf(
        item("Zebra", originalQuantity = 1.0, quantityChange = 1),
        item("Alma", originalQuantity = 1.0, quantityChange = 1),
        item("Őz", originalQuantity = 1.0, quantityChange = 1),
      )

    val text = PrintStorageTextBuilder.build(printAll = true, storageItems = items, storeDetails, user)

    val almaIndex = text.indexOf("Alma")
    val ozIndex = text.indexOf("Őz")
    val zebraIndex = text.indexOf("Zebra")
    assertTrue(almaIndex < ozIndex)
    assertTrue(ozIndex < zebraIndex)
  }

  @Test
  fun `barcode column trims but does not insert a space between item and expiration barcode`() {
    val items =
      listOf(item("Bc", originalQuantity = 1.0, quantityChange = 1, itemBarcode = "111", expirationBarcode = "222"))

    val text = PrintStorageTextBuilder.build(printAll = true, storageItems = items, storeDetails, user)

    assertTrue(text.contains("111222"))
    assertFalse(text.contains("111 222"))
  }

  @Test
  fun `footer contains signature placeholders and identity line`() {
    val text = PrintStorageTextBuilder.build(printAll = true, storageItems = emptyList(), storeDetails, user)

    assertTrue(text.contains("rakodta"))
    assertTrue(text.contains("ellenőrizte"))
    assertTrue(text.contains("|> $today | user1 | Kovács Béla <|"))
  }
}
