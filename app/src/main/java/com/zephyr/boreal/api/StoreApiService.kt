package com.zephyr.boreal.api

import com.zephyr.boreal.api.dto.request.SelectStoreRequestDto
import com.zephyr.boreal.api.dto.response.SelectStoreResponseTypeDto
import com.zephyr.boreal.api.dto.response.StoreDetailsResponseDto
import com.zephyr.boreal.api.dto.response.StoresResponseDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface StoreApiService {
  @GET("stores")
  suspend fun getStores(): StoresResponseDto

  @POST("storage/lock_to_user")
  suspend fun selectStore(
    @Body request: SelectStoreRequestDto,
  ): SelectStoreResponseTypeDto

  @POST("storage/unlock_from_user")
  suspend fun deselectStore()

  @GET("stores/{id}")
  suspend fun getStoreDetails(
    @Path("id") storeId: Int,
  ): StoreDetailsResponseDto
}
