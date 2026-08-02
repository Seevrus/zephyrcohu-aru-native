package com.zephyr.boreal.data.repository

import android.content.Context
import com.zephyr.boreal.R
import com.zephyr.boreal.api.StoreApiService
import com.zephyr.boreal.api.dto.request.SelectStoreDataDto
import com.zephyr.boreal.api.dto.request.SelectStoreRequestDto
import com.zephyr.boreal.data.local.converters.CacheGroups
import com.zephyr.boreal.data.local.dao.CacheMetadataDao
import com.zephyr.boreal.data.local.dao.StoreDao
import com.zephyr.boreal.data.local.dao.StoreDetailsDao
import com.zephyr.boreal.data.mapper.toDomain
import com.zephyr.boreal.data.mapper.toEntity
import com.zephyr.boreal.domain.model.Store
import com.zephyr.boreal.domain.model.StoreDetails
import com.zephyr.boreal.domain.model.User
import com.zephyr.boreal.network.ConnectivityObserver
import com.zephyr.boreal.store.user.UserSessionStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StoresRepository
  @Inject
  constructor(
    private val apiService: StoreApiService,
    private val storeDao: StoreDao,
    private val storeDetailsDao: StoreDetailsDao,
    connectivityObserver: ConnectivityObserver,
    userSessionStore: UserSessionStore,
    cacheMetadataDao: CacheMetadataDao,
    @param:ApplicationContext private val context: Context,
  ) : BaseRepository(connectivityObserver, userSessionStore, cacheMetadataDao) {
    companion object {
      private const val HTTP_STATUS_FORBIDDEN = 403
      private const val HTTP_STATUS_NOT_FOUND = 404
      private const val HTTP_STATUS_UNPROCESSABLE_ENTITY = 422
      private const val HTTP_STATUS_INSUFFICIENT_STORAGE = 507
    }

    fun getStores(forceRefresh: Boolean = false): Flow<ApiResource<List<Store>>> =
      networkBoundResource(
        query = {
          storeDao.getAllStores().map { entities ->
            entities.map { it.toDomain() }
          }
        },
        fetch = {
          apiService.getStores().data
        },
        saveFetchResult = { dtos ->
          storeDao.insertStores(dtos.map { it.toEntity() })
        },
        queryKey = "get_stores",
        cacheTimeoutMillis = if (forceRefresh) 0L else DEFAULT_CACHE_TIMEOUT_MS,
      )

    suspend fun selectStore(storeId: Int): ApiResource<User> =
      try {
        val response = apiService.selectStore(SelectStoreRequestDto(SelectStoreDataDto(storeId)))
        if (response.storeInUseId != storeId) {
          throw Exception("Invalid Store ID")
        }
        ApiResource.Success(response.toDomain())
      } catch (e: HttpException) {
        ApiResource.Error(selectStoreErrorMessage(e))
      } catch (e: Exception) {
        ApiResource.Error(e.localizedMessage ?: context.getString(R.string.select_store_error_unexpected))
      }

    private fun selectStoreErrorMessage(e: HttpException): String =
      when (e.code()) {
        HTTP_STATUS_FORBIDDEN -> context.getString(R.string.select_store_error_forbidden)
        HTTP_STATUS_NOT_FOUND -> context.getString(R.string.select_store_error_not_found)
        HTTP_STATUS_UNPROCESSABLE_ENTITY -> context.getString(R.string.select_store_error_primary_forbidden)
        HTTP_STATUS_INSUFFICIENT_STORAGE -> selectStore507ErrorMessage(e)
        else -> context.getString(R.string.select_store_error_unexpected)
      }

    private fun selectStore507ErrorMessage(e: HttpException): String {
      val backendMessage =
        try {
          JSONObject(
            e
              .response()
              ?.errorBody()
              ?.string()
              .orEmpty(),
          ).optString("message")
        } catch (
          @Suppress("SwallowedException") jsonException: Exception,
        ) {
          null
        }
      return when {
        backendMessage?.contains("already has a store", ignoreCase = true) == true ->
          context.getString(R.string.select_store_error_already_has_store)
        backendMessage?.contains("already locked", ignoreCase = true) == true ->
          context.getString(R.string.select_store_error_store_taken)
        else -> context.getString(R.string.select_store_error_unexpected)
      }
    }

    suspend fun deselectStore(): ApiResource<Unit> =
      try {
        apiService.deselectStore()
        ApiResource.Success(Unit)
      } catch (e: Exception) {
        ApiResource.Error(e.localizedMessage ?: "Failed to deselect store")
      }

    suspend fun getStoreDetails(
      storeId: Int,
      forceRefresh: Boolean = false,
    ): ApiResource<StoreDetails> {
      val flow =
        networkBoundResource(
          query = {
            storeDetailsDao.getStoreDetails(storeId).map { entity ->
              entity?.toDomain()
            }
          },
          fetch = {
            apiService.getStoreDetails(storeId).data
          },
          saveFetchResult = { dto ->
            storeDetailsDao.insertStoreDetails(dto.toEntity())
          },
          queryKey = "get_store_details_$storeId",
          cacheTimeoutMillis = if (forceRefresh) 0L else DEFAULT_CACHE_TIMEOUT_MS,
          degradeGroup = CacheGroups.STORE_DETAILS,
        )

      return when (val result = flow.first { it !is ApiResource.Loading }) {
        is ApiResource.Success -> {
          val data = result.data
          if (data != null) {
            ApiResource.Success(data)
          } else {
            ApiResource.Error("Store details not found")
          }
        }
        is ApiResource.Error -> ApiResource.Error(result.message, result.data)
        is ApiResource.Loading -> ApiResource.Loading()
      }
    }
  }
