package com.zephyr.boreal.data.repository

import com.zephyr.boreal.api.RoundApiService
import com.zephyr.boreal.api.dto.request.FinishRoundRequestDataDto
import com.zephyr.boreal.api.dto.request.FinishRoundRequestDto
import com.zephyr.boreal.api.dto.request.StartRoundRequestDataDto
import com.zephyr.boreal.api.dto.request.StartRoundRequestDto
import com.zephyr.boreal.data.local.dao.CacheMetadataDao
import com.zephyr.boreal.data.local.dao.RoundDao
import com.zephyr.boreal.data.mapper.toDomain
import com.zephyr.boreal.data.mapper.toEntity
import com.zephyr.boreal.domain.model.Round
import com.zephyr.boreal.network.ConnectivityObserver
import com.zephyr.boreal.store.user.UserSessionStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoundsRepository
  @Inject
  constructor(
    private val apiService: RoundApiService,
    private val roundDao: RoundDao,
    connectivityObserver: ConnectivityObserver,
    userSessionStore: UserSessionStore,
    cacheMetadataDao: CacheMetadataDao,
  ) : BaseRepository(connectivityObserver, userSessionStore, cacheMetadataDao) {
    fun getRounds(): Flow<ApiResource<List<Round>>> =
      networkBoundResource(
        query = {
          roundDao.getAllRounds().map { entities ->
            entities
              .map { it.toDomain() }
              .filter { it.roundFinished != null }
              .sortedByDescending { it.roundFinished }
          }
        },
        fetch = {
          apiService.getRounds().data
        },
        saveFetchResult = { dtos ->
          roundDao.insertRounds(dtos.map { it.toEntity() })
        },
        queryKey = "get_rounds",
      )

    suspend fun startRound(request: StartRoundRequestDataDto): ApiResource<Round> =
      try {
        val response = apiService.startRound(StartRoundRequestDto(request))
        roundDao.insertRounds(listOf(response.data.toEntity()))
        ApiResource.Success(response.data.toDomain())
      } catch (e: Exception) {
        ApiResource.Error(e.localizedMessage ?: "Failed to start round")
      }

    suspend fun finishRound(request: FinishRoundRequestDataDto): ApiResource<Round> =
      try {
        val response = apiService.finishRound(FinishRoundRequestDto(request))
        roundDao.insertRounds(listOf(response.data.toEntity()))
        ApiResource.Success(response.data.toDomain())
      } catch (e: Exception) {
        ApiResource.Error(e.localizedMessage ?: "Failed to finish round")
      }
  }
