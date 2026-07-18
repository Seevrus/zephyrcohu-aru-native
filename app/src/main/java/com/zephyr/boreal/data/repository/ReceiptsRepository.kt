package com.zephyr.boreal.data.repository

import android.util.Log
import com.zephyr.boreal.api.ReceiptApiService
import com.zephyr.boreal.api.dto.request.CreateCancelReceiptDataDto
import com.zephyr.boreal.api.dto.request.CreateCancelReceiptsRequestDto
import com.zephyr.boreal.api.dto.request.CreateOrdersRequestDto
import com.zephyr.boreal.api.dto.request.CreateReceiptRequestDataDto
import com.zephyr.boreal.api.dto.request.CreateReceiptsRequestDto
import com.zephyr.boreal.api.dto.request.OrderRequestDataDto
import com.zephyr.boreal.data.local.dao.CacheMetadataDao
import com.zephyr.boreal.data.mapper.toDomain
import com.zephyr.boreal.domain.model.CreatedOrder
import com.zephyr.boreal.domain.model.Receipt
import com.zephyr.boreal.network.ConnectivityObserver
import com.zephyr.boreal.store.user.UserSessionStore
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ReceiptsRepository"
private const val MAX_SERIAL_NUMBER_RETRIES = 5

@Singleton
class ReceiptsRepository
  @Inject
  constructor(
    private val apiService: ReceiptApiService,
    cacheMetadataDao: CacheMetadataDao,
    connectivityObserver: ConnectivityObserver,
    userSessionStore: UserSessionStore,
  ) : BaseRepository(connectivityObserver, userSessionStore, cacheMetadataDao) {
    /**
     * The backend silently skips creating a receipt (returning an empty `data` array with a
     * 200 status) when one already exists for the same company/serialNumber/yearCode — this is
     * an idempotent duplicate guard, not an error. That collision happens when the client's
     * locally-cached receipt list is stale (e.g. after a reinstall) and recomputes a serial
     * number the server has already used. Retry with the next serial number until the server
     * accepts one, rather than surfacing a confusing crash or a permanent dead end.
     */
    @Suppress("ReturnCount")
    suspend fun createReceipt(request: CreateReceiptRequestDataDto): ApiResource<Receipt> {
      var currentRequest = request
      repeat(MAX_SERIAL_NUMBER_RETRIES) {
        try {
          val response = apiService.createReceipt(CreateReceiptsRequestDto(listOf(currentRequest)))
          val created = response.data.firstOrNull()
          if (created != null) {
            return ApiResource.Success(created.toDomain())
          }
          currentRequest = currentRequest.copy(serialNumber = currentRequest.serialNumber + 1)
        } catch (e: Exception) {
          Log.e(TAG, "createReceipt failed", e)
          return ApiResource.Error(e.localizedMessage ?: "Failed to create receipt")
        }
      }
      return ApiResource.Error("A számla sorszáma többszöri próbálkozás után is foglalt.")
    }

    suspend fun cancelReceipt(request: CreateCancelReceiptDataDto): ApiResource<Receipt> =
      try {
        val response = apiService.cancelReceipt(CreateCancelReceiptsRequestDto(listOf(request)))
        val cancelled = response.data.firstOrNull()
        if (cancelled != null) {
          ApiResource.Success(cancelled.toDomain())
        } else {
          ApiResource.Error("A visszavonandó számla nem található.")
        }
      } catch (e: Exception) {
        Log.e(TAG, "cancelReceipt failed", e)
        ApiResource.Error(e.localizedMessage ?: "Failed to cancel receipt")
      }

    suspend fun createOrders(request: List<OrderRequestDataDto>): ApiResource<List<CreatedOrder>> =
      try {
        val response = apiService.createOrders(CreateOrdersRequestDto(request))
        ApiResource.Success(response.data.map { it.toDomain() })
      } catch (e: Exception) {
        Log.e(TAG, "createOrders failed", e)
        ApiResource.Error(e.localizedMessage ?: "Failed to create orders")
      }
  }
