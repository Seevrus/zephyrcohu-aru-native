package com.zephyr.boreal.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.io.IOException

/**
 * Base Repository providing common logic for offline-first data fetching.
 */
abstract class BaseRepository {
  /**
   * The "Single Source of Truth" pattern.
   *
   * @param query: Returns data from the local database.
   * @param fetch: Performs the network request.
   * @param saveFetchResult: Saves the network result into the local database.
   * @param shouldFetch: Logic to determine if we should hit the network.
   */
  protected fun <ResultType, RequestType> networkBoundResource(
    query: () -> Flow<ResultType>,
    fetch: suspend () -> RequestType,
    saveFetchResult: suspend (RequestType) -> Unit,
    shouldFetch: (ResultType?) -> Boolean = { true },
  ): Flow<ApiResource<ResultType>> =
    flow {
      // First, emit the current data from the local source
      val data = query().first()

      if (shouldFetch(data)) {
        // If we should fetch, notify UI that we are starting background fetch
        emit(ApiResource.Loading(data).apply { isFetching = true })

        try {
          // Perform the network fetch with retry logic (3 attempts total)
          val result = fetchWithRetry(fetch)

          // Save result to DB
          saveFetchResult(result)

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
      } catch (e: IOException) {
        lastException = e
        if (attempt == maxRetries) {
          throw e
        }
      }
    }
    throw lastException ?: IOException("Unknown network error")
  }
}
