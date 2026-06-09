package com.zephyr.boreal.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.zephyr.boreal.ui.theme.BorealColors

@Composable
fun BorealSearchField(
  query: String,
  onQueryChange: (String) -> Unit,
  modifier: Modifier = Modifier,
  placeholderText: String? = null,
) {
  OutlinedTextField(
    value = query,
    onValueChange = onQueryChange,
    modifier = modifier.fillMaxWidth(),
    singleLine = true,
    textStyle = MaterialTheme.typography.titleLarge,
    placeholder = {
      if (placeholderText != null) {
        Text(
          text = placeholderText,
          style = MaterialTheme.typography.titleLarge,
          color = BorealColors.White.copy(alpha = 0.6f),
        )
      }
    },
    colors =
      OutlinedTextFieldDefaults.colors(
        focusedContainerColor = BorealColors.Input,
        unfocusedContainerColor = BorealColors.Input,
        focusedBorderColor = Color.Transparent,
        unfocusedBorderColor = Color.Transparent,
        focusedTextColor = BorealColors.White,
        unfocusedTextColor = BorealColors.White,
      ),
    shape = RoundedCornerShape(8.dp),
  )
}
