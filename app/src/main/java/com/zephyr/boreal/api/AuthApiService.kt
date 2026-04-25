package com.zephyr.boreal.api

import com.zephyr.boreal.api.dto.request.ChangePasswordRequestDto
import com.zephyr.boreal.api.dto.request.LoginRequestDto
import com.zephyr.boreal.api.dto.response.CheckTokenResponseDto
import com.zephyr.boreal.api.dto.response.LoginResponseDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AuthApiService {
  @POST("users/login")
  suspend fun login(
    @retrofit2.http.Header("X-Device-Id") deviceId: String,
    @Body credentials: LoginRequestDto,
  ): LoginResponseDto

  @GET("users/check-token")
  suspend fun checkToken(): CheckTokenResponseDto

  @POST("users/logout")
  suspend fun logout()

  @POST("users/password")
  suspend fun changePassword(
    @Body request: ChangePasswordRequestDto,
  ): LoginResponseDto
}
