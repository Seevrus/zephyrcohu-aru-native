package com.zephyr.boreal.di

import com.zephyr.boreal.api.AuthInterceptor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import java.util.concurrent.TimeUnit

class NetworkModuleTest {
  @Test
  fun `OkHttpClient timeouts match the backend's execution timeout budget`() {
    val authInterceptor: AuthInterceptor = mock()

    val client = NetworkModule.provideOkHttpClient(authInterceptor)

    // Laravel's default max_execution_time is 30s; the client must wait at
    // least that long instead of relying on OkHttp's 10s default, otherwise
    // it times out before the backend does, while the backend keeps
    // processing (and, e.g., completes a login) after the client gave up.
    val expectedTimeoutMillis = TimeUnit.SECONDS.toMillis(30)

    assertEquals(expectedTimeoutMillis, client.connectTimeoutMillis.toLong())
    assertEquals(expectedTimeoutMillis, client.readTimeoutMillis.toLong())
    assertEquals(expectedTimeoutMillis, client.writeTimeoutMillis.toLong())
  }
}
