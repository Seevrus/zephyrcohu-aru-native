package com.zephyr.boreal.store.user

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.zephyr.boreal.store.core.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

data class LoginSettingsState(
  val lastCompanyCode: String? = null,
  val lastUsername: String? = null,
)

@Singleton
class LoginSettingsStore
  @Inject
  constructor(
    private val dataStore: DataStore<Preferences>,
    @param:ApplicationScope private val scope: CoroutineScope,
  ) {
    companion object {
      val LAST_COMPANY_CODE = stringPreferencesKey("last_company_code")
      val LAST_USERNAME = stringPreferencesKey("last_username")
    }

    val loginSettingsState: StateFlow<LoginSettingsState> =
      dataStore.data
        .map { preferences ->
          val lastCompanyCode = preferences[LAST_COMPANY_CODE]
          val lastUsername = preferences[LAST_USERNAME]

          LoginSettingsState(
            lastCompanyCode = lastCompanyCode,
            lastUsername = lastUsername,
          )
        }.stateIn(
          scope = scope,
          started = SharingStarted.Eagerly,
          initialValue = LoginSettingsState(),
        )

    suspend fun updateSettings(
      companyCode: String?,
      username: String?,
    ) {
      dataStore.edit { preferences ->
        if (companyCode != null) {
          preferences[LAST_COMPANY_CODE] = companyCode
        } else {
          preferences.remove(LAST_COMPANY_CODE)
        }

        if (username != null) {
          preferences[LAST_USERNAME] = username
        } else {
          preferences.remove(LAST_USERNAME)
        }
      }
    }
  }
