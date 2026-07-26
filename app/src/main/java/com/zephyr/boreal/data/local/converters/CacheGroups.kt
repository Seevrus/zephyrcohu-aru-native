package com.zephyr.boreal.data.local.converters

/**
 * [CacheDecodeGuard] group names shared between converters (which mark a group on a decode
 * failure) and repositories (which pass the same literal as `degradeGroup` to
 * [com.zephyr.boreal.data.repository.BaseRepository.networkBoundResource]). Centralizing them
 * turns a typo on either side into a compile error instead of a silently broken invalidation path.
 */
object CacheGroups {
  const val ITEMS = "get_items"
  const val PRICE_LISTS = "get_price_lists"
  const val PARTNERS = "get_partners"
  const val PARTNER_LISTS = "get_partner_lists"
  const val TAX_NUMBER_SEARCH = "search_tax_number"
  const val ROUNDS = "get_rounds"
  const val CURRENT_USER = "get_current_user"
  const val STORES = "get_stores"
  const val STORE_DETAILS = "get_store_details"
}
