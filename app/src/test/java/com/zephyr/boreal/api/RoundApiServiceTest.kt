package com.zephyr.boreal.api

import com.zephyr.boreal.api.dto.request.FinishRoundReceiptBuyerDto
import com.zephyr.boreal.api.dto.request.FinishRoundReceiptDto
import com.zephyr.boreal.api.dto.request.FinishRoundRequestDataDto
import com.zephyr.boreal.api.dto.request.FinishRoundRequestDto
import com.zephyr.boreal.api.dto.request.StartRoundRequestDataDto
import com.zephyr.boreal.api.dto.request.StartRoundRequestDto
import com.zephyr.boreal.domain.model.InvoiceType
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RoundApiServiceTest : BaseApiTest() {
  private lateinit var service: RoundApiService

  @BeforeEach
  override fun setUp() {
    super.setUp()
    service = createService()
  }

  @Test
  fun `getRounds should fetch rounds and parse response`() =
    runTest {
      val json =
        """
        {
          "data": [
            {
              "id": 1,
              "user": { "id": 1, "userName": "ofr", "name": "Lysanne Hammes" },
              "store": { "id": 1, "code": "S1", "name": "Store 1" },
              "partnerList": { "id": 1, "name": "List 1" },
              "roundStarted": "2026-04-03T09:00:00Z",
              "roundFinished": null,
              "receipts": []
            }
          ]
        }
        """.trimIndent()
      enqueueResponse(body = json)

      val response = service.getRounds()
      val recordedRequest = mockWebServer.takeRequest()

      assertEquals("GET", recordedRequest.method)
      assertEquals("/rounds", recordedRequest.path)
      assertEquals(1, response.data.size)
      assertEquals(1, response.data[0].id)
    }

  @Test
  fun `startRound should send POST request and parse response`() =
    runTest {
      val json =
        """
        {
          "data": {
            "id": 2,
            "user": { "id": 1, "userName": "ofr", "name": "Lysanne Hammes" },
            "store": { "id": 1, "code": "S1", "name": "Store 1" },
            "partnerList": { "id": 1, "name": "List 1" },
            "yearCode": 26,
            "roundStarted": "2026-04-03T10:00:00Z",
            "roundFinished": null,
            "lastSerialNumber": null,
            "receipts": []
          }
        }
        """.trimIndent()
      enqueueResponse(body = json)

      val request = StartRoundRequestDto(StartRoundRequestDataDto(1, 1, "2026-04-03"))
      val response = service.startRound(request)

      val recordedRequest = mockWebServer.takeRequest()
      assertEquals("POST", recordedRequest.method)
      assertEquals("/rounds/start", recordedRequest.path)
      assertEquals(2, response.data.id)
    }

  @Test
  fun `finishRound should send POST request and parse response`() =
    runTest {
      val json =
        """
        {
          "data": {
            "id": 2,
            "user": { "id": 1, "userName": "ofr", "name": "Lysanne Hammes" },
            "store": { "id": 1, "code": "S1", "name": "Store 1" },
            "partnerList": { "id": 1, "name": "List 1" },
            "yearCode": 26,
            "roundStarted": "2026-04-03T10:00:00Z",
            "roundFinished": "2026-04-03T18:00:00Z",
            "lastSerialNumber": 100,
            "receipts": []
          }
        }
        """.trimIndent()
      enqueueResponse(body = json)

      val request = FinishRoundRequestDto(FinishRoundRequestDataDto(roundId = 2, receipts = emptyList()))
      val response = service.finishRound(request)

      val recordedRequest = mockWebServer.takeRequest()
      assertEquals("POST", recordedRequest.method)
      assertEquals("/rounds/finish", recordedRequest.path)
      assertEquals(2, response.data.id)
    }

  @Test
  fun `finishRound request body includes null cancellation and vatNumber fields`() =
    runTest {
      val json =
        """
        {
          "data": {
            "id": 2,
            "user": { "id": 1, "userName": "ofr", "name": "Lysanne Hammes" },
            "store": { "id": 1, "code": "S1", "name": "Store 1" },
            "partnerList": { "id": 1, "name": "List 1" },
            "yearCode": 26,
            "roundStarted": "2026-04-03T10:00:00Z",
            "roundFinished": "2026-04-03T18:00:00Z",
            "lastSerialNumber": 100,
            "receipts": []
          }
        }
        """.trimIndent()
      enqueueResponse(body = json)

      val receipt =
        FinishRoundReceiptDto(
          id = 1,
          serialNumber = 10,
          yearCode = 2026,
          cancelSerialNumber = null,
          cancelYearCode = null,
          invoiceType = InvoiceType.PAPER,
          paymentDays = 0,
          buyer = FinishRoundReceiptBuyerDto(name = "Test Buyer", vatNumber = null),
          quantity = 1.0,
          netAmount = 100.0,
          vatAmount = 27.0,
          grossAmount = 127.0,
          roundAmount = 0.0,
          roundedAmount = 127.0,
        )
      val request = FinishRoundRequestDto(FinishRoundRequestDataDto(roundId = 2, receipts = listOf(receipt)))
      service.finishRound(request)

      val recordedRequest = mockWebServer.takeRequest()
      val bodyString = recordedRequest.body.readUtf8()
      assertTrue(bodyString.contains("\"cancelSerialNumber\":null"))
      assertTrue(bodyString.contains("\"cancelYearCode\":null"))
      assertTrue(bodyString.contains("\"vatNumber\":null"))
    }
}
