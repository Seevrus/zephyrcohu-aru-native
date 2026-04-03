package com.zephyr.boreal.api

import com.zephyr.boreal.api.dto.request.SelectStoreDataDto
import com.zephyr.boreal.api.dto.request.SelectStoreRequestDto
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class StoreApiServiceTest : BaseApiTest() {
  private lateinit var service: StoreApiService

  @BeforeEach
  override fun setUp() {
    super.setUp()
    service = createService()
  }

  @Test
  fun `getStores should fetch stores and parse response`() =
    runTest {
      val json =
        """
        {
          "data": [
            {
              "id": 1,
              "code": "S001",
              "name": "Main Store",
              "type": "P",
              "state": "I",
              "firstAvailableSerialNumber": 1,
              "lastAvailableSerialNumber": 999,
              "yearCode": 26,
              "createdAt": "2026-04-03T09:00:00Z",
              "updatedAt": "2026-04-03T09:00:00Z"
            }
          ]
        }
        """.trimIndent()
      enqueueResponse(body = json)

      val response = service.getStores()
      val recordedRequest = mockWebServer.takeRequest()

      assertEquals("GET", recordedRequest.method)
      assertEquals("/stores", recordedRequest.path)
      assertEquals(1, response.data.size)
      assertEquals("Main Store", response.data[0].name)
    }

  @Test
  fun `selectStore should send POST request and parse response`() =
    runTest {
      val json =
        """
        {
          "id": 101,
          "userName": "ofr",
          "state": "L",
          "name": "Lysanne Hammes",
          "isDev": false,
          "roles": ["A"],
          "storeInUseId": 101,
          "storeOwnedId": 101,
          "lastActive": "2026-04-03T10:00:00Z",
          "createdAt": "2026-04-03T09:00:00Z",
          "updatedAt": "2026-04-03T09:00:00Z",
          "company": {
            "id": 3,
            "code": "003",
            "name": "TESZT CÉG Kft.",
            "country": "HU",
            "postalCode": "1000",
            "city": "Budapest",
            "address": "Test Street 1",
            "felir": "AA0000000",
            "vatNumber": "12345678-2-41",
            "iban": "HU123",
            "bankAccount": "123-123"
          }
        }
        """.trimIndent()
      enqueueResponse(body = json)

      val request = SelectStoreRequestDto(SelectStoreDataDto(101))
      val response = service.selectStore(request)

      val recordedRequest = mockWebServer.takeRequest()
      assertEquals("POST", recordedRequest.method)
      assertEquals("/storage/lock_to_user", recordedRequest.path)
      assertEquals(101, response.id)
      assertEquals(101, response.storeInUseId)
    }

  @Test
  fun `deselectStore should send POST request`() =
    runTest {
      enqueueResponse(code = 204)

      service.deselectStore()

      val recordedRequest = mockWebServer.takeRequest()
      assertEquals("POST", recordedRequest.method)
      assertEquals("/storage/unlock_from_user", recordedRequest.path)
    }

  @Test
  fun `getStoreDetails should fetch store details`() =
    runTest {
      val json =
        """
        {
          "data": {
            "id": 1,
            "name": "Main Store",
            "code": "S001",
            "address": "A",
            "city": "B",
            "postalCode": "1000",
            "country": "HU",
            "createdAt": "2026-04-03T09:00:00Z",
            "updatedAt": "2026-04-03T09:00:00Z",
            "expirations": []
          }
        }
        """.trimIndent()
      enqueueResponse(body = json)

      val response = service.getStoreDetails(1)
      val recordedRequest = mockWebServer.takeRequest()

      assertEquals("GET", recordedRequest.method)
      assertEquals("/stores/1", recordedRequest.path)
      assertEquals(1, response.data.id)
      assertEquals("Main Store", response.data.name)
    }
}
