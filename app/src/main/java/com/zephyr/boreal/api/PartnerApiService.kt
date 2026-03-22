package com.zephyr.boreal.api

import com.zephyr.boreal.api.dto.response.PartnersListResponseDto
import com.zephyr.boreal.api.dto.response.PartnersResponseDto
import com.zephyr.boreal.api.dto.response.SearchTaxNumberResponseDto
import retrofit2.http.GET

interface PartnerApiService {
  @GET("partners")
  suspend fun getPartners(): PartnersResponseDto

  @GET("partner_lists")
  suspend fun getPartnerLists(): PartnersListResponseDto

  @POST("partners/search")
  suspend fun searchTaxNumber(
    @Body request: SearchTaxNumberRequestDto,
  ): SearchTaxNumberResponseDto
}
