@file:Suppress("MatchingDeclarationName")

package com.zephyr.boreal.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zephyr.boreal.ui.theme.BorealColors
import com.zephyr.boreal.ui.theme.BorealFontSizes
import com.zephyr.boreal.ui.theme.NunitoSansFamily

enum class ButtonVariant {
  NEUTRAL,
  OK,
  WARNING,
  ERROR,
  DISABLED,
}

@Composable
fun BorealButton(
  text: String,
  variant: ButtonVariant = ButtonVariant.NEUTRAL,
  modifier: Modifier = Modifier,
  onClick: () -> Unit,
) {
  val backgroundColor =
    when (variant) {
      ButtonVariant.NEUTRAL -> BorealColors.Neutral
      ButtonVariant.OK -> BorealColors.Ok
      ButtonVariant.WARNING -> BorealColors.Warning
      ButtonVariant.ERROR -> BorealColors.Error
      ButtonVariant.DISABLED -> BorealColors.Disabled
    }

  val textColor = BorealColors.White

  Surface(
    onClick = onClick,
    enabled = variant != ButtonVariant.DISABLED,
    shape = RoundedCornerShape(8.dp),
    color = backgroundColor,
    modifier = modifier,
  ) {
    Box(
      contentAlignment = Alignment.Center,
      modifier =
        Modifier
          .height(50.dp)
          .padding(horizontal = 24.dp),
    ) {
      Text(
        text = text,
        color = textColor,
        fontFamily = NunitoSansFamily,
        fontSize = BorealFontSizes.Input,
        fontWeight = FontWeight.Bold,
      )
    }
  }
}
