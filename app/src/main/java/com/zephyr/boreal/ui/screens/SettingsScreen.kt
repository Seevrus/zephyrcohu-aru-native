package com.zephyr.boreal.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zephyr.boreal.R
import com.zephyr.boreal.ui.components.BorealTile
import com.zephyr.boreal.ui.components.BorealTopAppBar
import com.zephyr.boreal.ui.components.InfoCard
import com.zephyr.boreal.ui.components.LoadingIndicator
import com.zephyr.boreal.ui.components.TileVariant
import com.zephyr.boreal.ui.theme.BorealColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
  modifier: Modifier = Modifier,
  onNavigateToLogin: () -> Unit = {},
  onNavigateToChangePassword: () -> Unit = {},
  onNavigateToPrintSettings: () -> Unit = {},
  onLogout: () -> Unit = {},
  viewModel: SettingsViewModel = hiltViewModel(),
) {
  val state by viewModel.state.collectAsState()

  SettingsScreenContent(
    isLoggedIn = state.isLoggedIn,
    isIdle = state.isIdle,
    isLoading = state.isLoading,
    isInternetReachable = state.isInternetReachable,
    onNavigateToLogin = onNavigateToLogin,
    onNavigateToChangePassword = onNavigateToChangePassword,
    onNavigateToPrintSettings = onNavigateToPrintSettings,
    onLogout = { viewModel.logout(onLogout) },
    modifier = modifier,
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenContent(
  isLoggedIn: Boolean,
  isIdle: Boolean,
  isLoading: Boolean,
  isInternetReachable: Boolean,
  onNavigateToLogin: () -> Unit = {},
  onNavigateToChangePassword: () -> Unit = {},
  onNavigateToPrintSettings: () -> Unit = {},
  onLogout: () -> Unit = {},
  modifier: Modifier = Modifier,
) {
  val tiles =
    getSettingsTiles(
      isLoggedIn = isLoggedIn,
      isIdle = isIdle,
      isInternetReachable = isInternetReachable,
    )

  Scaffold(
    modifier = modifier,
    topBar = {
      BorealTopAppBar(
        title = stringResource(R.string.screen_settings_title),
      )
    },
    containerColor = BorealColors.Background,
  ) { innerPadding ->
    if (isLoading) {
      Box(
        modifier =
          Modifier
            .padding(innerPadding)
            .fillMaxSize(),
        contentAlignment = Alignment.Center,
      ) {
        LoadingIndicator()
      }
    } else {
      SettingsScreenList(
        isInternetReachable = isInternetReachable,
        tiles = tiles,
        onNavigateToLogin = onNavigateToLogin,
        onNavigateToChangePassword = onNavigateToChangePassword,
        onNavigateToPrintSettings = onNavigateToPrintSettings,
        onLogout = onLogout,
        modifier = Modifier.padding(innerPadding),
      )
    }
  }
}

@Composable
private fun SettingsScreenList(
  isInternetReachable: Boolean,
  tiles: List<SettingsTileData>,
  onNavigateToLogin: () -> Unit,
  onNavigateToChangePassword: () -> Unit,
  onNavigateToPrintSettings: () -> Unit,
  onLogout: () -> Unit,
  modifier: Modifier = Modifier,
) {
  LazyColumn(
    contentPadding = PaddingValues(16.dp),
    modifier = modifier.fillMaxSize(),
  ) {
    if (!isInternetReachable) {
      item {
        InfoCard(
          message = stringResource(R.string.offline_warning),
          modifier = Modifier.padding(bottom = 16.dp),
        )
      }
    }

    items(tiles) { tile ->
      val onClick =
        when (tile.title) {
          stringResource(R.string.tile_login) -> onNavigateToLogin
          stringResource(R.string.tile_change_password) -> onNavigateToChangePassword
          stringResource(R.string.tile_print_settings) -> onNavigateToPrintSettings
          stringResource(R.string.tile_logout) -> onLogout
          else -> {
            {}
          }
        }

      val finalOnClick = if (tile.variant == TileVariant.DISABLED) ({}) else onClick

      BorealTile(
        title = tile.title,
        variant = tile.variant,
        modifier = Modifier.padding(bottom = 16.dp),
        onClick = finalOnClick,
      )
    }
  }
}

data class SettingsTileData(
  val title: String,
  val variant: TileVariant = TileVariant.NEUTRAL,
)

@Composable
fun getSettingsTiles(
  isLoggedIn: Boolean,
  isIdle: Boolean,
  isInternetReachable: Boolean,
): List<SettingsTileData> {
  val tiles = mutableListOf<SettingsTileData>()

  if (!isLoggedIn) {
    tiles.add(
      SettingsTileData(
        title = stringResource(R.string.tile_login),
        variant = if (isInternetReachable) TileVariant.NEUTRAL else TileVariant.DISABLED,
      ),
    )
  } else {
    tiles.add(
      SettingsTileData(
        title = stringResource(R.string.tile_change_password),
        variant = if (isInternetReachable) TileVariant.NEUTRAL else TileVariant.DISABLED,
      ),
    )
  }

  tiles.add(SettingsTileData(stringResource(R.string.tile_print_settings)))

  if (isLoggedIn && isIdle) {
    tiles.add(
      SettingsTileData(
        title = stringResource(R.string.tile_logout),
        variant = if (isInternetReachable) TileVariant.NEUTRAL else TileVariant.DISABLED,
      ),
    )
  }

  return tiles
}
