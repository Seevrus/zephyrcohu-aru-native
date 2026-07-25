package com.zephyr.boreal.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zephyr.boreal.R
import com.zephyr.boreal.ui.screens.PrintSectionUiState
import com.zephyr.boreal.ui.screens.PrintSectionViewModel
import com.zephyr.boreal.ui.theme.BorealColors

@Composable
fun PrintSection(
  isCancel: Boolean,
  receiptSerialNumber: Int,
  modifier: Modifier = Modifier,
  viewModel: PrintSectionViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  LaunchedEffect(isCancel, receiptSerialNumber) {
    viewModel.initialize(isCancel, receiptSerialNumber)
  }

  PrintSectionContent(
    uiState = uiState,
    onPrintSuccess = viewModel::onPrintSuccess,
    modifier = modifier,
  )
}

@Composable
internal fun PrintSectionContent(
  uiState: PrintSectionUiState,
  onPrintSuccess: () -> Unit,
  modifier: Modifier = Modifier,
  printerConnector: @Composable (String, String, () -> Unit) -> Unit = { text, fileName, onSuccess ->
    PrinterConnector(textToPrint = text, suggestedFileName = fileName, onPrintSuccess = onSuccess)
  },
) {
  if (uiState.isLoading) {
    LoadingIndicator(modifier = modifier)
    return
  }

  val formatLabel = stringResource(R.string.print_section_format_label)

  Column(modifier = modifier) {
    Text(
      text =
        buildAnnotatedString {
          append("$formatLabel ")
          withStyle(SpanStyle(color = BorealColors.Ok, fontWeight = FontWeight.Bold)) {
            append(uiState.receiptFormat)
          }
          append(".")
        },
    )

    if (uiState.receiptToPrint != null) {
      Spacer(modifier = Modifier.height(8.dp))
      Text(text = stringResource(R.string.print_section_description, uiState.copyCount))
      Spacer(modifier = Modifier.height(8.dp))
      printerConnector(uiState.receiptToPrint, uiState.suggestedFileName, onPrintSuccess)
    } else {
      Spacer(modifier = Modifier.height(8.dp))
      Text(text = stringResource(R.string.print_section_missing_data))
    }
  }
}
