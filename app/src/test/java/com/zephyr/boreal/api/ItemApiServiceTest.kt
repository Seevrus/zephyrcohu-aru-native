package com.zephyr.boreal.api

import com.zephyr.boreal.api.dto.request.ExpirationChangeDto
import com.zephyr.boreal.api.dto.request.SaveSelectedItemsRequestDataDto
import com.zephyr.boreal.api.dto.request.SaveSelectedItemsRequestDto
import com.zephyr.boreal.api.dto.request.SellSelectedItemsRequestDataDto
import com.zephyr.boreal.api.dto.request.SellSelectedItemsRequestDto
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ItemApiServiceTest : BaseApiTest() {
  private lateinit var service: ItemApiService

  @BeforeEach
  override fun setUp() {
    super.setUp()
    service = createService()
  }

  @Test
  fun `getItems should fetch items and parse response`() =
    runTest {
      val json =
        """
        {
          "data": [
            {
              "id": 1,
              "articleNumber": "ART001",
              "name": "Item 1",
              "shortName": "I1",
              "unitName": "kg",
              "vatRate": "27%",
              "netPrice": 100.0,
              "CNCode": "1234",
              "productCatalogCode": "PCC001",
              "expirations": [],
              "createdAt": "2026-04-03T09:00:00Z",
              "updatedAt": "2026-04-03T09:00:00Z"
            }
          ]
        }
        """.trimIndent()
      enqueueResponse(body = json)

      val response = service.getItems()
      val recordedRequest = mockWebServer.takeRequest()

      assertEquals("GET", recordedRequest.method)
      assertEquals("/items", recordedRequest.path)
      assertEquals(1, response.data.size)
      assertEquals("ART001", response.data[0].articleNumber)
    }

  @Test
  fun `getOtherItems should fetch other items and parse response`() =
    runTest {
      val json =
        """
        {
          "data": [
            {
              "id": 2,
              "articleNumber": "ART002",
              "name": "Item 2",
              "shortName": "I2",
              "unitName": "kg",
              "vatRate": "27%",
              "netPrice": 200.0,
              "createdAt": "2026-04-03T09:00:00Z",
              "updatedAt": "2026-04-03T09:00:00Z"
            }
          ]
        }
        """.trimIndent()
      enqueueResponse(body = json)

      val response = service.getOtherItems()
      val recordedRequest = mockWebServer.takeRequest()

      assertEquals("GET", recordedRequest.method)
      assertEquals("/other_items", recordedRequest.path)
      assertEquals(1, response.data.size)
      assertEquals("ART002", response.data[0].articleNumber)
    }

  @Test
  fun `getPriceLists should fetch price lists and parse response`() =
    runTest {
      val json =
        """
        {
          "data": [
            {
              "id": 1,
              "name": "Default Price List",
              "items": []
            }
          ]
        }
        """.trimIndent()
      enqueueResponse(body = json)

      val response = service.getPriceLists()
      val recordedRequest = mockWebServer.takeRequest()

      assertEquals("GET", recordedRequest.method)
      assertEquals("/price_lists", recordedRequest.path)
      assertEquals(1, response.data.size)
      assertEquals("Default Price List", response.data[0].name)
    }

  @Test
  fun `saveSelectedItems should send POST request`() =
    runTest {
      enqueueResponse(code = 204)

      val request =
        SaveSelectedItemsRequestDto(
          SaveSelectedItemsRequestDataDto(
            listOf(ExpirationChangeDto(1, 10.0, 5.0, 15.0)),
          ),
        )
      service.saveSelectedItems(request)

      val recordedRequest = mockWebServer.takeRequest()
      assertEquals("POST", recordedRequest.method)
      assertEquals("/storage/load", recordedRequest.path)
    }

  @Test
  fun `sellSelectedItems should send POST request`() =
    runTest {
      enqueueResponse(code = 204)

      val request =
        SellSelectedItemsRequestDto(
          SellSelectedItemsRequestDataDto(
            listOf(ExpirationChangeDto(1, 15.0, -5.0, 10.0)),
          ),
        )
      service.sellSelectedItems(request)

      val recordedRequest = mockWebServer.takeRequest()
      assertEquals("POST", recordedRequest.method)
      assertEquals("/storage/sell", recordedRequest.path)
    }
}
