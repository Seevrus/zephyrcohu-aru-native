package com.zephyr.boreal.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zephyr.boreal.R
import com.zephyr.boreal.ui.components.BorealTopAppBar
import com.zephyr.boreal.ui.theme.BorealColors

@Composable
fun AddPartnerScreen(
  viewModel: AddPartnerViewModel,
  onNavigateToSelectItems: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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
      PartnerTextField(
        value = uiState.taxNumber,
        label = stringResource(R.string.add_partner_tax_number),
        error = uiState.taxNumberError,
        onValueChange = { onFieldChanged(AddPartnerField.TAX_NUMBER, it) },
      )

      Spacer(modifier = Modifier.height(8.dp))

      PartnerTextField(
        value = uiState.name,
        label = stringResource(R.string.add_partner_name),
        error = uiState.nameError,
        onValueChange = { onFieldChanged(AddPartnerField.NAME, it) },
      )

      Spacer(modifier = Modifier.height(16.dp))

      SectionLabel(stringResource(R.string.add_partner_headquarters))
      AddressSectionFields(
        postalCode = uiState.centralPostalCode,
        city = uiState.centralCity,
        address = uiState.centralAddress,
        sectionError = uiState.centralAddressError,
        onPostalCodeChange = { onFieldChanged(AddPartnerField.CENTRAL_POSTAL_CODE, it) },
        onCityChange = { onFieldChanged(AddPartnerField.CENTRAL_CITY, it) },
        onAddressChange = { onFieldChanged(AddPartnerField.CENTRAL_ADDRESS, it) },
      )

      Spacer(modifier = Modifier.height(16.dp))

      SectionLabel(stringResource(R.string.add_partner_delivery))
      AddressSectionFields(
        postalCode = uiState.deliveryPostalCode,
        city = uiState.deliveryCity,
        address = uiState.deliveryAddress,
        sectionError =
          uiState.deliveryPostalCodeError
            ?: uiState.deliveryCityError
            ?: uiState.deliveryAddressError,
        onPostalCodeChange = { onFieldChanged(AddPartnerField.DELIVERY_POSTAL_CODE, it) },
        onCityChange = { onFieldChanged(AddPartnerField.DELIVERY_CITY, it) },
        onAddressChange = { onFieldChanged(AddPartnerField.DELIVERY_ADDRESS, it) },
      )

      Spacer(modifier = Modifier.height(24.dp))

      Button(
        onClick = onSubmit,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = BorealColors.Ok),
        enabled = !uiState.isSubmitting,
      ) {
        Text(stringResource(R.string.add_partner_submit), color = BorealColors.White)
      }
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
    style = MaterialTheme.typography.titleMedium,
    color = BorealColors.White,
    modifier = modifier,
  )
  Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun PartnerTextField(
  value: String,
  label: String,
  error: String?,
  onValueChange: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  OutlinedTextField(
    value = value,
    onValueChange = onValueChange,
    label = { Text(label) },
    isError = error != null,
    supportingText = error?.let { { Text(it, color = BorealColors.Error) } },
    singleLine = true,
    modifier = modifier.fillMaxWidth(),
  )
}

@Composable
private fun AddressSectionFields(
  postalCode: String,
  city: String,
  address: String,
  sectionError: String?,
  onPostalCodeChange: (String) -> Unit,
  onCityChange: (String) -> Unit,
  onAddressChange: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier) {
    PartnerTextField(
      value = postalCode,
      label = stringResource(R.string.add_partner_postal_code),
      error = null,
      onValueChange = onPostalCodeChange,
    )
    Spacer(modifier = Modifier.height(4.dp))
    PartnerTextField(
      value = city,
      label = stringResource(R.string.add_partner_city),
      error = null,
      onValueChange = onCityChange,
    )
    Spacer(modifier = Modifier.height(4.dp))
    PartnerTextField(
      value = address,
      label = stringResource(R.string.add_partner_address),
      error = sectionError,
      onValueChange = onAddressChange,
    )
  }
}
