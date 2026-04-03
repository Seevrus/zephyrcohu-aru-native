package com.zephyr.boreal.api

import com.zephyr.boreal.api.dto.request.CreateCancelReceiptsRequestDto
import com.zephyr.boreal.api.dto.request.CreateOrdersRequestDto
import com.zephyr.boreal.api.dto.request.CreateReceiptsRequestDto
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ReceiptApiServiceTest : BaseApiTest() {
  private lateinit var service: ReceiptApiService

  @BeforeEach
  override fun setUp() {
    super.setUp()
    service = createService()
  }

  @Test
  fun `createReceipt should send POST request and parse response`() =
    runTest {
      val json =
        """
        {
          "data": [
            {
              "id": 1,
              "companyId": 3,
              "companyCode": "003",
              "partnerId": 1,
              "partnerCode": "P001",
              "partnerSiteCode": "S001",
              "serialNumber": 100,
              "yearCode": 26,
              "originalCopiesPrinted": 1,
              "vendor": {
                "name": "Vendor", "country": "HU", "postalCode": "1000", "city": "B", "address": "A",
                "felir": "F", "iban": "I", "bankAccount": "B", "vatNumber": "V"
              },
              "buyer": {
                "id": 1, "name": "Buyer", "country": "HU", "postalCode": "1000", "city": "B", "address": "A",
                "deliveryName": "DN", "deliveryCountry": "HU", "deliveryPostalCode": "1000", "deliveryCity": "B", "deliveryAddress": "A"
              },
              "invoiceDate": "2026-04-03",
              "fulfillmentDate": "2026-04-03",
              "invoiceType": "P",
              "paidDate": "2026-04-03",
              "items": [],
              "quantity": 1.0,
              "netAmount": 1000.0,
              "vatAmount": 270.0,
              "grossAmount": 1270.0,
              "vatAmounts": [],
              "roundAmount": 0.0,
              "roundedAmount": 1270.0,
              "createdAt": "2026-04-03T10:00:00Z",
              "updatedAt": "2026-04-03T10:00:00Z"
            }
          ]
        }
        """.trimIndent()
      enqueueResponse(body = json)

      val request = CreateReceiptsRequestDto(emptyList())
      val response = service.createReceipt(request)

      val recordedRequest = mockWebServer.takeRequest()
      assertEquals("POST", recordedRequest.method)
      assertEquals("/receipts", recordedRequest.path)
      assertEquals(1, response.data.size)
      assertEquals(100, response.data[0].serialNumber)
    }

  @Test
  fun `cancelReceipt should send POST request and parse response`() =
    runTest {
      val json =
        """
        {
          "data": [
            {
              "id": 2,
              "companyId": 3,
              "companyCode": "003",
              "partnerId": 1,
              "partnerCode": "P001",
              "partnerSiteCode": "S001",
              "serialNumber": 101,
              "yearCode": 26,
              "originalCopiesPrinted": 1,
              "vendor": {
                "name": "Vendor", "country": "HU", "postalCode": "1000", "city": "B", "address": "A",
                "felir": "F", "iban": "I", "bankAccount": "B", "vatNumber": "V"
              },
              "buyer": {
                "id": 1, "name": "Buyer", "country": "HU", "postalCode": "1000", "city": "B", "address": "A",
                "deliveryName": "DN", "deliveryCountry": "HU", "deliveryPostalCode": "1000", "deliveryCity": "B", "deliveryAddress": "A"
              },
              "invoiceDate": "2026-04-03",
              "fulfillmentDate": "2026-04-03",
              "invoiceType": "P",
              "paidDate": "2026-04-03",
              "items": [],
              "quantity": -1.0,
              "netAmount": -1000.0,
              "vatAmount": -270.0,
              "grossAmount": -1270.0,
              "vatAmounts": [],
              "roundAmount": 0.0,
              "roundedAmount": -1270.0,
              "createdAt": "2026-04-03T11:00:00Z",
              "updatedAt": "2026-04-03T11:00:00Z"
            }
          ]
        }
        """.trimIndent()
      enqueueResponse(body = json)

      val request = CreateCancelReceiptsRequestDto(emptyList())
      val response = service.cancelReceipt(request)

      val recordedRequest = mockWebServer.takeRequest()
      assertEquals("POST", recordedRequest.method)
      assertEquals("/receipts/cancel", recordedRequest.path)
      assertEquals(1, response.data.size)
      assertEquals(101, response.data[0].serialNumber)
    }

  @Test
  fun `createOrders should send POST request and parse response`() =
    runTest {
      val json =
        """
        {
          "data": [
            {
              "id": 1,
              "partnerId": 1,
              "orderedAt": "2026-04-03T10:00:00Z",
              "items": []
            }
          ]
        }
        """.trimIndent()
      enqueueResponse(body = json)

      val request = CreateOrdersRequestDto(emptyList())
      val response = service.createOrders(request)

      val recordedRequest = mockWebServer.takeRequest()
      assertEquals("POST", recordedRequest.method)
      assertEquals("/orders", recordedRequest.path)
      assertEquals(1, response.data.size)
    }
}
