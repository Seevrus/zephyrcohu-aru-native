package com.zephyr.boreal.api

import com.zephyr.boreal.store.core.ApplicationScope
import com.zephyr.boreal.store.user.UserSessionStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor
  @Inject
  constructor(
    private val userSessionStore: UserSessionStore,
    @param:ApplicationScope private val scope: CoroutineScope,
  ) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
      val userState = userSessionStore.userState.value
      val originalRequest = chain.request()

      val requestBuilder =
        originalRequest
          .newBuilder()
          .header("Content-Type", "application/json")
          .header("Accept", "application/json")

      userState.deviceId?.let {
        requestBuilder.addHeader("X-Device-Id", it)
      }

      userState.storedToken?.token?.let {
        requestBuilder.addHeader("Authorization", "Bearer $it")
      }

      val response = chain.proceed(requestBuilder.build())

      if (response.code == java.net.HttpURLConnection.HTTP_UNAUTHORIZED) {
        scope.launch {
          userSessionStore.clearSession()
          // We intentionally DO NOT clear UserDao here, so the app can show a "Log Back" screen
          // with the user's name and company already filled in.
        }
      }

      return response
    }
  }
