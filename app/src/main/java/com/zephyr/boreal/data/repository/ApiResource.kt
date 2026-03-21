package com.zephyr.boreal.data.repository

/**
 * A generic class that holds a value with its loading status.
 * @param <T>
 */
sealed class ApiResource<out T> {
  data class Success<out T>(
    val data: T,
  ) : ApiResource<T>()

  data class Error<out T>(
    val message: String,
    val data: T? = null,
  ) : ApiResource<T>()

  data class Loading<out T>(
    val data: T? = null,
  ) : ApiResource<T>()

  /**
   * Status-only getters for convenience
   */
  val isLoading: Boolean get() = this is Loading
  val isError: Boolean get() = this is Error
  val isSuccess: Boolean get() = this is Success

  /**
   * [isFetching] represents if a background network request is currently active,
   * even if we already have (stale) data.
   */
  var isFetching: Boolean = false
    internal set

  /**
   * Returns the data if available, regardless of status.
   */
  fun getOrNull(): T? =
    when (this) {
      is Success -> data
      is Error -> data
      is Loading -> data
    }
}
