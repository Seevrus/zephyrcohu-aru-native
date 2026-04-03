package com.zephyr.boreal.data.repository

import com.zephyr.boreal.api.AuthApiService
import com.zephyr.boreal.api.dto.request.LoginRequestDto
import com.zephyr.boreal.data.local.dao.UserDao
import com.zephyr.boreal.data.mapper.toDomain
import com.zephyr.boreal.data.mapper.toEntity
import com.zephyr.boreal.domain.model.User
import com.zephyr.boreal.domain.model.UserRole
import com.zephyr.boreal.store.user.StoredToken
import com.zephyr.boreal.store.user.UserSessionStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository
  @Inject
  constructor(
    private val apiService: AuthApiService,
    private val userDao: UserDao,
    private val userSessionStore: UserSessionStore,
  ) : BaseRepository() {
    suspend fun login(
      company: String,
      username: String,
      password: String,
    ): ApiResource<User> =
      try {
        val credentials = LoginRequestDto(userName = "$username@$company", password = password)
        val response = apiService.login(credentials)

        // Save to store immediately as per plan
        userSessionStore.updateSession(
          deviceId = userSessionStore.userState.value.deviceId, // Preserve device ID
          token =
            StoredToken(
              token = response.token.accessToken,
              isPasswordExpired = response.token.abilities.contains(UserRole.PASSWORD_EXPIRED),
              expiresAt = response.token.expiresAt ?: "",
            ),
        )

        // Save to Room
        userDao.insertUser(response.toEntity())

        ApiResource.Success(response.toDomain())
      } catch (e: Exception) {
        ApiResource.Error(e.localizedMessage ?: "Login failed")
      }

    suspend fun logout(): ApiResource<Unit> =
      try {
        apiService.logout()
        userDao.clearUser()
        userSessionStore.clearSession()
        ApiResource.Success(Unit)
      } catch (e: Exception) {
        // Even if API fails, we clear local session
        userDao.clearUser()
        userSessionStore.clearSession()
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

          // Update session if needed (e.g., password expired)
          userSessionStore.updateSession(
            deviceId = userSessionStore.userState.value.deviceId,
            token =
              StoredToken(
                token = response.token.accessToken,
                isPasswordExpired = response.token.abilities.contains(UserRole.PASSWORD_EXPIRED),
                expiresAt = response.token.expiresAt ?: "",
              ),
          )
          response
        },
        saveFetchResult = { response ->
          userDao.insertUser(response.toEntity())
        },
        shouldFetch = { it != null },
      )
  }
