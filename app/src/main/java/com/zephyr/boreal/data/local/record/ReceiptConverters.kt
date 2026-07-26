package com.zephyr.boreal.data.local.record

import androidx.room.TypeConverter
import com.zephyr.boreal.data.local.converters.PersistenceJson
import com.zephyr.boreal.domain.model.InvoiceType
import kotlinx.serialization.encodeToString

/**
 * Unlike the cache converters in `data/local/converters`, these deliberately do not degrade on a
 * decode failure — `receipts` is a durable record, so a corrupt row is a bug to surface, never a
 * cache miss to paper over.
 */
class ReceiptConverters {
  private val json = PersistenceJson.instance

  @TypeConverter
  fun fromInvoiceType(value: InvoiceType): String = value.name

  @TypeConverter
  fun toInvoiceType(value: String): InvoiceType = InvoiceType.valueOf(value)

  @TypeConverter
  fun fromReceiptVendor(value: PersistedReceiptVendor): String = json.encodeToString(value)

  @TypeConverter
  fun toReceiptVendor(value: String): PersistedReceiptVendor = json.decodeFromString(value)

  @TypeConverter
  fun fromReceiptBuyer(value: PersistedReceiptBuyer): String = json.encodeToString(value)

  @TypeConverter
  fun toReceiptBuyer(value: String): PersistedReceiptBuyer = json.decodeFromString(value)

  @TypeConverter
  fun fromReceiptUser(value: PersistedReceiptUser?): String? = value?.let { json.encodeToString(it) }

  @TypeConverter
  fun toReceiptUser(value: String?): PersistedReceiptUser? = value?.let { json.decodeFromString(it) }

  @TypeConverter
  fun fromReceiptItemList(value: List<PersistedReceiptItem>): String = json.encodeToString(value)

  @TypeConverter
  fun toReceiptItemList(value: String): List<PersistedReceiptItem> = json.decodeFromString(value)

  @TypeConverter
  fun fromReceiptOtherItemList(value: List<PersistedReceiptOtherItem>): String = json.encodeToString(value)

  @TypeConverter
  fun toReceiptOtherItemList(value: String): List<PersistedReceiptOtherItem> = json.decodeFromString(value)

  @TypeConverter
  fun fromReceiptVatAmountList(value: List<PersistedReceiptVatAmount>): String = json.encodeToString(value)

  @TypeConverter
  fun toReceiptVatAmountList(value: String): List<PersistedReceiptVatAmount> = json.decodeFromString(value)
}
