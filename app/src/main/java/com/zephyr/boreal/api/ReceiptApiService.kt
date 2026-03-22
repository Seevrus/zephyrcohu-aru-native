package com.zephyr.boreal.api

import com.zephyr.boreal.api.dto.request.CreateCancelReceiptsRequestWrapperDto
import com.zephyr.boreal.api.dto.request.CreateOrdersRequestWrapperDto
import com.zephyr.boreal.api.dto.request.CreateReceiptsRequestWrapperDto
import com.zephyr.boreal.api.dto.response.CreateOrdersResponseDto
import com.zephyr.boreal.api.dto.response.ReceiptsResponseDto
import retrofit2.http.Body
import retrofit2.http.POST

interface ReceiptApiService {
  @POST("receipts")
  suspend fun createReceipt(
    @Body request: CreateReceiptsRequestWrapperDto,
  ): ReceiptsResponseDto

  @POST("receipts/cancel")
  suspend fun cancelReceipt(
    @Body request: CreateCancelReceiptsRequestWrapperDto,
  ): ReceiptsResponseDto

  @POST("orders")
  suspend fun createOrders(
    @Body request: CreateOrdersRequestWrapperDto,
  ): CreateOrdersResponseDto
}
