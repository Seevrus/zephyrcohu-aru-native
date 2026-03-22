package com.zephyr.boreal.api

import com.zephyr.boreal.store.user.UserSessionStore
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor
  @Inject
  constructor(
    private val userSessionStore: UserSessionStore,
  ) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
      val userState = userSessionStore.userState.value
      val originalRequest = chain.request()

      val requestBuilder = originalRequest.newBuilder()

      userState.deviceId?.let {
        requestBuilder.addHeader("X-Device-Id", it)
      }

      userState.storedToken?.token?.let {
        requestBuilder.addHeader("Authorization", "Bearer $it")
      }

      return chain.proceed(requestBuilder.build())
    }
  }
