package com.zephyr.boreal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zephyr.boreal.R
import com.zephyr.boreal.ui.theme.BorealColors
import com.zephyr.boreal.ui.theme.BorealFontSizes
import com.zephyr.boreal.ui.theme.NunitoSansFamily

@Suppress("LongMethod")
@Composable
fun BorealDropdown(
  label: String,
  data: List<DropdownItem>,
  selectedKey: String?,
  onSelect: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  var expanded by remember { mutableStateOf(false) }
  val selectedValue =
    data.find { it.key == selectedKey }?.value
      ?: stringResource(R.string.dropdown_default_placeholder)

  Column(modifier = modifier) {
    Text(
      text = label,
      color = BorealColors.White,
      fontFamily = NunitoSansFamily,
      fontWeight = FontWeight.Bold,
      fontSize = BorealFontSizes.Input,
      modifier = Modifier.padding(bottom = 8.dp),
    )

    BoxWithConstraints {
      val menuWidth = maxWidth
      Box(
        modifier =
          Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(BorealColors.Input)
            .clickable { expanded = true }
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = selectedValue,
            color = BorealColors.White,
            fontFamily = NunitoSansFamily,
            fontSize = BorealFontSizes.Input,
            modifier = Modifier.weight(1f),
          )

          Icon(
            painter =
              painterResource(
                id = if (expanded) R.drawable.arrow_drop_up else R.drawable.arrow_drop_down,
              ),
            contentDescription = null,
            tint = BorealColors.White,
            modifier = Modifier.size(24.dp),
          )
        }
      }

      DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false },
        modifier =
          Modifier
            .width(menuWidth)
            .background(BorealColors.Input),
      ) {
        data.forEach { item ->
          DropdownMenuItem(
            text = {
              Text(
                text = item.value,
                fontFamily = NunitoSansFamily,
                fontSize = BorealFontSizes.Input,
                color = BorealColors.White,
              )
            },
            onClick = {
              onSelect(item.key)
              expanded = false
            },
          )
        }
      }
    }
  }
}

data class DropdownItem(
  val key: String,
  val value: String,
)
