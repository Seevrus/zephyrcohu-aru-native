package com.zephyr.boreal.data.repository

import com.zephyr.boreal.api.ItemApiService
import com.zephyr.boreal.api.dto.request.SaveSelectedItemsRequestDto
import com.zephyr.boreal.api.dto.request.SellSelectedItemsRequestDto
import com.zephyr.boreal.data.local.dao.ItemDao
import com.zephyr.boreal.data.local.dao.OtherItemDao
import com.zephyr.boreal.data.mapper.toDomain
import com.zephyr.boreal.data.mapper.toEntity
import com.zephyr.boreal.domain.model.Item
import com.zephyr.boreal.domain.model.OtherItem
import com.zephyr.boreal.domain.model.PriceList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ItemsRepository
  @Inject
  constructor(
    private val apiService: ItemApiService,
    private val itemDao: ItemDao,
    private val otherItemDao: OtherItemDao,
  ) : BaseRepository() {
    fun getItems(): Flow<ApiResource<List<Item>>> =
      networkBoundResource(
        query = {
          itemDao.getAllItems().map { entities ->
            entities.map { it.toDomain() }
          }
        },
        fetch = {
          apiService.getItems().data
        },
        saveFetchResult = { dtos ->
          itemDao.insertItems(dtos.map { it.toEntity() })
        },
      )

    fun getOtherItems(): Flow<ApiResource<List<OtherItem>>> =
      networkBoundResource(
        query = {
          otherItemDao.getAllOtherItems().map { entities ->
            entities.map { it.toDomain() }
          }
        },
        fetch = {
          apiService.getOtherItems().data
        },
        saveFetchResult = { dtos ->
          otherItemDao.insertOtherItems(dtos.map { it.toEntity() })
        },
      )

    suspend fun getPriceLists(): ApiResource<List<PriceList>> =
      try {
        val response = apiService.getPriceLists()
        ApiResource.Success(response.data.map { it.toDomain() })
      } catch (e: Exception) {
        ApiResource.Error(e.localizedMessage ?: "Failed to fetch price list")
      }

    suspend fun saveSelectedItems(request: SaveSelectedItemsRequestDto): ApiResource<Unit> =
      try {
        apiService.saveSelectedItems(request)
        ApiResource.Success(Unit)
      } catch (e: Exception) {
        ApiResource.Error(e.localizedMessage ?: "Failed to save selected items")
      }

    suspend fun sellSelectedItems(request: SellSelectedItemsRequestDto): ApiResource<Unit> =
      try {
        apiService.sellSelectedItems(request)
        ApiResource.Success(Unit)
      } catch (e: Exception) {
        ApiResource.Error(e.localizedMessage ?: "Failed to sell selected items")
      }
  }
