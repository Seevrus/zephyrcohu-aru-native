package com.zephyr.boreal.data.repository

import com.zephyr.boreal.api.ReceiptApiService
import com.zephyr.boreal.api.dto.request.CreateReceiptRequestDataDto
import com.zephyr.boreal.api.dto.request.CreateReceiptsRequestDto
import com.zephyr.boreal.api.dto.response.ReceiptBuyerDto
import com.zephyr.boreal.api.dto.response.ReceiptResponseDataDto
import com.zephyr.boreal.api.dto.response.ReceiptVatAmountDto
import com.zephyr.boreal.api.dto.response.ReceiptVendorDto
import com.zephyr.boreal.api.dto.response.ReceiptsResponseDto
import com.zephyr.boreal.data.local.dao.CacheMetadataDao
import com.zephyr.boreal.domain.model.InvoiceType
import com.zephyr.boreal.network.ConnectivityObserver
import com.zephyr.boreal.store.user.UserSessionStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class ReceiptsRepositoryTest {
  private val apiService: ReceiptApiService = mock()
  private val cacheMetadataDao: CacheMetadataDao = mock()
  private val connectivityObserver: ConnectivityObserver = mock()
  private val userSessionStore: UserSessionStore = mock()

  private fun createRepository() =
    ReceiptsRepository(apiService, cacheMetadataDao, connectivityObserver, userSessionStore)

  private val vendor = ReceiptVendorDto("V", "HU", "1000", "Bp", "Addr", "FELIR0001", "iban", "acc", "vat")
  private val buyer = ReceiptBuyerDto(1, "B", "HU", "1000", "Bp", "Addr", "B", "HU", "1000", "Bp", "Addr")
  private val vatAmounts = listOf(ReceiptVatAmountDto("27", 100.0, 27.0, 127.0))

  private fun buildRequest(serialNumber: Int = 100) =
    CreateReceiptRequestDataDto(
      partnerId = 1,
      partnerCode = "P00001",
      partnerSiteCode = "S001",
      serialNumber = serialNumber,
      yearCode = 26,
      vendor = vendor,
      buyer = buyer,
      invoiceDate = "2026-07-18",
      fulfillmentDate = "2026-07-18",
      invoiceType = InvoiceType.PAPER,
      paymentDays = 0,
      paidDate = "2026-07-18",
      items = emptyList(),
      otherItems = emptyList(),
      quantity = 1.0,
      netAmount = 100.0,
      vatAmount = 27.0,
      grossAmount = 127.0,
      vatAmounts = vatAmounts,
      roundAmount = 0.0,
      roundedAmount = 127.0,
    )

  private fun buildResponseData(serialNumber: Int) =
    ReceiptResponseDataDto(
      id = 1,
      companyId = 1,
      companyCode = "C",
      partnerId = 1,
      partnerCode = "P00001",
      partnerSiteCode = "S001",
      serialNumber = serialNumber,
      yearCode = 26,
      vendor = vendor,
      buyer = buyer,
      invoiceDate = "2026-07-18",
      fulfillmentDate = "2026-07-18",
      invoiceType = InvoiceType.PAPER,
      paidDate = "2026-07-18",
      items = emptyList(),
      otherItems = emptyList(),
      quantity = 1.0,
      netAmount = 100.0,
      vatAmount = 27.0,
      grossAmount = 127.0,
      vatAmounts = vatAmounts,
      roundAmount = 0.0,
      roundedAmount = 127.0,
      createdAt = "2026-07-18T00:00:00Z",
      updatedAt = "2026-07-18T00:00:00Z",
    )

  @Test
  fun `createReceipt returns success on the first attempt when the backend creates a receipt`() =
    runTest {
      whenever(apiService.createReceipt(any())).thenReturn(ReceiptsResponseDto(listOf(buildResponseData(100))))

      val result = createRepository().createReceipt(buildRequest(serialNumber = 100))

      assertTrue(result is ApiResource.Success)
      verify(apiService, times(1)).createReceipt(any())
    }

  @Test
  fun `createReceipt retries with the next serial number when the backend returns an empty list`() =
    runTest {
      whenever(apiService.createReceipt(any()))
        .thenReturn(ReceiptsResponseDto(emptyList()))
        .thenReturn(ReceiptsResponseDto(listOf(buildResponseData(101))))

      val result = createRepository().createReceipt(buildRequest(serialNumber = 100))

      assertTrue(result is ApiResource.Success)
      val captor = argumentCaptor<CreateReceiptsRequestDto>()
      verify(apiService, times(2)).createReceipt(captor.capture())
      assertEquals(
        100,
        captor.firstValue.data
          .first()
          .serialNumber,
      )
      assertEquals(
        101,
        captor.secondValue.data
          .first()
          .serialNumber,
      )
    }

  @Test
  fun `createReceipt gives up after exhausting retries and returns an error instead of crashing`() =
    runTest {
      whenever(apiService.createReceipt(any())).thenReturn(ReceiptsResponseDto(emptyList()))

      val result = createRepository().createReceipt(buildRequest(serialNumber = 100))

      assertTrue(result is ApiResource.Error)
      verify(apiService, times(5)).createReceipt(any())
    }

  @Test
  fun `createReceipt returns an error instead of throwing when the api call fails`() =
    runTest {
      whenever(apiService.createReceipt(any())).thenThrow(RuntimeException("boom"))

      val result = createRepository().createReceipt(buildRequest())

      assertTrue(result is ApiResource.Error)
      verify(apiService, times(1)).createReceipt(any())
    }
}
