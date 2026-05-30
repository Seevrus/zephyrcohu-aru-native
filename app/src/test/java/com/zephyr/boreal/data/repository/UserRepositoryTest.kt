package com.zephyr.boreal.data.repository

import com.zephyr.boreal.api.AuthApiService
import com.zephyr.boreal.api.dto.response.LoginResponseDto
import com.zephyr.boreal.api.dto.response.PartialCompanyDto
import com.zephyr.boreal.api.dto.response.TokenDto
import com.zephyr.boreal.data.local.dao.CacheMetadataDao
import com.zephyr.boreal.data.local.dao.UserDao
import com.zephyr.boreal.domain.model.UserRole
import com.zephyr.boreal.domain.model.UserState
import com.zephyr.boreal.network.ConnectivityObserver
import com.zephyr.boreal.store.user.UserSessionStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class UserRepositoryTest {
  private lateinit var repository: UserRepository
  private val apiService: AuthApiService = mock()
  private val userDao: UserDao = mock()
  private val userSessionStore: UserSessionStore = mock()
  private val connectivityObserver: ConnectivityObserver = mock()
  private val cacheMetadataDao: CacheMetadataDao = mock()
  private val context: android.content.Context = mock()

  private val mockLoginResponse =
    LoginResponseDto(
      id = 1,
      userName = "test@company",
      state = UserState.IDLE,
      name = "Test User",
      phoneNumber = null,
      isDev = false,
      roles = listOf(UserRole.ADMIN),
      storeInUseId = null,
      storeOwnedId = null,
      lastActive = "2026-04-03T10:00:00Z",
      createdAt = "2026-04-03T09:00:00Z",
      updatedAt = "2026-04-03T09:00:00Z",
      company = PartialCompanyDto(3, "Company 3"),
      token = TokenDto("Bearer", "mocked_token", listOf(UserRole.ADMIN)),
    )

  @BeforeEach
  fun setUp() {
    val stateFlow =
      MutableStateFlow(
        com.zephyr.boreal.store.user.UserState(
          deviceId = "test-device",
          storedToken =
            com.zephyr.boreal.store.user.StoredToken(
              token = "mock_token",
              isPasswordExpired = false,
              expiresAt = "2099-01-01T00:00:00Z",
            ),
        ),
      )
    whenever(userSessionStore.userState).thenReturn(stateFlow)
    val connectivityFlow = MutableStateFlow(true)
    whenever(connectivityObserver.isInternetReachable).thenReturn(connectivityFlow)
    whenever(context.getString(any())).thenReturn("Error")
    repository =
      UserRepository(
        apiService,
        userDao,
        connectivityObserver,
        userSessionStore,
        cacheMetadataDao,
        context,
      )
  }

  @Test
  fun `login should update session and save to dao on success`() =
    runTest {
      whenever(apiService.login(any(), any())).thenReturn(mockLoginResponse)

      val result = repository.login("company", "test", "password")

      assertTrue(result is ApiResource.Success)
      verify(userSessionStore).updateSession(any(), any())
      verify(userDao).insertUser(any())
    }

  @Test
  fun `login should return error on failure`() =
    runTest {
      whenever(apiService.login(any(), any())).thenThrow(RuntimeException("Network error"))

      val result = repository.login("company", "test", "password")

      assertTrue(result is ApiResource.Error)
      assertEquals("Network error", (result as ApiResource.Error).message)
    }

  @Test
  fun `logout should clear session, dao and cache metadata`() =
    runTest {
      val result = repository.logout()

      assertTrue(result is ApiResource.Success)
      verify(apiService).logout()
      verify(userSessionStore).clearSession()
      verify(userDao).clearUser()
      verify(cacheMetadataDao).clearCacheMetadata("get_current_user")
    }

  @Test
  fun `changePassword should update session and clear local cache on success`() =
    runTest {
      whenever(apiService.changePassword(any())).thenReturn(mockLoginResponse)

      val result = repository.changePassword("newPassword")

      assertTrue(result is ApiResource.Success)
      verify(apiService).changePassword(any())
      verify(userSessionStore).updateSession(any(), any())
      verify(userDao).clearUser()
      verify(cacheMetadataDao).clearCacheMetadata("get_current_user")
    }

  @Test
  fun `changePassword should logout and return error on 401`() =
    runTest {
      val responseBody = "Unauthorized".toResponseBody("application/json".toMediaType())
      val error = retrofit2.HttpException(retrofit2.Response.error<Any>(401, responseBody))
      whenever(apiService.changePassword(any())).thenThrow(error)

      val result = repository.changePassword("newPassword")

      assertTrue(result is ApiResource.Error)
      verify(userSessionStore).clearSession()
      verify(userDao).clearUser()
    }

  private val mockCheckTokenResponse =
    com.zephyr.boreal.api.dto.response.CheckTokenResponseDto(
      id = 1,
      userName = "test@company",
      state = UserState.IDLE,
      name = "Test User",
      phoneNumber = null,
      isDev = false,
      roles = listOf(UserRole.ADMIN),
      storeInUseId = null,
      storeOwnedId = null,
      lastActive = "2026-04-03T10:00:00Z",
      createdAt = "2026-04-03T09:00:00Z",
      updatedAt = "2026-04-03T09:00:00Z",
      company =
        com.zephyr.boreal.api.dto.response.CompanyDto(
          id = 3,
          code = "CODE",
          name = "Company 3",
          country = "HU",
          postalCode = "1234",
          city = "City",
          address = "Address",
          felir = "FELIR",
          vatNumber = "12345678-1-11",
          iban = "IBAN",
          bankAccount = "BANK",
        ),
      token = TokenDto("Bearer", "mocked_token", listOf(UserRole.ADMIN)),
    )

  @Test
  fun `refreshCurrentUser should fetch from network, save to dao, and return success`() =
    runTest {
      whenever(apiService.checkToken()).thenReturn(mockCheckTokenResponse)

      val result = repository.refreshCurrentUser()

      assertTrue(result is ApiResource.Success)
      verify(apiService).checkToken()
      verify(userDao).insertUser(any())
    }

  @Test
  fun `refreshCurrentUser should return error on failure`() =
    runTest {
      val responseBody = "Unauthorized".toResponseBody("application/json".toMediaType())
      val error = retrofit2.HttpException(retrofit2.Response.error<Any>(401, responseBody))
      whenever(apiService.checkToken()).thenThrow(error)

      val result = repository.refreshCurrentUser()

      assertTrue(result is ApiResource.Error)
      verify(apiService).checkToken()
    }
}
