package com.zephyr.boreal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.zephyr.boreal.ui.theme.BorealColors
import com.zephyr.boreal.ui.theme.BorealFontSizes
import com.zephyr.boreal.ui.theme.NunitoSansFamily

@Composable
fun BorealTextInput(
  label: String,
  value: String,
  onValueChange: (String) -> Unit,
  modifier: Modifier = Modifier,
  isError: Boolean = false,
  enabled: Boolean = true,
  maxLength: Int? = null,
  keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
  keyboardActions: KeyboardActions = KeyboardActions.Default,
  visualTransformation: VisualTransformation = VisualTransformation.None,
) {
  Column(modifier = modifier) {
    Text(
      text = label,
      color = BorealColors.White,
      fontFamily = NunitoSansFamily,
      fontWeight = FontWeight.Bold,
      fontSize = BorealFontSizes.Input,
      modifier = Modifier.padding(bottom = 8.dp),
    )

    val borderModifier =
      if (isError) {
        Modifier.border(2.dp, BorealColors.Error, RoundedCornerShape(4.dp))
      } else {
        Modifier
      }

    BasicTextField(
      value = value,
      onValueChange = {
        if (maxLength == null || it.length <= maxLength) {
          onValueChange(it)
        }
      },
      modifier =
        Modifier
          .fillMaxWidth()
          .height(56.dp)
          .background(
            if (enabled) BorealColors.Input else BorealColors.Disabled,
            RoundedCornerShape(4.dp),
          ).then(borderModifier),
      enabled = enabled,
      textStyle =
        TextStyle(
          color = BorealColors.White,
          fontFamily = NunitoSansFamily,
          fontSize = BorealFontSizes.Input,
        ),
      cursorBrush = SolidColor(BorealColors.White),
      keyboardOptions = keyboardOptions,
      keyboardActions = keyboardActions,
      visualTransformation = visualTransformation,
      singleLine = true,
      decorationBox = { innerTextField ->
        Box(
          modifier = Modifier.padding(horizontal = 16.dp),
          contentAlignment = Alignment.CenterStart,
        ) {
          innerTextField()
        }
      },
    )
  }
}
