package com.zephyr.boreal.api

import com.zephyr.boreal.store.user.StoredToken
import com.zephyr.boreal.store.user.UserSessionStore
import com.zephyr.boreal.store.user.UserState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class AuthInterceptorTest {
  private lateinit var mockWebServer: MockWebServer
  private lateinit var interceptor: AuthInterceptor
  private val userSessionStore: UserSessionStore = mock()
  private val testDispatcher = StandardTestDispatcher()
  private val testScope = TestScope(testDispatcher)

  @BeforeEach
  fun setUp() {
    mockWebServer = MockWebServer()
    interceptor = AuthInterceptor(userSessionStore, testScope)
  }

  @AfterEach
  fun tearDown() {
    mockWebServer.shutdown()
  }

  @Test
  fun `intercept should add Authorization and Device-Id headers`() =
    runTest {
      val userState =
        UserState(
          deviceId = "device-123",
          storedToken = StoredToken("token-abc", false, ""),
        )
      whenever(userSessionStore.userState).thenReturn(MutableStateFlow(userState))

      mockWebServer.enqueue(MockResponse().setResponseCode(200))

      val client =
        OkHttpClient
          .Builder()
          .addInterceptor(interceptor)
          .build()

      val request = Request.Builder().url(mockWebServer.url("/")).build()
      client.newCall(request).execute()

      val recordedRequest = mockWebServer.takeRequest()
      assertEquals("Bearer token-abc", recordedRequest.getHeader("Authorization"))
      assertEquals("device-123", recordedRequest.getHeader("X-Device-Id"))
      assertEquals("application/json", recordedRequest.getHeader("Content-Type"))
      assertEquals("application/json", recordedRequest.getHeader("Accept"))
    }

  @Test
  fun `intercept should clear session on 401 Unauthorized`() =
    runTest {
      val userState =
        UserState(
          deviceId = "device-123",
          storedToken = StoredToken("expired-token", false, ""),
        )
      whenever(userSessionStore.userState).thenReturn(MutableStateFlow(userState))

      mockWebServer.enqueue(MockResponse().setResponseCode(401))

      val client =
        OkHttpClient
          .Builder()
          .addInterceptor(interceptor)
          .build()

      val request = Request.Builder().url(mockWebServer.url("/")).build()
      client.newCall(request).execute()

      testDispatcher.scheduler.advanceUntilIdle()

      verify(userSessionStore).clearSession()
    }
}
