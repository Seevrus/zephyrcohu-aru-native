package com.zephyr.boreal.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zephyr.boreal.R
import com.zephyr.boreal.domain.model.Discount
import com.zephyr.boreal.ui.components.BorealButton
import com.zephyr.boreal.ui.components.BorealTextInput
import com.zephyr.boreal.ui.components.BorealTopAppBar
import com.zephyr.boreal.ui.components.ButtonVariant
import com.zephyr.boreal.ui.components.ErrorCard
import com.zephyr.boreal.ui.components.LoadingIndicator
import com.zephyr.boreal.ui.theme.BorealColors
import com.zephyr.boreal.ui.theme.BorealFontSizes
import com.zephyr.boreal.ui.theme.NunitoSansFamily

@Composable
fun DiscountsScreen(
  viewModel: DiscountsViewModel,
  onNavigateBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  DiscountsScreenContent(
    uiState = uiState,
    onAbsoluteQuantityChanged = viewModel::onAbsoluteQuantityChanged,
    onPercentageQuantityChanged = viewModel::onPercentageQuantityChanged,
    onFreeFormQuantityChanged = viewModel::onFreeFormQuantityChanged,
    onFreeFormPriceChanged = viewModel::onFreeFormPriceChanged,
    onApply = { viewModel.applyDiscounts(onNavigateBack) },
    modifier = modifier,
  )
}

@Composable
internal fun DiscountsScreenContent(
  uiState: DiscountsUiState,
  onAbsoluteQuantityChanged: (String) -> Unit,
  onPercentageQuantityChanged: (String) -> Unit,
  onFreeFormQuantityChanged: (String) -> Unit,
  onFreeFormPriceChanged: (String) -> Unit,
  onApply: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Scaffold(
    topBar = { BorealTopAppBar(title = stringResource(R.string.discounts_title)) },
    containerColor = BorealColors.Background,
    modifier = modifier,
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
          .padding(16.dp)
          .verticalScroll(rememberScrollState()),
    ) {
      DiscountsFormHeader(uiState)
      DiscountsTypeBlocks(
        uiState = uiState,
        onAbsoluteQuantityChanged = onAbsoluteQuantityChanged,
        onPercentageQuantityChanged = onPercentageQuantityChanged,
        onFreeFormQuantityChanged = onFreeFormQuantityChanged,
        onFreeFormPriceChanged = onFreeFormPriceChanged,
      )
      DiscountsApplyButton(onApply)
    }
  }
}

@Composable
private fun DiscountsFormHeader(uiState: DiscountsUiState) {
  val quantityStr =
    if (uiState.quantity % 1 ==
      0.0
    ) {
      uiState.quantity.toInt().toString()
    } else {
      uiState.quantity.toString()
    }

  Text(
    text = "${uiState.name} (${uiState.expiresAt})",
    color = BorealColors.White,
    fontFamily = NunitoSansFamily,
    fontSize = BorealFontSizes.Title,
    fontWeight = FontWeight.Bold,
    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
    textAlign = TextAlign.Center,
  )

  uiState.formErrorMessage?.let { ErrorCard(message = it) }

  DiscountsInfoRow(stringResource(R.string.discounts_quantity_label), "$quantityStr ${uiState.unitName}")

  Text(
    text = stringResource(R.string.discounts_available_label),
    color = BorealColors.White,
    fontFamily = NunitoSansFamily,
    fontSize = BorealFontSizes.Input,
    fontWeight = FontWeight.Bold,
    modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
  )
}

@Composable
private fun DiscountsTypeBlocks(
  uiState: DiscountsUiState,
  onAbsoluteQuantityChanged: (String) -> Unit,
  onPercentageQuantityChanged: (String) -> Unit,
  onFreeFormQuantityChanged: (String) -> Unit,
  onFreeFormPriceChanged: (String) -> Unit,
) {
  uiState.absoluteDiscount?.let { discount ->
    DiscountTypeBlock(
      typeLabel = stringResource(R.string.discounts_type_absolute),
      discount = discount,
      rateText = "${discount.amount?.toInt() ?: 0} Ft",
    ) {
      BorealTextInput(
        label = stringResource(R.string.discounts_quantity_input_label),
        value = uiState.absoluteQuantityText,
        onValueChange = onAbsoluteQuantityChanged,
        isError = uiState.absoluteQuantityError,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
      )
    }
  }

  uiState.percentageDiscount?.let { discount ->
    DiscountTypeBlock(
      typeLabel = stringResource(R.string.discounts_type_percentage),
      discount = discount,
      rateText = "${discount.amount?.toInt() ?: 0}%",
    ) {
      BorealTextInput(
        label = stringResource(R.string.discounts_quantity_input_label),
        value = uiState.percentageQuantityText,
        onValueChange = onPercentageQuantityChanged,
        isError = uiState.percentageQuantityError,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
      )
    }
  }

  uiState.freeFormDiscount?.let { discount ->
    DiscountTypeBlock(
      typeLabel = stringResource(R.string.discounts_type_free_form),
      discount = discount,
      rateText = null,
    ) {
      BorealTextInput(
        label = stringResource(R.string.discounts_quantity_input_label),
        value = uiState.freeFormQuantityText,
        onValueChange = onFreeFormQuantityChanged,
        isError = uiState.freeFormQuantityError,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
      )
      BorealTextInput(
        label = stringResource(R.string.discounts_price_input_label),
        value = uiState.freeFormPriceText,
        onValueChange = onFreeFormPriceChanged,
        isError = uiState.freeFormPriceError,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.padding(top = 8.dp),
      )
    }
  }
}

@Composable
private fun DiscountsApplyButton(onApply: () -> Unit) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 20.dp),
    horizontalArrangement = Arrangement.Center,
  ) {
    BorealButton(
      text = stringResource(R.string.discounts_apply_button),
      variant = ButtonVariant.OK,
      onClick = onApply,
    )
  }
}

@Composable
private fun DiscountTypeBlock(
  typeLabel: String,
  discount: Discount,
  rateText: String?,
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit,
) {
  Column(
    modifier =
      modifier
        .fillMaxWidth()
        .padding(top = 8.dp),
  ) {
    HorizontalDivider(color = Color.White, thickness = 1.dp)
    Column(modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)) {
      DiscountsInfoRow(stringResource(R.string.discounts_type_label), typeLabel)
      DiscountsInfoRow(stringResource(R.string.discounts_name_label), discount.name)
      rateText?.let { DiscountsInfoRow(stringResource(R.string.discounts_rate_label), it) }
    }
    content()
  }
}

@Composable
private fun DiscountsInfoRow(
  label: String,
  value: String,
) {
  Row(
    modifier =
      Modifier
        .fillMaxWidth()
        .padding(vertical = 2.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Text(
      text = label,
      color = BorealColors.White,
      fontFamily = NunitoSansFamily,
      fontSize = BorealFontSizes.Input,
      fontWeight = FontWeight.Bold,
    )
    Text(
      text = value,
      color = BorealColors.White,
      fontFamily = NunitoSansFamily,
      fontSize = BorealFontSizes.Input,
    )
  }
}
