package com.zephyr.boreal.data.repository

import com.zephyr.boreal.api.ReceiptApiService
import com.zephyr.boreal.api.dto.request.CreateCancelReceiptDataDto
import com.zephyr.boreal.api.dto.request.CreateCancelReceiptsRequestDto
import com.zephyr.boreal.api.dto.request.CreateOrdersRequestDto
import com.zephyr.boreal.api.dto.request.CreateReceiptRequestDataDto
import com.zephyr.boreal.api.dto.request.CreateReceiptsRequestDto
import com.zephyr.boreal.api.dto.request.OrderRequestDataDto
import com.zephyr.boreal.data.mapper.toDomain
import com.zephyr.boreal.domain.model.CreatedOrder
import com.zephyr.boreal.domain.model.Receipt
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReceiptsRepository
  @Inject
  constructor(
    private val apiService: ReceiptApiService,
  ) : BaseRepository() {
    suspend fun createReceipt(request: CreateReceiptRequestDataDto): ApiResource<Receipt> =
      try {
        val response = apiService.createReceipt(CreateReceiptsRequestDto(listOf(request)))
        // Original TS returns response.data.data (array), we take the first one
        ApiResource.Success(response.data.first().toDomain())
      } catch (e: Exception) {
        ApiResource.Error(e.localizedMessage ?: "Failed to create receipt")
      }

    suspend fun cancelReceipt(request: CreateCancelReceiptDataDto): ApiResource<Receipt> =
      try {
        val response = apiService.cancelReceipt(CreateCancelReceiptsRequestDto(listOf(request)))
        ApiResource.Success(response.data.first().toDomain())
      } catch (e: Exception) {
        ApiResource.Error(e.localizedMessage ?: "Failed to cancel receipt")
      }

    suspend fun createOrders(request: List<OrderRequestDataDto>): ApiResource<List<CreatedOrder>> =
      try {
        val response = apiService.createOrders(CreateOrdersRequestDto(request))
        ApiResource.Success(response.data.map { it.toDomain() })
      } catch (e: Exception) {
        ApiResource.Error(e.localizedMessage ?: "Failed to create orders")
      }
  }
