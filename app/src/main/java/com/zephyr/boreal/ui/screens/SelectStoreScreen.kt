package com.zephyr.boreal.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zephyr.boreal.R
import com.zephyr.boreal.ui.components.BorealDropdown
import com.zephyr.boreal.ui.components.BorealTopAppBar
import com.zephyr.boreal.ui.components.DropdownItem
import com.zephyr.boreal.ui.components.ErrorCard
import com.zephyr.boreal.ui.components.LoadingIndicator
import com.zephyr.boreal.ui.theme.BorealColors

@Composable
fun SelectStoreScreen(
  viewModel: SelectStoreViewModel,
  onNavigateBack: () -> Unit,
  onSuccess: () -> Unit,
) {
  val uiState by viewModel.uiState.collectAsState()

  LaunchedEffect(uiState.isInternetReachable) {
    if (!uiState.isInternetReachable) {
      onNavigateBack()
    }
  }

  SelectStoreScreenContent(
    uiState = uiState,
    onStoreSelected = viewModel::onStoreSelected,
    onSubmit = { viewModel.submit(onSuccess) },
  )
}

@Composable
fun SelectStoreScreenContent(
  uiState: SelectStoreUiState,
  onStoreSelected: (Int) -> Unit,
  onSubmit: () -> Unit,
) {
  Scaffold(
    topBar = {
      BorealTopAppBar(
        title = stringResource(R.string.screen_select_store_title),
        actions = {
          if (uiState.canSubmit) {
            IconButton(onClick = onSubmit) {
              Icon(
                painter = painterResource(id = R.drawable.arrow_forward),
                contentDescription = "Tovább",
                tint = BorealColors.White,
                modifier = Modifier.size(32.dp),
              )
            }
          }
        },
      )
    },
    containerColor = BorealColors.Background,
  ) { paddingValues ->
    Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
      if (uiState.isLoading || uiState.isSubmitting) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          LoadingIndicator(
            message =
              stringResource(
                if (uiState.isSubmitting) {
                  R.string.select_store_locking_message
                } else {
                  R.string.select_store_loading_message
                },
              ),
          )
        }
      } else {
        SelectStoreForm(
          uiState = uiState,
          onStoreSelected = onStoreSelected,
        )
      }
    }
  }
}

@Composable
private fun SelectStoreForm(
  uiState: SelectStoreUiState,
  onStoreSelected: (Int) -> Unit,
) {
  Column(
    modifier =
      Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 24.dp, vertical = 16.dp),
  ) {
    if (uiState.errorMessage != null) {
      ErrorCard(message = uiState.errorMessage)
      Spacer(modifier = Modifier.height(16.dp))
    }

    BorealDropdown(
      label = stringResource(R.string.select_store_store_label),
      data =
        uiState.displayStores.map {
          DropdownItem(key = it.id.toString(), value = it.name)
        },
      selectedKey = uiState.selectedStoreId?.toString(),
      onSelect = { onStoreSelected(it.toInt()) },
      modifier = Modifier.fillMaxWidth(),
    )
  }
}
