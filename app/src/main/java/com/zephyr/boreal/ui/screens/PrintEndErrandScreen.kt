package com.zephyr.boreal.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zephyr.boreal.R
import com.zephyr.boreal.ui.components.BorealAlert
import com.zephyr.boreal.ui.components.BorealButton
import com.zephyr.boreal.ui.components.BorealTopAppBar
import com.zephyr.boreal.ui.components.ButtonVariant
import com.zephyr.boreal.ui.components.ErrorCard
import com.zephyr.boreal.ui.components.LoadingIndicator
import com.zephyr.boreal.ui.components.PrinterConnector
import com.zephyr.boreal.ui.components.SuccessCard
import com.zephyr.boreal.ui.theme.BorealColors

@Composable
fun PrintEndErrandScreen(
  viewModel: PrintEndErrandViewModel,
  onNavigateHome: () -> Unit,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  BackHandler(enabled = true) {
    viewModel.onBackPress(onNavigateHome)
  }

  PrintEndErrandScreenContent(
    uiState = uiState,
    onPrintSuccess = viewModel::onPrintSuccess,
    onReturnClick = { viewModel.onReturnClick(onNavigateHome) },
    onConfirmExitClick = { viewModel.confirmExit(onNavigateHome) },
    onDismissExitConfirm = viewModel::dismissExitConfirm,
  )
}

@Composable
internal fun PrintEndErrandScreenContent(
  uiState: PrintEndErrandUiState,
  onPrintSuccess: () -> Unit,
  onReturnClick: () -> Unit,
  onConfirmExitClick: () -> Unit,
  onDismissExitConfirm: () -> Unit,
) {
  Scaffold(
    topBar = {
      BorealTopAppBar(title = stringResource(R.string.screen_print_end_errand_title))
    },
    containerColor = BorealColors.Background,
  ) { paddingValues ->
    if (uiState.isLoading) {
      Box(
        modifier = Modifier.fillMaxSize().padding(paddingValues),
        contentAlignment = Alignment.Center,
      ) {
        LoadingIndicator()
      }
      return@Scaffold
    }

    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .padding(paddingValues)
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 24.dp, vertical = 20.dp),
    ) {
      SuccessCard(message = stringResource(R.string.print_end_errand_success_header))

      if (!uiState.isPrintEnabled) {
        Spacer(modifier = Modifier.height(16.dp))
        ErrorCard(message = stringResource(R.string.print_end_errand_missing_data))
      }

      PrintStatusSection(uiState = uiState, onPrintSuccess = onPrintSuccess)

      Spacer(modifier = Modifier.height(16.dp))
      Text(text = stringResource(R.string.print_end_errand_no_more_printing))

      Spacer(modifier = Modifier.height(30.dp))

      ReturnButtonSection(uiState = uiState, onReturnClick = onReturnClick)
    }

    if (uiState.showExitConfirm) {
      ExitConfirmAlert(onConfirmExitClick = onConfirmExitClick, onDismissExitConfirm = onDismissExitConfirm)
    }
  }
}

@Composable
private fun PrintStatusSection(
  uiState: PrintEndErrandUiState,
  onPrintSuccess: () -> Unit,
) {
  if (uiState.textToPrint == null) {
    return
  }

  Spacer(modifier = Modifier.height(16.dp))
  Text(
    text =
      stringResource(
        if (uiState.canLeaveWithoutPrint) {
          R.string.print_end_errand_no_print_needed
        } else {
          R.string.print_end_errand_print_intro
        },
      ),
  )

  if (!uiState.canLeaveWithoutPrint) {
    Spacer(modifier = Modifier.height(8.dp))
    PrinterConnector(
      textToPrint = uiState.textToPrint,
      suggestedFileName = uiState.suggestedFileName,
      onPrintSuccess = onPrintSuccess,
    )
  }
}

@Composable
private fun ReturnButtonSection(
  uiState: PrintEndErrandUiState,
  onReturnClick: () -> Unit,
) {
  Box(
    modifier =
      Modifier
        .fillMaxWidth()
        .padding(horizontal = 30.dp),
  ) {
    BorealButton(
      text = stringResource(R.string.print_end_errand_return_button),
      variant = if (uiState.canLeaveWithoutPrint || uiState.isPrinted) ButtonVariant.OK else ButtonVariant.DISABLED,
      onClick = onReturnClick,
      modifier = Modifier.align(Alignment.Center),
    )
  }
}

@Composable
private fun ExitConfirmAlert(
  onConfirmExitClick: () -> Unit,
  onDismissExitConfirm: () -> Unit,
) {
  BorealAlert(
    title = stringResource(R.string.print_end_errand_exit_confirm_title),
    message = stringResource(R.string.print_end_errand_exit_confirm_message),
    confirmButtonText = stringResource(R.string.dialog_yes),
    confirmButtonVariant = ButtonVariant.WARNING,
    cancelButtonText = stringResource(R.string.dialog_cancel),
    onConfirmClick = onConfirmExitClick,
    onCancelClick = onDismissExitConfirm,
    onDismissRequest = onDismissExitConfirm,
  )
}
