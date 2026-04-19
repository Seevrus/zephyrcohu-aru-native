package com.zephyr.boreal.api

import com.zephyr.boreal.api.dto.request.ChangePasswordRequestDto
import com.zephyr.boreal.api.dto.request.LoginRequestDto
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AuthApiServiceTest : BaseApiTest() {
  private lateinit var service: AuthApiService

  @BeforeEach
  override fun setUp() {
    super.setUp()
    service = createService()
  }

  @Test
  fun `login should send correct request and parse response`() =
    runTest {
      val mockResponseBody =
        """
        {
          "id": 1,
          "userName": "testuser",
          "state": "I",
          "name": "Test User",
          "isDev": false,
          "roles": ["AM"],
          "lastActive": "2026-04-03T10:00:00Z",
          "createdAt": "2026-04-03T09:00:00Z",
          "updatedAt": "2026-04-03T09:00:00Z",
          "company": {
            "id": 3,
            "code": "C3",
            "name": "Company 3",
            "country": "HU",
            "postalCode": "1000",
            "city": "Budapest",
            "address": "Test Street 1",
            "felir": "AA0000000",
            "vatNumber": "12345678-2-41",
            "iban": "HU1234567890",
            "bankAccount": "12345678-12345678-12345678"
          },
          "token": {
            "tokenType": "Bearer",
            "accessToken": "mocked_token",
            "abilities": ["AM"]
          }
        }
        """.trimIndent()
      enqueueResponse(body = mockResponseBody)

      val request = LoginRequestDto("testuser", "password")
      val response = service.login("device-123", request)

      val recordedRequest = mockWebServer.takeRequest()
      assertEquals("POST", recordedRequest.method)
      assertEquals("/users/login", recordedRequest.path)
      assertEquals("device-123", recordedRequest.getHeader("X-Device-Id"))

      // We check that the response parsed correctly
      assertEquals(1, response.id)
      assertEquals("testuser", response.userName)
      assertEquals(3, response.company.id)
      assertEquals("mocked_token", response.token.accessToken)
    }

  @Test
  fun `checkToken should send GET request and parse response`() =
    runTest {
      val mockResponseBody =
        """
        {
          "id": 1,
          "userName": "testuser",
          "state": "I",
          "name": "Test User",
          "isDev": false,
          "roles": ["AM"],
          "lastActive": "2026-04-03T10:00:00Z",
          "createdAt": "2026-04-03T09:00:00Z",
          "updatedAt": "2026-04-03T09:00:00Z",
          "company": {
            "id": 3,
            "code": "C3",
            "name": "Company 3",
            "country": "HU",
            "postalCode": "1000",
            "city": "Budapest",
            "address": "Test Street 1",
            "felir": "AA0000000",
            "vatNumber": "12345678-2-41",
            "iban": "HU1234567890",
            "bankAccount": "12345678-12345678-12345678"
          },
          "token": {
            "tokenType": "Bearer",
            "accessToken": "mocked_token",
            "abilities": ["AM"]
          }
        }
        """.trimIndent()
      enqueueResponse(body = mockResponseBody)

      val response = service.checkToken()

      val recordedRequest = mockWebServer.takeRequest()
      assertEquals("GET", recordedRequest.method)
      assertEquals("/users/check-token", recordedRequest.path)

      assertEquals(3, response.company.id)
      assertEquals("mocked_token", response.token.accessToken)
    }

  @Test
  fun `logout should send POST request`() =
    runTest {
      enqueueResponse(code = 204)

      service.logout()

      val recordedRequest = mockWebServer.takeRequest()
      assertEquals("POST", recordedRequest.method)
      assertEquals("/users/logout", recordedRequest.path)
    }

  @Test
  fun `changePassword should send POST request with correct body`() =
    runTest {
      enqueueResponse(code = 204)

      val request = ChangePasswordRequestDto("password")
      service.changePassword(request)

      val recordedRequest = mockWebServer.takeRequest()
      assertEquals("POST", recordedRequest.method)
      assertEquals("/users/password", recordedRequest.path)

      val bodyString = recordedRequest.body.readUtf8()
      assert(bodyString.contains("password"))
    }
}
