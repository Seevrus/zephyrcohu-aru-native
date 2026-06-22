package com.zephyr.boreal.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.zephyr.boreal.ui.theme.BorealColors
import com.zephyr.boreal.ui.theme.BorealFontSizes
import com.zephyr.boreal.ui.theme.NunitoSansFamily
import com.zephyr.boreal.ui.theme.RobotoFamily

@Composable
fun BorealAlert(
  title: String,
  modifier: Modifier = Modifier,
  message: String? = null,
  confirmButtonText: String? = null,
  confirmButtonVariant: ButtonVariant = ButtonVariant.OK,
  cancelButtonText: String? = null,
  onConfirmClick: () -> Unit = {},
  onCancelClick: () -> Unit = {},
  onDismissRequest: () -> Unit = {},
) {
  Dialog(onDismissRequest = onDismissRequest) {
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = BorealColors.Background,
      modifier = modifier.fillMaxWidth(),
    ) {
      Column(
        modifier = Modifier.padding(24.dp),
      ) {
        Text(
          text = title,
          color = BorealColors.White,
          fontFamily = RobotoFamily,
          fontSize = BorealFontSizes.Body,
          fontWeight = FontWeight.Bold,
        )

        if (message != null) {
          Spacer(modifier = Modifier.height(16.dp))
          Text(
            text = message,
            color = BorealColors.White,
            fontFamily = NunitoSansFamily,
            fontSize = BorealFontSizes.Input,
          )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Spacer(modifier = Modifier.weight(1f))
          if (cancelButtonText != null) {
            BorealButton(
              text = cancelButtonText,
              variant = ButtonVariant.NEUTRAL,
              onClick = onCancelClick,
            )
          }
          if (confirmButtonText != null) {
            if (cancelButtonText != null) {
              Spacer(modifier = Modifier.width(8.dp))
            }
            BorealButton(
              text = confirmButtonText,
              variant = confirmButtonVariant,
              onClick = onConfirmClick,
            )
          }
        }
      }
    }
  }
}
