package com.zephyr.boreal.api.dto.response

import com.zephyr.boreal.domain.model.TaxpayerAddressType
import kotlinx.serialization.Serializable

@Serializable
data class AddressDto(
  val taxpayerAddressType: TaxpayerAddressType,
  val taxpayerAddress: TaxpayerAddressDto,
)

@Serializable
data class TaxpayerAddressDto(
  val countryCode: String,
  val postalCode: String,
  val city: String,
  val streetName: String,
  val publicPlaceCategory: String,
  val number: String,
)

@Serializable
data class SearchTaxNumberResponseDataDto(
  val infoDate: String? = null,
  val validity: Boolean,
  val name: String? = null,
  val shortName: String? = null,
  val taxNumber: String,
  // Flattened address list if it's either an object or an array in JSON
  val addressList: List<AddressDto>? = null,
)

@Serializable
data class SearchTaxNumberResponseDto(
  val data: SearchTaxNumberResponseDataDto,
)
