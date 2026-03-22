package com.zephyr.boreal.api

import com.zephyr.boreal.api.dto.request.FinishRoundRequestWrapperDto
import com.zephyr.boreal.api.dto.request.StartRoundRequestWrapperDto
import com.zephyr.boreal.api.dto.response.RoundResponseDto
import com.zephyr.boreal.api.dto.response.RoundsResponseDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface RoundApiService {
  @GET("rounds")
  suspend fun getRounds(): RoundsResponseDto

  @POST("rounds/start")
  suspend fun startRound(
    @Body request: StartRoundRequestWrapperDto,
  ): RoundResponseDto

  @POST("rounds/finish")
  suspend fun finishRound(
    @Body request: FinishRoundRequestWrapperDto,
  ): RoundResponseDto
}
