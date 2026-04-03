package com.zephyr.boreal.api

import com.zephyr.boreal.api.dto.request.SearchTaxNumberDataDto
import com.zephyr.boreal.api.dto.request.SearchTaxNumberRequestDto
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PartnerApiServiceTest : BaseApiTest() {
  private lateinit var service: PartnerApiService

  @BeforeEach
  override fun setUp() {
    super.setUp()
    service = createService()
  }

  @Test
  fun `getPartners should fetch partners and parse response`() =
    runTest {
      val json =
        """
        {
          "data": [
            {
              "id": 1,
              "code": "P001",
              "siteCode": "S001",
              "vatNumber": "12345678-2-41",
              "invoiceType": "P",
              "invoiceCopies": 1,
              "paymentDays": 8,
              "locations": [
                {
                  "name": "Partner 1 HQ",
                  "locationType": "C",
                  "country": "HU",
                  "postalCode": "1000",
                  "city": "Budapest",
                  "address": "Partner Street 1",
                  "createdAt": "2026-04-03T09:00:00Z",
                  "updatedAt": "2026-04-03T09:00:00Z"
                }
              ],
              "createdAt": "2026-04-03T09:00:00Z",
              "updatedAt": "2026-04-03T09:00:00Z"
            }
          ]
        }
        """.trimIndent()
      enqueueResponse(body = json)

      val response = service.getPartners()
      val recordedRequest = mockWebServer.takeRequest()

      assertEquals("GET", recordedRequest.method)
      assertEquals("/partners", recordedRequest.path)
      assertEquals(1, response.data.size)
      assertEquals("P001", response.data[0].code)
    }

  @Test
  fun `getPartnerLists should fetch partner lists and parse response`() =
    runTest {
      val json =
        """
        {
          "data": [
            {
              "id": 1,
              "name": "Default Partner List",
              "partners": [1, 2],
              "createdAt": "2026-04-03T09:00:00Z",
              "updatedAt": "2026-04-03T09:00:00Z"
            }
          ]
        }
        """.trimIndent()
      enqueueResponse(body = json)

      val response = service.getPartnerLists()
      val recordedRequest = mockWebServer.takeRequest()

      assertEquals("GET", recordedRequest.method)
      assertEquals("/partner_lists", recordedRequest.path)
      assertEquals(1, response.data.size)
      assertEquals("Default Partner List", response.data[0].name)
    }

  @Test
  fun `searchTaxNumber should send POST request and parse response`() =
    runTest {
      val json =
        """
        {
          "data": {
            "validity": true,
            "name": "Searched Partner",
            "taxNumber": "12345678-2-41",
            "addressList": []
          }
        }
        """.trimIndent()
      enqueueResponse(body = json)

      val request = SearchTaxNumberRequestDto(SearchTaxNumberDataDto("12345678-2-41"))
      val response = service.searchTaxNumber(request)

      val recordedRequest = mockWebServer.takeRequest()
      assertEquals("POST", recordedRequest.method)
      assertEquals("/partners/search", recordedRequest.path)
      assertEquals("Searched Partner", response.data.name)
    }
}
