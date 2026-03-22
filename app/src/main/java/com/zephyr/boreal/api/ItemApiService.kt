package com.zephyr.boreal.api

import com.zephyr.boreal.api.dto.request.SaveSelectedItemsRequestDto
import com.zephyr.boreal.api.dto.request.SellSelectedItemsRequestDto
import com.zephyr.boreal.api.dto.response.ItemsResponseDto
import com.zephyr.boreal.api.dto.response.PriceListResponseDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ItemApiService {
  @GET("items")
  suspend fun getItems(): ItemsResponseDto

  @GET("other_items")
  suspend fun getOtherItems(): ItemsResponseDto

  @GET("price_lists")
  suspend fun getPriceLists(): PriceListResponseDto

  @POST("storage/load")
  suspend fun saveSelectedItems(
    @Body request: SaveSelectedItemsRequestDto,
  )

  @POST("storage/sell")
  suspend fun sellSelectedItems(
    @Body request: SellSelectedItemsRequestDto,
  )
}
