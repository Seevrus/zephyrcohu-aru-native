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
      // First, emit the current data from the local source
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
          // If session is invalid, don't attempt network fetch, just return error or existing data
          emitAll(query().map { ApiResource.Error("Invalid session. Please log in.", it) })
          return@flow
        }

        if (!isInternetReachable) {
          // If offline, emit local data and optionally an error or keep it as success/loading based on needs.
          // For now, emit local data as Success, as offline should allow usage.
          emitAll(query().map { ApiResource.Success(it) })
          return@flow
        }

        // If we should fetch, notify UI that we are starting background fetch
        emit(ApiResource.Loading(data).apply { isFetching = true })

        try {
          // Perform the network fetch with retry logic (3 attempts total)
          val result = fetchWithRetry(fetch)

          // Save result to DB
          saveFetchResult(result)

          // Update cache metadata
          if (queryKey != null) {
            cacheMetadataDao.insertCacheMetadata(CacheMetadataEntity(queryKey, currentTime))
          }

          // Emit the final success from the new DB content
          emitAll(query().map { ApiResource.Success(it) })
        } catch (throwable: Exception) {
          // If network fails, emit Error with the previous data we had
          emitAll(query().map { ApiResource.Error(throwable.localizedMessage ?: "Unknown Error", it) })
        }
      } else {
        // If no fetch is needed, just emit the current DB data as success
        emitAll(query().map { ApiResource.Success(it) })
      }
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
