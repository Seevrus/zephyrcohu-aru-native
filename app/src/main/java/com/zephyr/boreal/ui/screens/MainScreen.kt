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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zephyr.boreal.R
import com.zephyr.boreal.ui.components.BorealTile
import com.zephyr.boreal.ui.components.BorealTopAppBar
import com.zephyr.boreal.ui.components.InfoCard
import com.zephyr.boreal.ui.components.LoadingIndicator
import com.zephyr.boreal.ui.components.SettingsButton
import com.zephyr.boreal.ui.components.TileVariant
import com.zephyr.boreal.ui.theme.BorealColors

@Composable
fun getTiles(
  numberOfReceipts: Int,
  isLoggedIn: Boolean,
): List<TileData> {
  val defaultVariant = if (isLoggedIn) TileVariant.NEUTRAL else TileVariant.DISABLED
  return listOf(
    TileData(
      stringResource(R.string.tile_loading),
      if (isLoggedIn) TileVariant.OK else TileVariant.DISABLED,
      ImageVector.vectorResource(R.drawable.truck_solid_full),
    ),
    TileData(
      stringResource(R.string.tile_unloading),
      if (isLoggedIn) TileVariant.WARNING else TileVariant.DISABLED,
      ImageVector.vectorResource(R.drawable.cart_arrow_down_solid_full),
    ),
    TileData(
      stringResource(R.string.tile_rounds),
      defaultVariant,
      ImageVector.vectorResource(R.drawable.rectangle_list_solid_full),
    ),
    TileData(
      stringResource(R.string.tile_receipts, numberOfReceipts),
      TileVariant.DISABLED,
      ImageVector.vectorResource(R.drawable.receipt_solid_full),
    ),
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
  modifier: Modifier = Modifier,
  onNavigateToAppLocked: () -> Unit = {},
  onNavigateToSettings: () -> Unit = {},
  onNavigateToLogin: () -> Unit = {},
  viewModel: MainViewModel = hiltViewModel(),
) {
  val appState by viewModel.appState.collectAsState()

  val isReady = appState is AppStartState.Ready
  val readyState = appState as? AppStartState.Ready
  val isLoggedIn = readyState?.isLoggedIn ?: false
  val canUseApp = readyState?.canUseApp
  val isInternetReachable = readyState?.isInternetReachable ?: true

  MainScreenContent(
    isReady = isReady,
    isLoggedIn = isLoggedIn,
    canUseApp = canUseApp,
    isInternetReachable = isInternetReachable,
    onNavigateToAppLocked = onNavigateToAppLocked,
    onNavigateToSettings = onNavigateToSettings,
    onNavigateToLogin = onNavigateToLogin,
    modifier = modifier,
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenContent(
  isReady: Boolean,
  isLoggedIn: Boolean,
  canUseApp: Boolean?,
  isInternetReachable: Boolean,
  onNavigateToAppLocked: () -> Unit,
  onNavigateToSettings: () -> Unit,
  onNavigateToLogin: () -> Unit,
  modifier: Modifier = Modifier,
) {
  LaunchedEffect(isLoggedIn, canUseApp) {
    if (isLoggedIn && canUseApp == false) {
      onNavigateToAppLocked()
    }
  }

  val tiles = getTiles(numberOfReceipts = 0, isLoggedIn = isLoggedIn)

  Scaffold(
    modifier = modifier,
    topBar = {
      BorealTopAppBar(
        title = stringResource(R.string.screen_main_title),
        actions = {
          SettingsButton(onClick = onNavigateToSettings)
        },
      )
    },
    containerColor = BorealColors.Background,
  ) { innerPadding ->
    if (!isReady) {
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
      MainScreenList(
        isLoggedIn = isLoggedIn,
        isInternetReachable = isInternetReachable,
        tiles = tiles,
        onNavigateToLogin = onNavigateToLogin,
        modifier = Modifier.padding(innerPadding),
      )
    }
  }
}

@Composable
private fun MainScreenList(
  isLoggedIn: Boolean,
  isInternetReachable: Boolean,
  tiles: List<TileData>,
  onNavigateToLogin: () -> Unit,
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

    if (!isLoggedIn && isInternetReachable) {
      item {
        InfoCard(
          message = stringResource(R.string.main_login_prompt),
          modifier = Modifier.padding(bottom = 16.dp),
          onClick = onNavigateToLogin,
        )
      }
    }

    items(tiles) { tile ->
      BorealTile(
        title = tile.title,
        variant = tile.variant,
        icon = tile.icon,
        modifier = Modifier.padding(bottom = 16.dp),
      )
    }
  }
}
