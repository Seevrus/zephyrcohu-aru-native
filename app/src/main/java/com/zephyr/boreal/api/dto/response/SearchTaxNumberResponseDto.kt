package com.zephyr.boreal.api.dto.response

import com.zephyr.boreal.domain.model.TaxpayerAddressType
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement

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
  val number: String? = null,
)

// The API returns either a single AddressDto object or an array depending on the result count.
private object SingleOrListAddressSerializer : KSerializer<List<AddressDto>> {
  private val delegate = ListSerializer(AddressDto.serializer())
  override val descriptor: SerialDescriptor = delegate.descriptor

  override fun deserialize(decoder: Decoder): List<AddressDto> {
    val json = decoder as JsonDecoder
    return when (val element = json.decodeJsonElement()) {
      is JsonArray -> json.json.decodeFromJsonElement(delegate, element)
      is JsonObject -> listOf(json.json.decodeFromJsonElement(AddressDto.serializer(), element))
      else -> emptyList()
    }
  }

  override fun serialize(
    encoder: Encoder,
    value: List<AddressDto>,
  ) = delegate.serialize(encoder, value)
}

@Serializable
data class SearchTaxNumberResponseDataDto(
  val infoDate: String? = null,
  val validity: Boolean,
  val name: String? = null,
  val shortName: String? = null,
  val taxNumber: String,
  @Serializable(with = SingleOrListAddressSerializer::class)
  val addressList: List<AddressDto> = emptyList(),
)

@Serializable
data class SearchTaxNumberResponseDto(
  val data: SearchTaxNumberResponseDataDto,
)
