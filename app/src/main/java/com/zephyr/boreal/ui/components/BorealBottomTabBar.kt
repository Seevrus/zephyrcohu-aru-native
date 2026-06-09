package com.zephyr.boreal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zephyr.boreal.ui.theme.BorealColors

@Composable
fun BorealBottomTabBar(
  tabs: List<BorealTabItem>,
  selectedIndex: Int,
  onTabSelected: (Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier =
      modifier
        .fillMaxWidth()
        .background(BorealColors.Neutral)
        .padding(vertical = 8.dp),
    horizontalArrangement = Arrangement.SpaceEvenly,
  ) {
    tabs.forEachIndexed { index, tab ->
      val isSelected = selectedIndex == index
      val color = if (isSelected) BorealColors.Blue200 else BorealColors.White

      Column(
        modifier =
          Modifier
            .clickable(onClick = { onTabSelected(index) })
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Icon(
          painter = painterResource(id = tab.iconRes),
          contentDescription = tab.text,
          tint = color,
          modifier = Modifier.size(24.dp),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = tab.text,
          style = MaterialTheme.typography.titleLarge,
          color = color,
          fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
        )
      }
    }
  }
}
