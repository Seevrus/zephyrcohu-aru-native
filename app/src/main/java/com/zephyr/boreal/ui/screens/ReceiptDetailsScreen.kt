package com.zephyr.boreal.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zephyr.boreal.R
import com.zephyr.boreal.domain.model.Receipt
import com.zephyr.boreal.ui.components.BorealAlert
import com.zephyr.boreal.ui.components.BorealButton
import com.zephyr.boreal.ui.components.BorealTopAppBar
import com.zephyr.boreal.ui.components.ButtonVariant
import com.zephyr.boreal.ui.components.ErrorCard
import com.zephyr.boreal.ui.components.InfoCard
import com.zephyr.boreal.ui.components.LoadingIndicator
import com.zephyr.boreal.ui.components.PrintSection
import com.zephyr.boreal.ui.components.SuccessCard
import com.zephyr.boreal.ui.theme.BorealColors
import com.zephyr.boreal.ui.theme.BorealFontSizes
import com.zephyr.boreal.ui.theme.NunitoSansFamily

@Composable
fun ReceiptDetailsScreen(viewModel: ReceiptDetailsViewModel) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  ReceiptDetailsScreenContent(
    uiState = uiState,
    onCancelRequested = viewModel::requestCancel,
  )
}

@Composable
internal fun ReceiptDetailsScreenContent(
  uiState: ReceiptDetailsUiState,
  onCancelRequested: () -> Unit,
) {
  var isConfirmDialogVisible by remember { mutableStateOf(false) }
  val receipt = uiState.receipt

  Scaffold(
    topBar = {
      BorealTopAppBar(title = receipt?.let { "${it.serialNumber}/${it.yearCode}" } ?: "")
    },
    containerColor = BorealColors.Background,
  ) { paddingValues ->
    if (uiState.isLoading || receipt == null) {
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
      ReceiptDetailsStatusSection(uiState = uiState, receipt = receipt)

      if (uiState.isCancelInProgress) {
        LoadingIndicator()
      } else {
        PrintSection(isCancel = receipt.isCanceled, receiptSerialNumber = receipt.serialNumber)
      }

      if (uiState.canCancel) {
        ReceiptDetailsCancelSection(onCancelButtonClick = { isConfirmDialogVisible = true })
      }
    }
  }

  if (isConfirmDialogVisible) {
    ReceiptDetailsCancelConfirmDialog(
      onConfirm = {
        isConfirmDialogVisible = false
        onCancelRequested()
      },
      onDismiss = { isConfirmDialogVisible = false },
    )
  }
}

@Composable
private fun ReceiptDetailsStatusSection(
  uiState: ReceiptDetailsUiState,
  receipt: Receipt,
) {
  Text(
    text =
      if (receipt.isCanceled) {
        stringResource(R.string.receipt_details_subtitle_canceled)
      } else {
        stringResource(R.string.receipt_details_subtitle_normal)
      },
    fontFamily = NunitoSansFamily,
    fontWeight = FontWeight.Bold,
    fontSize = BorealFontSizes.Subtitle,
    color = BorealColors.White,
    textAlign = TextAlign.Center,
    modifier = Modifier.fillMaxWidth(),
  )

  if (receipt.isCanceled) {
    if (uiState.hasCanceledNow) {
      SuccessCard(message = stringResource(R.string.receipt_details_canceled_now))
    } else {
      InfoCard(message = stringResource(R.string.receipt_details_canceled_previously))
    }
  }

  if (!uiState.isInternetReachable) {
    InfoCard(message = stringResource(R.string.receipt_details_offline_notice))
  }

  if (uiState.errorMessage != null) {
    ErrorCard(message = uiState.errorMessage)
  }
}

@Composable
private fun ReceiptDetailsCancelSection(onCancelButtonClick: () -> Unit) {
  Text(
    text = stringResource(R.string.receipt_details_cancel_body),
    color = BorealColors.White,
    modifier = Modifier.padding(top = 24.dp, bottom = 16.dp),
  )
  Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
    BorealButton(
      text = stringResource(R.string.receipt_details_cancel_button),
      variant = ButtonVariant.WARNING,
      onClick = onCancelButtonClick,
    )
  }
}

@Composable
private fun ReceiptDetailsCancelConfirmDialog(
  onConfirm: () -> Unit,
  onDismiss: () -> Unit,
) {
  BorealAlert(
    title = stringResource(R.string.receipt_details_cancel_confirm_title),
    message = stringResource(R.string.receipt_details_cancel_confirm_message),
    confirmButtonText = stringResource(R.string.receipt_details_cancel_confirm_confirm),
    confirmButtonVariant = ButtonVariant.WARNING,
    cancelButtonText = stringResource(R.string.receipt_details_cancel_confirm_dismiss),
    onConfirmClick = onConfirm,
    onCancelClick = onDismiss,
    onDismissRequest = onDismiss,
  )
}
