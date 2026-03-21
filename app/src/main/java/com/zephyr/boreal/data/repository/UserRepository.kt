package com.zephyr.boreal.data.repository

import com.zephyr.boreal.api.BorealApiService
import com.zephyr.boreal.data.local.UserDao
import com.zephyr.boreal.data.local.UserEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Domain model for User.
 */
data class User(
  val id: String,
  val name: String,
  val email: String,
)

@Singleton
class UserRepository
  @Inject
  constructor(
    private val apiService: BorealApiService,
    private val userDao: UserDao,
  ) : BaseRepository() {
/**
     * Login action.
     * Simple setter/action wrapper (no persistence required for the action itself).
     */
    suspend fun login(credentials: Any): ApiResource<User> =
      try {
        apiService.login(credentials)
        // In real app, we would map response to User and save to DB
        // val user = response.toDomain()
        // userDao.insertUser(user.toEntity())
        ApiResource.Success(User("1", "Demo User", "user@example.com"))
      } catch (e: Exception) {
        ApiResource.Error(e.localizedMessage ?: "Login failed")
      }

/**
     * Logout action.
     */
    suspend fun logout(): ApiResource<Unit> =
      try {
        userDao.clearUser()
        ApiResource.Success(Unit)
      } catch (e: Exception) {
        ApiResource.Error(e.localizedMessage ?: "Logout failed")
      }

    /**
     * Fetcher for user data with persistence and background refresh.
     */
    fun getCurrentUser(): Flow<ApiResource<User?>> =
      networkBoundResource(
        query = {
          userDao.getUser().map { entity ->
            entity?.let { User(it.id, it.name, it.email) }
          }
        },
        fetch = {
          apiService.checkToken()
          // Simulate fetching updated user details
          UserEntity("1", "Demo User (Updated)", "user@example.com")
        },
        saveFetchResult = { entity ->
          userDao.insertUser(entity)
        },
        shouldFetch = { it != null }, // Example: fetch if we have a cached user to verify token
      )
  }
