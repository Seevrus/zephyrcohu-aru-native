package com.zephyr.boreal.data.local.converters

import kotlinx.serialization.json.Json

/**
 * Single shared configuration for the persistence-format `Json` used by Room's TypeConverters
 * and [com.zephyr.boreal.store.receipts.ReceiptsStore]'s draft encoding. Kept separate from
 * `NetworkModule`'s wire-format `Json` (a legitimately different concern) so a network config
 * change can't silently alter stored data.
 */
object PersistenceJson {
  val instance = Json { ignoreUnknownKeys = true }
}
