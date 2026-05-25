package com.zephyr.boreal.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zephyr.boreal.R
import com.zephyr.boreal.ui.components.BorealButton
import com.zephyr.boreal.ui.components.BorealDropdown
import com.zephyr.boreal.ui.components.BorealTopAppBar
import com.zephyr.boreal.ui.components.ButtonVariant
import com.zephyr.boreal.ui.components.DropdownItem
import com.zephyr.boreal.ui.components.ErrorCard
import com.zephyr.boreal.ui.components.LoadingIndicator
import com.zephyr.boreal.ui.theme.BorealColors
import com.zephyr.boreal.ui.theme.BorealFontSizes
import com.zephyr.boreal.ui.theme.NunitoSansFamily
import java.util.Calendar

@Composable
fun StartErrandScreen(
  viewModel: StartErrandViewModel,
  onSuccess: () -> Unit,
) {
  val uiState by viewModel.uiState.collectAsState()
  val context = LocalContext.current

  val datePickerDialog =
    rememberDatePickerDialog(
      context = context,
      onDateSelected = viewModel::onDateSelected,
    )

  Scaffold(
    topBar = {
      BorealTopAppBar(
        title = stringResource(R.string.screen_start_errand_title),
      )
    },
    containerColor = BorealColors.Background,
  ) { paddingValues ->
    StartErrandContent(
      uiState = uiState,
      onStoreSelected = viewModel::onStoreSelected,
      onPartnerListSelected = viewModel::onPartnerListSelected,
      onShowDatePicker = { datePickerDialog.show() },
      onConfirm = { viewModel.startRound(onSuccess) },
      modifier = Modifier.padding(paddingValues),
    )
  }
}

@Composable
private fun rememberDatePickerDialog(
  context: android.content.Context,
  onDateSelected: (java.util.Date) -> Unit,
): DatePickerDialog {
  val oneSecondInMillis = 1000L
  return DatePickerDialog(
    context,
    { _, year, month, dayOfMonth ->
      val calendar = Calendar.getInstance()
      calendar.set(year, month, dayOfMonth)
      onDateSelected(calendar.time)
    },
    Calendar.getInstance().get(Calendar.YEAR),
    Calendar.getInstance().get(Calendar.MONTH),
    Calendar.getInstance().get(Calendar.DAY_OF_MONTH),
  ).apply {
    datePicker.minDate = System.currentTimeMillis() - oneSecondInMillis
  }
}

@Composable
private fun StartErrandContent(
  uiState: StartErrandUiState,
  onStoreSelected: (Int) -> Unit,
  onPartnerListSelected: (Int) -> Unit,
  onShowDatePicker: () -> Unit,
  onConfirm: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier.fillMaxSize(),
  ) {
    if (uiState.isLoading || uiState.isStartingRound) {
      StartErrandLoading(uiState.isStartingRound)
    } else {
      StartErrandForm(
        uiState = uiState,
        onStoreSelected = onStoreSelected,
        onPartnerListSelected = onPartnerListSelected,
        onShowDatePicker = onShowDatePicker,
        onConfirm = onConfirm,
      )
    }
  }
}

@Composable
private fun StartErrandLoading(isStartingRound: Boolean) {
  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    LoadingIndicator(
      message =
        stringResource(
          if (isStartingRound) {
            R.string.start_errand_starting_message
          } else {
            R.string.start_errand_loading_message
          },
        ),
    )
  }
}

@Composable
private fun StartErrandForm(
  uiState: StartErrandUiState,
  onStoreSelected: (Int) -> Unit,
  onPartnerListSelected: (Int) -> Unit,
  onShowDatePicker: () -> Unit,
  onConfirm: () -> Unit,
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
    }

    Spacer(modifier = Modifier.height(16.dp))

    BorealDropdown(
      label = stringResource(R.string.start_errand_store_label),
      data =
        uiState.displayStores.map {
          DropdownItem(key = it.id.toString(), value = it.name)
        },
      selectedKey = uiState.selectedStoreId?.toString(),
      onSelect = { onStoreSelected(it.toInt()) },
      modifier = Modifier.fillMaxWidth(),
    )

    Spacer(modifier = Modifier.height(24.dp))

    BorealDropdown(
      label = stringResource(R.string.start_errand_partner_list_label),
      data =
        uiState.displayPartnerLists.map {
          DropdownItem(key = it.id.toString(), value = it.name)
        },
      selectedKey = uiState.selectedPartnerListId?.toString(),
      onSelect = { onPartnerListSelected(it.toInt()) },
      modifier = Modifier.fillMaxWidth(),
    )

    Spacer(modifier = Modifier.height(24.dp))

    StartErrandDatePicker(
      label = stringResource(R.string.start_errand_date_label),
      date = uiState.formattedDate,
      onClick = onShowDatePicker,
    )

    Spacer(modifier = Modifier.height(48.dp))

    Box(modifier = Modifier.fillMaxWidth()) {
      BorealButton(
        text = stringResource(R.string.start_errand_button_label),
        variant = if (uiState.isConfirmEnabled) ButtonVariant.OK else ButtonVariant.DISABLED,
        onClick = onConfirm,
        modifier = Modifier.align(Alignment.Center),
      )
    }
  }
}

@Composable
private fun StartErrandDatePicker(
  label: String,
  date: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier.fillMaxWidth()) {
    Text(
      text = label,
      color = BorealColors.White,
      fontFamily = NunitoSansFamily,
      fontWeight = FontWeight.Bold,
      fontSize = BorealFontSizes.Input,
      modifier = Modifier.padding(bottom = 8.dp),
    )

    Box(
      modifier =
        Modifier
          .fillMaxWidth()
          .height(56.dp)
          .clip(RoundedCornerShape(4.dp))
          .background(BorealColors.Input)
          .clickable { onClick() }
          .padding(horizontal = 16.dp),
      contentAlignment = Alignment.CenterStart,
    ) {
      Text(
        text = date,
        color = BorealColors.White,
        fontFamily = NunitoSansFamily,
        fontSize = BorealFontSizes.Input,
      )
    }
  }
}
