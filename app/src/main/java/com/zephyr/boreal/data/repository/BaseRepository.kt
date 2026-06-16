package com.zephyr.boreal.data.repository

import com.zephyr.boreal.data.local.CacheMetadataEntity
import com.zephyr.boreal.data.local.dao.CacheMetadataDao
import com.zephyr.boreal.network.ConnectivityObserver
import com.zephyr.boreal.store.user.UserSessionStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import java.io.IOException

/**
 * Base Repository providing common logic for offline-first data fetching.
 */
abstract class BaseRepository(
  protected val connectivityObserver: ConnectivityObserver,
  protected val userSessionStore: UserSessionStore,
  protected val cacheMetadataDao: CacheMetadataDao,
) {
  companion object {
    const val DEFAULT_CACHE_TIMEOUT_MS = 300000L
  }

  /**
   * The "Single Source of Truth" pattern.
   *
   * @param query: Returns data from the local database.
   * @param fetch: Performs the network request.
   * @param saveFetchResult: Saves the network result into the local database.
   * @param shouldFetch: Logic to determine if we should hit the network.
   * @param queryKey: Key for cache metadata tracking.
   * @param cacheTimeoutMillis: Time after which cache is considered stale (default 5 mins).
   */
  protected fun <ResultType, RequestType> networkBoundResource(
    query: () -> Flow<ResultType>,
    fetch: suspend () -> RequestType,
    saveFetchResult: suspend (RequestType) -> Unit,
    shouldFetch: (ResultType?) -> Boolean = { true },
    queryKey: String? = null,
    cacheTimeoutMillis: Long = DEFAULT_CACHE_TIMEOUT_MS,
  ): Flow<ApiResource<ResultType>> =
    flow {
      val data = query().first()

      val isInternetReachable = connectivityObserver.isInternetReachable.value
      val userState = userSessionStore.userState.value
      val hasValidSession =
        userState.storedToken?.token != null && !userState.storedToken.isPasswordExpired && userState.deviceId != null

      val currentTime = System.currentTimeMillis()
      val isStale =
        if (queryKey != null) {
          val fetchedAt = cacheMetadataDao.getFetchedAt(queryKey)
          fetchedAt == null || (currentTime - fetchedAt) >= cacheTimeoutMillis
        } else {
          shouldFetch(data)
        }

      if (isStale) {
        if (!hasValidSession) {
          emit(ApiResource.Error("Invalid session. Please log in.", data))
          return@flow
        }

        if (!isInternetReachable) {
          emit(ApiResource.Success(data))
          return@flow
        }

        emit(ApiResource.Loading(data).apply { isFetching = true })

        try {
          val result = fetchWithRetry(fetch)
          saveFetchResult(result)

          if (queryKey != null) {
            cacheMetadataDao.insertCacheMetadata(CacheMetadataEntity(queryKey, currentTime))
          }
        } catch (throwable: Exception) {
          // Emit error, then we will continue to emit the DB flow with Error state
          emitAll(
            query().map {
              ApiResource.Error(throwable.localizedMessage ?: "Unknown Error", it)
            },
          )
          return@flow
        }
      }

      // Defer to the local database as the single source of truth for ongoing updates
      emitAll(
        query().map {
          ApiResource.Success(it)
        },
      )
    }

  /**
   * Helper for retrying network operations.
   */
  private suspend fun <T> fetchWithRetry(
    fetch: suspend () -> T,
    // 1 original + 2 retries = 3 attempts
    maxRetries: Int = 2,
  ): T {
    var lastException: Exception? = null
    repeat(maxRetries + 1) { attempt ->
      try {
        return fetch()
      } catch (e: Exception) {
        lastException = e
        val isRetryable =
          when (e) {
            is IOException -> true
            is HttpException -> {
              // Retry on 5XX errors or timeouts (e.g. 408)
              e.code() in 500..599 || e.code() == 408 || e.code() == 429
            }
            else -> false
          }
        if (!isRetryable || attempt == maxRetries) {
          throw e
        }
      }
    }
    throw lastException ?: IOException("Unknown network error")
  }
}
