package com.zephyr.boreal.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.zephyr.boreal.ui.theme.BorealColors
import com.zephyr.boreal.ui.theme.BorealFontSizes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BorealTopAppBar(
  title: String,
  modifier: Modifier = Modifier,
  navigationIcon: @Composable () -> Unit = {},
  actions: @Composable RowScope.() -> Unit = {},
) {
  CenterAlignedTopAppBar(
    modifier = modifier,
    colors =
      TopAppBarDefaults.topAppBarColors(
        containerColor = BorealColors.Neutral,
        titleContentColor = BorealColors.White,
      ),
    title = {
      Text(
        text = title,
        fontSize = BorealFontSizes.Title,
        fontWeight = FontWeight.Bold,
      )
    },
    navigationIcon = navigationIcon,
    actions = actions,
  )
}
