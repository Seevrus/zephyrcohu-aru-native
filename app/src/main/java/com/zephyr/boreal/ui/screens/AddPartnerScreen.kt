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
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zephyr.boreal.R
import com.zephyr.boreal.ui.components.BorealAlert
import com.zephyr.boreal.ui.components.BorealButton
import com.zephyr.boreal.ui.components.BorealTextInput
import com.zephyr.boreal.ui.components.BorealTopAppBar
import com.zephyr.boreal.ui.components.ButtonVariant
import com.zephyr.boreal.ui.components.InfoCard
import com.zephyr.boreal.ui.theme.BorealColors

@Composable
fun AddPartnerScreen(
  viewModel: AddPartnerViewModel,
  onNavigateToSelectItems: () -> Unit,
  onNavigateBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  var showExitWarning by remember { mutableStateOf(false) }

  BackHandler(enabled = uiState.isModified) { showExitWarning = true }

  if (showExitWarning) {
    BorealAlert(
      title = stringResource(R.string.select_items_exit_warning_title),
      message = stringResource(R.string.add_partner_exit_warning_message),
      confirmButtonText = stringResource(R.string.dialog_yes),
      cancelButtonText = stringResource(R.string.dialog_cancel),
      onConfirmClick = {
        showExitWarning = false
        onNavigateBack()
      },
      onCancelClick = { showExitWarning = false },
      onDismissRequest = { showExitWarning = false },
    )
  }

  AddPartnerScreenContent(
    uiState = uiState,
    onFieldChanged = viewModel::onFieldChanged,
    onSubmit = { viewModel.onSubmit(onNavigateToSelectItems) },
    modifier = modifier,
  )
}

@Suppress("LongMethod")
@Composable
fun AddPartnerScreenContent(
  uiState: AddPartnerUiState,
  onFieldChanged: (AddPartnerField, String) -> Unit,
  onSubmit: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Scaffold(
    topBar = { BorealTopAppBar(title = stringResource(R.string.add_partner_title)) },
    containerColor = BorealColors.Background,
    modifier = modifier,
  ) { paddingValues ->
    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .padding(paddingValues)
          .padding(16.dp)
          .verticalScroll(rememberScrollState()),
    ) {
      if (uiState.isPreFilled) {
        InfoCard(message = stringResource(R.string.add_partner_info_prefilled))
      } else {
        InfoCard(message = stringResource(R.string.add_partner_info_manual))
      }

      Spacer(modifier = Modifier.height(16.dp))

      PartnerField(
        label = stringResource(R.string.add_partner_tax_number),
        value = uiState.taxNumber,
        error = uiState.taxNumberError,
        onValueChange = { onFieldChanged(AddPartnerField.TAX_NUMBER, it) },
      )

      Spacer(modifier = Modifier.height(16.dp))

      PartnerField(
        label = stringResource(R.string.add_partner_name),
        value = uiState.name,
        error = uiState.nameError,
        onValueChange = { onFieldChanged(AddPartnerField.NAME, it) },
      )

      SectionLabel(stringResource(R.string.add_partner_headquarters))

      AddressSectionFields(
        postalCode = uiState.centralPostalCode,
        postalCodeLabel = stringResource(R.string.add_partner_postal_code),
        city = uiState.centralCity,
        cityLabel = stringResource(R.string.add_partner_city),
        address = uiState.centralAddress,
        addressLabel = stringResource(R.string.add_partner_address),
        sectionError = uiState.centralAddressError,
        onPostalCodeChange = { onFieldChanged(AddPartnerField.CENTRAL_POSTAL_CODE, it) },
        onCityChange = { onFieldChanged(AddPartnerField.CENTRAL_CITY, it) },
        onAddressChange = { onFieldChanged(AddPartnerField.CENTRAL_ADDRESS, it) },
      )

      SectionLabel(stringResource(R.string.add_partner_delivery))

      PartnerField(
        label = stringResource(R.string.add_partner_delivery_name),
        value = uiState.deliveryName,
        error = uiState.deliveryNameError,
        onValueChange = { onFieldChanged(AddPartnerField.DELIVERY_NAME, it) },
      )

      Spacer(modifier = Modifier.height(16.dp))

      AddressSectionFields(
        postalCode = uiState.deliveryPostalCode,
        postalCodeLabel = stringResource(R.string.add_partner_delivery_postal_code),
        city = uiState.deliveryCity,
        cityLabel = stringResource(R.string.add_partner_delivery_city),
        address = uiState.deliveryAddress,
        addressLabel = stringResource(R.string.add_partner_delivery_address),
        sectionError = null,
        onPostalCodeChange = { onFieldChanged(AddPartnerField.DELIVERY_POSTAL_CODE, it) },
        onCityChange = { onFieldChanged(AddPartnerField.DELIVERY_CITY, it) },
        onAddressChange = { onFieldChanged(AddPartnerField.DELIVERY_ADDRESS, it) },
        postalCodeError = uiState.deliveryPostalCodeError,
        cityError = uiState.deliveryCityError,
        addressError = uiState.deliveryAddressError,
      )

      Spacer(modifier = Modifier.height(24.dp))

      Box(modifier = Modifier.fillMaxWidth()) {
        BorealButton(
          text = stringResource(R.string.add_partner_submit),
          variant = if (uiState.isSubmitting) ButtonVariant.DISABLED else ButtonVariant.NEUTRAL,
          onClick = onSubmit,
          modifier = Modifier.align(Alignment.Center),
        )
      }

      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}

@Composable
private fun SectionLabel(
  text: String,
  modifier: Modifier = Modifier,
) {
  Text(
    text = text,
    style = MaterialTheme.typography.titleLarge,
    color = BorealColors.White,
    textAlign = TextAlign.Center,
    modifier =
      modifier
        .fillMaxWidth()
        .padding(top = 20.dp, bottom = 8.dp),
  )
}

@Composable
private fun PartnerField(
  label: String,
  value: String,
  error: String?,
  onValueChange: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  BorealTextInput(
    label = label,
    value = value,
    onValueChange = onValueChange,
    isError = error != null,
    modifier = modifier,
  )
}

@Composable
private fun AddressSectionFields(
  postalCode: String,
  postalCodeLabel: String,
  city: String,
  cityLabel: String,
  address: String,
  addressLabel: String,
  sectionError: String?,
  onPostalCodeChange: (String) -> Unit,
  onCityChange: (String) -> Unit,
  onAddressChange: (String) -> Unit,
  modifier: Modifier = Modifier,
  postalCodeError: String? = null,
  cityError: String? = null,
  addressError: String? = null,
) {
  Column(modifier = modifier) {
    PartnerField(
      label = postalCodeLabel,
      value = postalCode,
      error = postalCodeError,
      onValueChange = onPostalCodeChange,
    )
    Spacer(modifier = Modifier.height(8.dp))
    PartnerField(
      label = cityLabel,
      value = city,
      error = cityError,
      onValueChange = onCityChange,
    )
    Spacer(modifier = Modifier.height(8.dp))
    PartnerField(
      label = addressLabel,
      value = address,
      error = sectionError ?: addressError,
      onValueChange = onAddressChange,
    )
  }
}
