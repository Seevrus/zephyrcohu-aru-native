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
import com.zephyr.boreal.ui.components.BorealAlert
import com.zephyr.boreal.ui.components.BorealTile
import com.zephyr.boreal.ui.components.BorealTopAppBar
import com.zephyr.boreal.ui.components.InfoCard
import com.zephyr.boreal.ui.components.LoadingIndicator
import com.zephyr.boreal.ui.components.RoundInfo
import com.zephyr.boreal.ui.components.SettingsButton
import com.zephyr.boreal.ui.theme.BorealColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
  modifier: Modifier = Modifier,
  onNavigateToAppLocked: () -> Unit = {},
  onNavigateToSettings: () -> Unit = {},
  onNavigateToLogin: () -> Unit = {},
  onNavigateToChangePassword: () -> Unit = {},
  onNavigateToErrands: () -> Unit = {},
  onNavigateToSelectPartner: () -> Unit = {},
  viewModel: MainViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsState()

  LaunchedEffect(Unit) {
    viewModel.events.collect { event ->
      when (event) {
        MainScreenEvent.NavigateToErrands -> onNavigateToErrands()
        MainScreenEvent.NavigateToSelectPartner -> onNavigateToSelectPartner()
      }
    }
  }

  MainScreenContent(
    uiState = uiState,
    onNavigateToAppLocked = onNavigateToAppLocked,
    onNavigateToSettings = onNavigateToSettings,
    onNavigateToLogin = onNavigateToLogin,
    onNavigateToChangePassword = onNavigateToChangePassword,
    onTileClick = viewModel::onTileClick,
    onDismissAlert = viewModel::dismissAlert,
    modifier = modifier,
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenContent(
  uiState: MainScreenUiState,
  onNavigateToAppLocked: () -> Unit,
  onNavigateToSettings: () -> Unit,
  onNavigateToLogin: () -> Unit,
  onNavigateToChangePassword: () -> Unit,
  onTileClick: (TileUiModel<MainTileId>) -> Unit,
  onDismissAlert: () -> Unit,
  modifier: Modifier = Modifier,
) {
  LaunchedEffect(uiState.isLoggedIn, uiState.canUseApp) {
    if (uiState.isLoggedIn && uiState.canUseApp == false) {
      onNavigateToAppLocked()
    }
  }

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
    MainScreenBody(
      uiState = uiState,
      onNavigateToLogin = onNavigateToLogin,
      onNavigateToChangePassword = onNavigateToChangePassword,
      onTileClick = onTileClick,
      innerPadding = innerPadding,
    )

    uiState.alertState?.let { alert ->
      BorealAlert(
        title = stringResource(alert.titleResId),
        message = alert.messageResId?.let { stringResource(it) },
        confirmButtonText = alert.confirmTextResId?.let { stringResource(it) },
        cancelButtonText = alert.cancelButtonTextResId?.let { stringResource(it) },
        onConfirmClick = alert.onConfirm,
        onCancelClick = alert.onCancel,
        onDismissRequest = onDismissAlert,
      )
    }
  }
}

@Composable
private fun MainScreenBody(
  uiState: MainScreenUiState,
  onNavigateToLogin: () -> Unit,
  onNavigateToChangePassword: () -> Unit,
  onTileClick: (TileUiModel<MainTileId>) -> Unit,
  innerPadding: PaddingValues,
) {
  if (!uiState.isReady) {
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
    androidx.compose.foundation.layout.Column(
      modifier = Modifier.padding(innerPadding).fillMaxSize(),
    ) {
      MainScreenList(
        uiState = uiState,
        onNavigateToLogin = onNavigateToLogin,
        onNavigateToChangePassword = onNavigateToChangePassword,
        onTileClick = onTileClick,
        modifier = Modifier.weight(1f),
      )

      uiState.roundInfo?.let { roundInfo ->
        RoundInfo(
          roundInfo = roundInfo,
          modifier = Modifier.padding(bottom = 16.dp),
        )
      }
    }
  }
}

@Composable
private fun MainScreenList(
  uiState: MainScreenUiState,
  onNavigateToLogin: () -> Unit,
  onNavigateToChangePassword: () -> Unit,
  onTileClick: (TileUiModel<MainTileId>) -> Unit,
  modifier: Modifier = Modifier,
) {
  LazyColumn(
    contentPadding = PaddingValues(16.dp),
    modifier = modifier.fillMaxSize(),
  ) {
    if (!uiState.isInternetReachable) {
      item {
        InfoCard(
          message = stringResource(R.string.offline_warning),
          modifier = Modifier.padding(bottom = 16.dp),
        )
      }
    }

    if (!uiState.isLoggedIn && uiState.isInternetReachable) {
      item {
        InfoCard(
          message = stringResource(R.string.main_login_prompt),
          modifier = Modifier.padding(bottom = 16.dp),
          onClick = onNavigateToLogin,
        )
      }
    }

    if (uiState.isLoggedIn && uiState.isPasswordExpired && uiState.isInternetReachable) {
      item {
        InfoCard(
          message = stringResource(R.string.change_password_expired_warning),
          modifier = Modifier.padding(bottom = 16.dp),
          onClick = onNavigateToChangePassword,
        )
      }
    }

    items(uiState.tiles, key = { it.id }) { tile ->
      val title =
        tile.titleArg?.let {
          stringResource(tile.titleResId, it)
        } ?: stringResource(tile.titleResId)

      BorealTile(
        title = title,
        variant = tile.variant,
        icon = ImageVector.vectorResource(tile.iconResId),
        modifier = Modifier.padding(bottom = 16.dp),
        onClick = { onTileClick(tile) },
      )
    }
  }
}
