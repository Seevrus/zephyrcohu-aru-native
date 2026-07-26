package com.zephyr.boreal.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zephyr.boreal.R
import com.zephyr.boreal.ui.theme.BorealFontSizes

@Composable
fun PrinterConnector(
  textToPrint: String,
  suggestedFileName: String,
  onPrintSuccess: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: PrinterConnectorViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  DisposableEffect(Unit) {
    onDispose { viewModel.disconnect() }
  }

  val saveFileLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
      viewModel.onSaveFileResult(uri)
    }

  LaunchedEffect(uiState.saveFileRequest) {
    uiState.saveFileRequest?.let { request ->
      saveFileLauncher.launch(request.suggestedFileName)
    }
  }

  var lastHandledPrintSuccessToken by remember { mutableIntStateOf(uiState.printSuccessToken) }
  LaunchedEffect(uiState.printSuccessToken) {
    if (uiState.printSuccessToken != lastHandledPrintSuccessToken) {
      lastHandledPrintSuccessToken = uiState.printSuccessToken
      onPrintSuccess()
    }
  }

  PrinterConnectorContent(
    uiState = uiState,
    onConnectClick = viewModel::connect,
    onPrintClick = { viewModel.print(textToPrint, suggestedFileName) },
    modifier = modifier,
  )
}

@Composable
internal fun PrinterConnectorContent(
  uiState: PrinterConnectorUiState,
  onConnectClick: () -> Unit,
  onPrintClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier) {
    Text(
      text =
        stringResource(
          if (uiState.isBluetoothEnabled) {
            R.string.printer_connector_bluetooth_on
          } else {
            R.string.printer_connector_bluetooth_off
          },
        ),
      fontSize = BorealFontSizes.Subtitle,
      textAlign = TextAlign.Center,
      modifier =
        Modifier
          .fillMaxWidth()
          .padding(vertical = 16.dp),
    )

    if (uiState.error != null) {
      Spacer(modifier = Modifier.height(8.dp))
      ErrorCard(message = stringResource(R.string.printer_connector_error, errorMessage(uiState.error)))
    }

    if (uiState.isBluetoothEnabled && uiState.selectedPrinterAddress != null) {
      Spacer(modifier = Modifier.height(8.dp))
      Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        when (uiState.phase) {
          PrinterConnectorPhase.IDLE ->
            BorealButton(
              text = stringResource(R.string.printer_connector_connect),
              variant = ButtonVariant.NEUTRAL,
              onClick = onConnectClick,
            )
          PrinterConnectorPhase.CONNECTING ->
            BorealButton(
              text = stringResource(R.string.printer_connector_connecting),
              variant = ButtonVariant.DISABLED,
              onClick = {},
            )
          PrinterConnectorPhase.CONNECTED ->
            BorealButton(
              text = stringResource(R.string.printer_connector_print),
              variant = ButtonVariant.NEUTRAL,
              onClick = onPrintClick,
            )
          PrinterConnectorPhase.PRINTING ->
            BorealButton(
              text = stringResource(R.string.printer_connector_printing),
              variant = ButtonVariant.DISABLED,
              onClick = {},
            )
        }
      }
    }
  }
}

@Composable
private fun errorMessage(error: PrinterConnectorError): String =
  when (error) {
    is PrinterConnectorError.ProbablyOff -> stringResource(R.string.printer_connector_probably_off)
    is PrinterConnectorError.Other ->
      error.message.ifBlank { stringResource(R.string.printer_connector_generic_connect_error) }
  }
