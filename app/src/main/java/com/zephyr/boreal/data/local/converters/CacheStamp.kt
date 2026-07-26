package com.zephyr.boreal.data.local.converters

import com.zephyr.boreal.api.dto.response.BasePriceListDto
import com.zephyr.boreal.api.dto.response.CompanyDto
import com.zephyr.boreal.api.dto.response.DiscountDto
import com.zephyr.boreal.api.dto.response.ExpirationDto
import com.zephyr.boreal.api.dto.response.PartnerLocationDto
import com.zephyr.boreal.api.dto.response.RoundPartnerListDto
import com.zephyr.boreal.api.dto.response.RoundReceiptDto
import com.zephyr.boreal.api.dto.response.RoundResponseDataDto
import com.zephyr.boreal.api.dto.response.RoundStoreDto
import com.zephyr.boreal.api.dto.response.RoundUserDto
import com.zephyr.boreal.api.dto.response.StoreUserDto
import com.zephyr.boreal.domain.model.PartnerLocation
import com.zephyr.boreal.domain.model.PriceListItem
import com.zephyr.boreal.domain.model.StoreDetailsExpiration
import com.zephyr.boreal.domain.model.UserRole
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.serializer

/**
 * A cache-schema stamp derived from the shape of every DTO/domain type a cache converter
 * (see [CacheDecodeGuard]) decodes JSON into — not a hand-maintained version number. Adding,
 * removing, renaming, or retyping a field on any listed type changes [CURRENT] automatically, so
 * every stored [com.zephyr.boreal.data.local.CacheMetadataEntity] row goes stale the moment the
 * backend or a DTO changes shape, with no release-process step to remember. [ReceiptConverters]'s
 * types are deliberately excluded — `receipts` is a record, not a cache, and isn't governed by
 * this stamp.
 */
private const val HASH_MULTIPLIER = 31

object CacheStamp {
  internal val CACHE_SHAPE_TYPES: List<SerialDescriptor> =
    listOf(
      serializer<ExpirationDto>().descriptor,
      serializer<DiscountDto>().descriptor,
      serializer<PriceListItem>().descriptor,
      serializer<PartnerLocationDto>().descriptor,
      serializer<BasePriceListDto>().descriptor,
      serializer<PartnerLocation>().descriptor,
      serializer<RoundUserDto>().descriptor,
      serializer<RoundStoreDto>().descriptor,
      serializer<RoundPartnerListDto>().descriptor,
      serializer<RoundReceiptDto>().descriptor,
      serializer<StoreUserDto>().descriptor,
      serializer<StoreDetailsExpiration>().descriptor,
      serializer<CompanyDto>().descriptor,
      serializer<RoundResponseDataDto>().descriptor,
      serializer<UserRole>().descriptor,
    )

  val CURRENT: Int =
    CACHE_SHAPE_TYPES.fold(0) { acc, descriptor -> HASH_MULTIPLIER * acc + descriptor.shapeFingerprint() }
}

internal fun SerialDescriptor.shapeFingerprint(visited: MutableSet<String> = mutableSetOf()): Int {
  if (!visited.add(serialName)) {
    return serialName.hashCode()
  }
  var hash = serialName.hashCode()
  for (i in 0 until elementsCount) {
    hash = HASH_MULTIPLIER * hash + getElementName(i).hashCode()
    hash = HASH_MULTIPLIER * hash + getElementDescriptor(i).shapeFingerprint(visited)
  }
  return hash
}
