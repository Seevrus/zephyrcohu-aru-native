package com.zephyr.boreal.data.repository

import android.content.Context
import com.zephyr.boreal.R
import com.zephyr.boreal.api.AuthApiService
import com.zephyr.boreal.api.dto.request.LoginRequestDto
import com.zephyr.boreal.data.local.dao.CacheMetadataDao
import com.zephyr.boreal.data.local.dao.UserDao
import com.zephyr.boreal.data.mapper.toDomain
import com.zephyr.boreal.data.mapper.toEntity
import com.zephyr.boreal.domain.model.User
import com.zephyr.boreal.domain.model.UserRole
import com.zephyr.boreal.network.ConnectivityObserver
import com.zephyr.boreal.store.user.StoredToken
import com.zephyr.boreal.store.user.UserSessionStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository
  @Inject
  constructor(
    private val apiService: AuthApiService,
    private val userDao: UserDao,
    connectivityObserver: ConnectivityObserver,
    userSessionStore: UserSessionStore,
    cacheMetadataDao: CacheMetadataDao,
    @param:ApplicationContext private val context: Context,
  ) : BaseRepository(connectivityObserver, userSessionStore, cacheMetadataDao) {
    suspend fun login(
      company: String,
      username: String,
      password: String,
    ): ApiResource<User> =
      try {
        val deviceId = UUID.randomUUID().toString()
        val credentials = LoginRequestDto(userName = "$username@$company", password = password)
        val response = apiService.login(deviceId, credentials)

        // Save session data (including the device ID used) upon success
        userSessionStore.updateSession(
          deviceId = deviceId,
          token =
            StoredToken(
              token = response.token.accessToken,
              isPasswordExpired = response.token.abilities.contains(UserRole.PASSWORD_EXPIRED),
              expiresAt = response.token.expiresAt,
            ),
        )

        // Save to Room
        userDao.insertUser(response.toEntity())

        ApiResource.Success(response.toDomain())
      } catch (e: retrofit2.HttpException) {
        val errorMessage =
          when (e.code()) {
            401 -> context.getString(R.string.login_error_invalid_credentials)
            422 -> context.getString(R.string.login_error_invalid_format)
            423 -> context.getString(R.string.login_error_already_logged_in)
            429 -> context.getString(R.string.login_error_locked)
            else -> e.response()?.errorBody()?.string() ?: context.getString(R.string.login_error_unexpected)
          }
        ApiResource.Error(errorMessage)
      } catch (e: Exception) {
        ApiResource.Error(e.localizedMessage ?: context.getString(R.string.login_error_unexpected))
      }

    suspend fun logout(): ApiResource<Unit> =
      try {
        apiService.logout()
        userDao.clearUser()
        userSessionStore.clearSession()
        cacheMetadataDao.clearCacheMetadata("get_current_user")
        ApiResource.Success(Unit)
      } catch (e: Exception) {
        // Even if API fails, we clear local session
        userDao.clearUser()
        userSessionStore.clearSession()
        cacheMetadataDao.clearCacheMetadata("get_current_user")
        ApiResource.Error(e.localizedMessage ?: "Logout failed")
      }

    fun getCurrentUser(): Flow<ApiResource<User?>> =
      networkBoundResource(
        query = {
          userDao.getUser().map { entity ->
            entity?.toDomain()
          }
        },
        fetch = {
          val response = apiService.checkToken()

          // Update session if needed (e.g., password expired), preserving current device ID
          userSessionStore.updateSession(
            deviceId = userSessionStore.userState.value.deviceId,
            token =
              StoredToken(
                token = response.token.accessToken,
                isPasswordExpired = response.token.abilities.contains(UserRole.PASSWORD_EXPIRED),
                expiresAt = response.token.expiresAt,
              ),
          )
          response
        },
        saveFetchResult = { response ->
          userDao.insertUser(response.toEntity())
        },
        shouldFetch = { it != null },
        queryKey = "get_current_user",
      )
  }
