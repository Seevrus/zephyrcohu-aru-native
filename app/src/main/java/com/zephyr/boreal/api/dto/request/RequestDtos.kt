package com.zephyr.boreal.api.dto.request

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequestDto(
  val userName: String,
  val password: String,
)

@Serializable
data class ChangePasswordRequestDto(
  val password: String,
)

@Serializable
data class SelectStoreRequestDto(
  val data: SelectStoreDataDto,
)

@Serializable
data class SelectStoreDataDto(
  val storeId: Int,
)

@Serializable
data class StartRoundRequestDataDto(
  val storeId: Int,
  val partnerListId: Int,
  // yyyy-MM-dd
  val roundStarted: String,
)

@Serializable
data class StartRoundRequestDto(
  val data: StartRoundRequestDataDto,
)

@Serializable
data class CreateCancelReceiptDataDto(
  val id: Int,
  val cancelSerialNumber: Int,
  val cancelYearCode: Int,
)

@Serializable
data class CreateCancelReceiptsRequestDto(
  val data: List<CreateCancelReceiptDataDto>,
)

@Serializable
data class CreateReceiptsRequestDto(
  val data: List<CreateReceiptRequestDataDto>,
)

@Serializable
data class OrderRequestDataDto(
  val partnerId: Int,
  // UTC
  val orderedAt: String,
  val items: List<OrderItemDto>,
)

@Serializable
data class CreateOrdersRequestDto(
  val data: List<OrderRequestDataDto>,
)

@Serializable
data class FinishRoundRequestDto(
  val data: FinishRoundRequestDataDto,
)

@Serializable
data class SearchTaxNumberRequestDto(
  val data: SearchTaxNumberDataDto,
)

@Serializable
data class SearchTaxNumberDataDto(
  val taxNumber: String,
)

@Serializable
data class OrderItemDto(
  val articleNumber: String,
  val name: String,
  val quantity: Double,
)

@Serializable
data class ExpirationChangeDto(
  val expirationId: Int,
  val startingQuantity: Double,
  val quantityChange: Double,
  val finalQuantity: Double,
)

@Serializable
data class SaveSelectedItemsRequestDto(
  val data: SaveSelectedItemsRequestDataDto,
)

@Serializable
data class SaveSelectedItemsRequestDataDto(
  val changes: List<ExpirationChangeDto>,
)

@Serializable
data class SellSelectedItemsRequestDto(
  val data: SellSelectedItemsRequestDataDto,
)

@Serializable
data class SellSelectedItemsRequestDataDto(
  val changes: List<ExpirationChangeDto>,
)
