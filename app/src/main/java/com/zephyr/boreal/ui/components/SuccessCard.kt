package com.zephyr.boreal.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zephyr.boreal.R
import com.zephyr.boreal.ui.theme.BorealColors
import com.zephyr.boreal.ui.theme.BorealFontSizes
import com.zephyr.boreal.ui.theme.NunitoSansFamily

@Composable
fun SuccessCard(
  message: String,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier =
      modifier
        .fillMaxWidth()
        .padding(vertical = 16.dp),
    shape = RoundedCornerShape(8.dp),
    colors =
      CardDefaults.cardColors(
        containerColor = BorealColors.Ok,
        contentColor = BorealColors.White,
      ),
  ) {
    Row(
      modifier = Modifier.padding(8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = message,
        modifier = Modifier.weight(1f),
        color = BorealColors.White,
        fontFamily = NunitoSansFamily,
        fontWeight = FontWeight.Normal,
        fontSize = BorealFontSizes.Body,
      )
      Icon(
        imageVector = ImageVector.vectorResource(id = R.drawable.info_outlined),
        contentDescription = "success_icon",
        modifier =
          Modifier
            .padding(start = 4.dp)
            .size(36.dp),
        tint = BorealColors.White,
      )
    }
  }
}
