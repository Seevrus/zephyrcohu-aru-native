package com.zephyr.boreal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zephyr.boreal.R
import com.zephyr.boreal.domain.model.StorageListItem
import com.zephyr.boreal.ui.components.BorealAlert
import com.zephyr.boreal.ui.components.BorealTopAppBar
import com.zephyr.boreal.ui.components.ButtonVariant
import com.zephyr.boreal.ui.components.ErrorCard
import com.zephyr.boreal.ui.components.InfoCard
import com.zephyr.boreal.ui.theme.BorealColors

@Composable
fun ReviewStorageChangesScreen(
  viewModel: ReviewStorageChangesViewModel,
  onSaved: () -> Unit,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  ReviewStorageChangesScreenContent(
    uiState = uiState,
    onForward = viewModel::requestConfirm,
    onConfirm = { viewModel.confirmSend(onSaved) },
    onCancelConfirm = viewModel::dismissConfirm,
  )
}

@Composable
internal fun ReviewStorageChangesScreenContent(
  uiState: ReviewStorageChangesUiState,
  onForward: () -> Unit,
  onConfirm: () -> Unit,
  onCancelConfirm: () -> Unit,
) {
  if (uiState.showConfirmAlert) {
    BorealAlert(
      title = stringResource(R.string.review_storage_changes_confirm_title),
      message = stringResource(R.string.review_storage_changes_confirm_message),
      confirmButtonText = stringResource(R.string.review_storage_changes_confirm_button),
      confirmButtonVariant = ButtonVariant.WARNING,
      cancelButtonText = stringResource(R.string.dialog_cancel),
      onConfirmClick = onConfirm,
      onCancelClick = onCancelConfirm,
      onDismissRequest = onCancelConfirm,
    )
  }

  Scaffold(
    topBar = { ReviewStorageChangesTopBar(canConfirm = uiState.canConfirm, onForward = onForward) },
    bottomBar = { ReviewStorageChangesFooter(uiState) },
    containerColor = BorealColors.Background,
  ) { paddingValues ->
    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .padding(paddingValues)
          .padding(horizontal = 16.dp),
    ) {
      ReviewStorageChangesBanners(uiState)

      LazyColumn(modifier = Modifier.weight(1f)) {
        items(items = uiState.changedItems, key = { it.expirationId }) { item ->
          ReviewExpirationItemCard(item)
        }
      }
    }
  }
}

@Composable
private fun ReviewStorageChangesTopBar(
  canConfirm: Boolean,
  onForward: () -> Unit,
) {
  BorealTopAppBar(
    title = stringResource(R.string.screen_review_storage_changes_title),
    actions = {
      if (canConfirm) {
        IconButton(onClick = onForward) {
          Icon(
            painter = painterResource(id = R.drawable.arrow_forward),
            contentDescription = stringResource(R.string.review_storage_changes_confirm_button),
            tint = BorealColors.White,
          )
        }
      }
    },
  )
}

@Composable
private fun ReviewStorageChangesFooter(uiState: ReviewStorageChangesUiState) {
  Column {
    HorizontalDivider(color = Color.White, thickness = 1.dp)
    Row(
      modifier =
        Modifier
          .fillMaxWidth()
          .background(BorealColors.Neutral)
          .padding(16.dp),
      horizontalArrangement = Arrangement.End,
    ) {
      Text(
        text =
          stringResource(
            R.string.review_storage_changes_total,
            uiState.changedItems.size,
            uiState.up,
            uiState.down,
          ),
        color = BorealColors.White,
        fontWeight = FontWeight.Bold,
      )
    }
  }
}

@Composable
private fun ReviewStorageChangesBanners(uiState: ReviewStorageChangesUiState) {
  if (!uiState.isInternetReachable) {
    InfoCard(message = stringResource(R.string.review_storage_changes_offline_notice))
  }
  if (uiState.reallyUnexpectedBlocker) {
    ErrorCard(message = stringResource(R.string.review_storage_changes_no_primary_store))
  }
  if (uiState.isError) {
    ErrorCard(message = stringResource(R.string.review_storage_changes_save_error))
  }
}

@Composable
private fun ReviewExpirationItemCard(item: StorageListItem) {
  Column(
    modifier =
      Modifier
        .fillMaxWidth()
        .padding(vertical = 10.dp)
        .background(BorealColors.Neutral, RoundedCornerShape(10.dp))
        .padding(16.dp),
  ) {
    Row(modifier = Modifier.fillMaxWidth()) {
      Text(
        text = item.name,
        color = BorealColors.White,
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.weight(1f),
      )
      Text(
        text = item.expiresAt,
        color = BorealColors.White,
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.titleLarge,
      )
    }

    val barcode = "${item.itemBarcode} ${item.expirationBarcode}".trim()
    if (barcode.isNotEmpty()) {
      ReviewDetailRow(label = stringResource(R.string.review_storage_changes_barcode_label), value = barcode)
    }
    ReviewDetailRow(
      label = stringResource(R.string.review_storage_changes_quantity_change_label),
      value = "${item.quantityChange ?: 0}",
    )
    ReviewDetailRow(
      label = stringResource(R.string.review_storage_changes_final_quantity_label),
      value = "${(item.originalQuantity ?: 0.0).toInt() + (item.quantityChange ?: 0)}",
    )
  }
}

@Composable
private fun ReviewDetailRow(
  label: String,
  value: String,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Text(text = label, color = BorealColors.White)
    Text(text = value, color = BorealColors.White)
  }
}
