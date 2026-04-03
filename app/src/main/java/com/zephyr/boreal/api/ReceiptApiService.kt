package com.zephyr.boreal.api

import com.zephyr.boreal.api.dto.request.CreateCancelReceiptsRequestDto
import com.zephyr.boreal.api.dto.request.CreateOrdersRequestDto
import com.zephyr.boreal.api.dto.request.CreateReceiptsRequestDto
import com.zephyr.boreal.api.dto.response.CreateOrdersResponseDto
import com.zephyr.boreal.api.dto.response.ReceiptsResponseDto
import retrofit2.http.Body
import retrofit2.http.POST

interface ReceiptApiService {
  @POST("receipts")
  suspend fun createReceipt(
    @Body request: CreateReceiptsRequestDto,
  ): ReceiptsResponseDto

  @POST("receipts/cancel")
  suspend fun cancelReceipt(
    @Body request: CreateCancelReceiptsRequestDto,
  ): ReceiptsResponseDto

  @POST("orders")
  suspend fun createOrders(
    @Body request: CreateOrdersRequestDto,
  ): CreateOrdersResponseDto
}
