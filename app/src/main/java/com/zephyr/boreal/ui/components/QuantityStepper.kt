package com.zephyr.boreal.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.zephyr.boreal.R
import com.zephyr.boreal.ui.theme.BorealColors

@Suppress("LongMethod", "MagicNumber")
@Composable
fun QuantityStepper(
  label: String,
  quantity: Double?,
  maxQuantity: Double?,
  onQuantityChange: (Double?) -> Unit,
  modifier: Modifier = Modifier,
) {
  val currentQty = quantity ?: 0.0

  val handleIncrease = {
    val next = currentQty + 1.0
    if (maxQuantity == null || next <= maxQuantity) {
      onQuantityChange(next)
    } else {
      onQuantityChange(maxQuantity)
    }
  }

  val handleDecrease = {
    val next = currentQty - 1.0
    if (next <= 0) {
      onQuantityChange(null)
    } else {
      onQuantityChange(next)
    }
  }

  val handleTextChange = { text: String ->
    val cleanText = text.trim().replace(",", ".")
    val num = cleanText.toDoubleOrNull()
    if (num == null || num <= 0) {
      onQuantityChange(null)
    } else if (maxQuantity != null && num > maxQuantity) {
      onQuantityChange(maxQuantity)
    } else {
      onQuantityChange(num)
    }
  }

  Column(
    modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(
      text = label,
      color = Color.White,
      style = MaterialTheme.typography.bodyMedium,
      fontWeight = FontWeight.Bold,
      modifier = Modifier.padding(bottom = 4.dp),
    )
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center,
      modifier = Modifier.fillMaxWidth(),
    ) {
      IconButton(onClick = handleDecrease) {
        Icon(
          painter = painterResource(id = R.drawable.remove_circle),
          contentDescription = stringResource(R.string.select_items_decrease),
          tint = Color.White,
          modifier = Modifier.size(40.dp),
        )
      }

      Spacer(modifier = Modifier.width(16.dp))

      TextField(
        value =
          if (quantity != null) {
            (if (quantity % 1 == 0.0) quantity.toInt().toString() else quantity.toString())
          } else {
            ""
          },
        onValueChange = handleTextChange,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.width(140.dp),
        colors =
          TextFieldDefaults.colors(
            focusedContainerColor = BorealColors.Input,
            unfocusedContainerColor = BorealColors.Input,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
          ),
        textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Center),
        singleLine = true,
      )

      Spacer(modifier = Modifier.width(16.dp))

      IconButton(onClick = handleIncrease) {
        Icon(
          painter = painterResource(id = R.drawable.add_circle),
          contentDescription = stringResource(R.string.select_items_increase),
          tint = Color.White,
          modifier = Modifier.size(40.dp),
        )
      }
    }
  }
}
