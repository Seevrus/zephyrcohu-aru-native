package com.zephyr.boreal.api

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

abstract class BaseApiTest {
  lateinit var mockWebServer: MockWebServer
  protected lateinit var okHttpClient: OkHttpClient

  @BeforeEach
  open fun setUp() {
    mockWebServer = MockWebServer()
    mockWebServer.start()
    okHttpClient = OkHttpClient.Builder().build()
  }

  @AfterEach
  open fun tearDown() {
    mockWebServer.shutdown()
  }

  protected inline fun <reified T> createService(): T {
    val json =
      Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
      }
    val contentType = "application/json".toMediaType()

    return Retrofit
      .Builder()
      .baseUrl(mockWebServer.url("/"))
      .client(okHttpClient)
      .addConverterFactory(json.asConverterFactory(contentType))
      .build()
      .create(T::class.java)
  }

  protected fun enqueueResponse(
    fileName: String? = null,
    body: String = "",
    code: Int = 200,
  ) {
    val mockResponse = MockResponse()
    mockResponse.setResponseCode(code)
    if (body.isNotEmpty()) {
      mockResponse.setBody(body)
    }
    mockWebServer.enqueue(mockResponse)
  }
}
