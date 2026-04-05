package com.zephyr.boreal.data.repository

import com.zephyr.boreal.api.StoreApiService
import com.zephyr.boreal.api.dto.request.SelectStoreDataDto
import com.zephyr.boreal.api.dto.request.SelectStoreRequestDto
import com.zephyr.boreal.data.local.dao.CacheMetadataDao
import com.zephyr.boreal.data.local.dao.StoreDao
import com.zephyr.boreal.data.mapper.toDomain
import com.zephyr.boreal.data.mapper.toEntity
import com.zephyr.boreal.domain.model.Store
import com.zephyr.boreal.domain.model.User
import com.zephyr.boreal.network.ConnectivityObserver
import com.zephyr.boreal.store.user.UserSessionStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StoresRepository
  @Inject
  constructor(
    private val apiService: StoreApiService,
    private val storeDao: StoreDao,
    connectivityObserver: ConnectivityObserver,
    userSessionStore: UserSessionStore,
    cacheMetadataDao: CacheMetadataDao,
  ) : BaseRepository(connectivityObserver, userSessionStore, cacheMetadataDao) {
    fun getStores(): Flow<ApiResource<List<Store>>> =
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
      )

    suspend fun selectStore(storeId: Int): ApiResource<User> =
      try {
        val response = apiService.selectStore(SelectStoreRequestDto(SelectStoreDataDto(storeId)))
        if (response.storeInUseId != storeId) {
          throw Exception("Invalid Store ID")
        }
        ApiResource.Success(response.toDomain())
      } catch (e: Exception) {
        ApiResource.Error(e.localizedMessage ?: "Failed to select store")
      }

    suspend fun deselectStore(): ApiResource<Unit> =
      try {
        apiService.deselectStore()
        ApiResource.Success(Unit)
      } catch (e: Exception) {
        ApiResource.Error(e.localizedMessage ?: "Failed to deselect store")
      }
  }
