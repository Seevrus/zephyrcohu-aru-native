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
import com.zephyr.boreal.ui.components.LoadingIndicator
import com.zephyr.boreal.ui.theme.BorealColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ErrandsScreen(
  modifier: Modifier = Modifier,
  onNavigateBack: () -> Unit = {},
  viewModel: ErrandsViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsState()

  LaunchedEffect(Unit) {
    viewModel.events.collect { event ->
      when (event) {
        ErrandsEvent.NavigateBack -> onNavigateBack()
      }
    }
  }

  ErrandsScreenContent(
    uiState = uiState,
    onTileClick = viewModel::onTileClick,
    onDismissAlert = viewModel::dismissAlert,
    modifier = modifier,
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ErrandsScreenContent(
  uiState: ErrandsUiState,
  onTileClick: (TileUiModel<ErrandTileId>) -> Unit,
  onDismissAlert: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Scaffold(
    modifier = modifier,
    topBar = {
      BorealTopAppBar(
        title = stringResource(R.string.screen_errands_title),
      )
    },
    containerColor = BorealColors.Background,
  ) { innerPadding ->
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
      ErrandsScreenList(
        uiState = uiState,
        onTileClick = onTileClick,
        modifier = Modifier.padding(innerPadding),
      )
    }

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
private fun ErrandsScreenList(
  uiState: ErrandsUiState,
  onTileClick: (TileUiModel<ErrandTileId>) -> Unit,
  modifier: Modifier = Modifier,
) {
  LazyColumn(
    contentPadding = PaddingValues(16.dp),
    modifier = modifier.fillMaxSize(),
  ) {
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
