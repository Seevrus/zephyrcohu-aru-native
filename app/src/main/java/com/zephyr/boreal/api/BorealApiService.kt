package com.zephyr.boreal.api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Retrofit interface for Boreal API.
 */
interface BorealApiService {
  @POST("login")
  suspend fun login(
    @Body credentials: Any,
  ): Any // Placeholder

  @GET("check-token")
  suspend fun checkToken(): Boolean // Placeholder
}
