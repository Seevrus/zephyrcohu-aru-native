package com.zephyr.boreal.data.repository

import com.zephyr.boreal.data.local.dao.CacheMetadataDao
import com.zephyr.boreal.network.ConnectivityObserver
import com.zephyr.boreal.store.user.StoredToken
import com.zephyr.boreal.store.user.UserSessionStore
import com.zephyr.boreal.store.user.UserState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.IOException

class BaseRepositoryTest {
  private val connectivityObserver: ConnectivityObserver = mock()
  private val userSessionStore: UserSessionStore = mock()
  private val cacheMetadataDao: CacheMetadataDao = mock()

  private lateinit var repository: TestRepository

  private class TestRepository(
    connectivityObserver: ConnectivityObserver,
    userSessionStore: UserSessionStore,
    cacheMetadataDao: CacheMetadataDao,
  ) : BaseRepository(connectivityObserver, userSessionStore, cacheMetadataDao) {
    fun <ResultType, RequestType> testNetworkBoundResource(
      query: () -> kotlinx.coroutines.flow.Flow<ResultType>,
      fetch: suspend () -> RequestType,
      saveFetchResult: suspend (RequestType) -> Unit,
      shouldFetch: (ResultType?) -> Boolean = { true },
      queryKey: String? = null,
    ) = networkBoundResource(query, fetch, saveFetchResult, shouldFetch, queryKey)
  }

  @BeforeEach
  fun setUp() {
    val userStateFlow =
      MutableStateFlow(
        UserState(
          deviceId = "device-1",
          storedToken = StoredToken("token", false, "2099-01-01T00:00:00Z"),
        ),
      )
    whenever(userSessionStore.userState).thenReturn(userStateFlow)

    val connectivityFlow = MutableStateFlow(true)
    whenever(connectivityObserver.isInternetReachable).thenReturn(connectivityFlow)

    repository = TestRepository(connectivityObserver, userSessionStore, cacheMetadataDao)
  }

  @Test
  fun `should fetch from network when stale and save to DB`() =
    runTest {
      val queryData = "LocalData"
      val networkData = "NetworkData"
      var savedData: String? = null

      whenever(cacheMetadataDao.getFetchedAt("test_key")).thenReturn(null)

      val flow =
        repository.testNetworkBoundResource(
          query = { flowOf(queryData) },
          fetch = { networkData },
          saveFetchResult = { savedData = it },
          queryKey = "test_key",
        )

      val results = flow.toList()

      // 1. Loading(LocalData)
      // 2. Success(LocalData) -> actually it emits from query() after saving.
      // Since query() is flowOf(queryData), it doesn't change when savedData changes in this test.
      // In a real DB, query() would emit the NEW data.

      assertTrue(results[0] is ApiResource.Loading)
      assertEquals(queryData, results[0].getOrNull())
      assertEquals(networkData, savedData)
      verify(cacheMetadataDao).insertCacheMetadata(any())
    }

  @Test
  fun `should NOT fetch from network when NOT stale`() =
    runTest {
      val queryData = "LocalData"
      val currentTime = System.currentTimeMillis()

      whenever(cacheMetadataDao.getFetchedAt("test_key")).thenReturn(currentTime)

      val flow =
        repository.testNetworkBoundResource(
          query = { flowOf(queryData) },
          fetch = { "NetworkData" },
          saveFetchResult = { },
          queryKey = "test_key",
        )

      val results = flow.toList()

      assertEquals(1, results.size)
      assertTrue(results[0] is ApiResource.Success)
      assertEquals(queryData, results[0].getOrNull())
    }

  @Test
  fun `should return error when session is invalid`() =
    runTest {
      whenever(userSessionStore.userState).thenReturn(MutableStateFlow(UserState()))

      val flow =
        repository.testNetworkBoundResource(
          query = { flowOf("LocalData") },
          fetch = { "NetworkData" },
          saveFetchResult = { },
          queryKey = "test_key",
        )

      val results = flow.toList()
      assertTrue(results[0] is ApiResource.Error)
      assertEquals("Invalid session. Please log in.", (results[0] as ApiResource.Error).message)
    }

  @Test
  fun `should return existing data when offline`() =
    runTest {
      whenever(connectivityObserver.isInternetReachable).thenReturn(MutableStateFlow(false))
      whenever(cacheMetadataDao.getFetchedAt("test_key")).thenReturn(null)

      val flow =
        repository.testNetworkBoundResource(
          query = { flowOf("LocalData") },
          fetch = { "NetworkData" },
          saveFetchResult = { },
          queryKey = "test_key",
        )

      val results = flow.toList()
      assertTrue(results[0] is ApiResource.Success)
      assertEquals("LocalData", results[0].getOrNull())
    }

  @Test
  fun `should retry on network error`() =
    runTest {
      whenever(cacheMetadataDao.getFetchedAt("test_key")).thenReturn(null)

      var attempts = 0
      val flow =
        repository.testNetworkBoundResource(
          query = { flowOf("LocalData") },
          fetch = {
            attempts++
            throw IOException("Network failure")
          },
          saveFetchResult = { },
          queryKey = "test_key",
        )

      val results = flow.toList()

      assertEquals(3, attempts) // 1 initial + 2 retries
      assertEquals(2, results.size) // Loading -> Error(LocalData)
      assertTrue(results[0] is ApiResource.Loading)
      assertTrue(results[1] is ApiResource.Error)
      assertEquals("LocalData", results[1].getOrNull())
    }
}
