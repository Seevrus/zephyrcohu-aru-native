package com.zephyr.boreal.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.zephyr.boreal.R
import com.zephyr.boreal.ui.screens.RoundInfoUiModel
import com.zephyr.boreal.ui.theme.BorealColors

@Composable
fun RoundInfo(
  roundInfo: RoundInfoUiModel,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier =
      modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 16.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    HorizontalDivider(
      thickness = 2.dp,
      color = BorealColors.White,
    )

    InfoRow(
      label = stringResource(R.string.round_info_partner_list),
      value = roundInfo.partnerListName,
      modifier = Modifier.padding(top = 8.dp),
    )

    InfoRow(
      label = stringResource(R.string.round_info_store),
      value = roundInfo.storeName,
    )

    InfoRow(
      label = stringResource(R.string.round_info_started),
      value = roundInfo.roundStartedDate,
    )
  }
}

@Composable
private fun InfoRow(
  label: String,
  value: String,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelLarge,
      fontWeight = FontWeight.SemiBold,
      color = BorealColors.White,
      modifier = Modifier.padding(end = 16.dp),
    )
    Text(
      text = value,
      style = MaterialTheme.typography.labelLarge,
      fontWeight = FontWeight.SemiBold,
      color = BorealColors.White,
      textAlign = TextAlign.End,
      modifier = Modifier.weight(1f),
    )
  }
}
