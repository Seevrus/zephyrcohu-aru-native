package com.zephyr.boreal.data.local.record

import kotlinx.serialization.SerializationException
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

/**
 * The inverse of the cache converter tests in `data/local/converters` — `receipts` is a durable
 * record, so a decode failure here must surface as a bug, not degrade to a cache miss.
 */
class ReceiptConvertersTest {
  private val converters = ReceiptConverters()

  @Test
  fun `toReceiptVendor throws on malformed JSON instead of degrading`() {
    assertThrows(SerializationException::class.java) {
      converters.toReceiptVendor("not valid json")
    }
  }

  @Test
  fun `toReceiptBuyer throws when a required field is missing`() {
    assertThrows(SerializationException::class.java) {
      // Missing the required "name" field.
      converters.toReceiptBuyer("""{"id":1,"country":"HU"}""")
    }
  }

  @Test
  fun `toReceiptItemList throws on malformed JSON instead of degrading`() {
    assertThrows(SerializationException::class.java) {
      converters.toReceiptItemList("not valid json")
    }
  }

  @Test
  fun `toReceiptVatAmountList throws on malformed JSON instead of degrading`() {
    assertThrows(SerializationException::class.java) {
      converters.toReceiptVatAmountList("not valid json")
    }
  }
}
