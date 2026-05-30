package com.zephyr.boreal.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.zephyr.boreal.R
import com.zephyr.boreal.ui.components.BorealButton
import com.zephyr.boreal.ui.components.BorealTopAppBar
import com.zephyr.boreal.ui.components.ButtonVariant
import com.zephyr.boreal.ui.theme.BorealColors
import com.zephyr.boreal.ui.theme.BorealFontSizes
import com.zephyr.boreal.ui.theme.NunitoSansFamily

@Composable
fun PrintEndErrandScreen(onNavigateHome: () -> Unit) {
  Scaffold(
    topBar = {
      BorealTopAppBar(
        title = stringResource(R.string.screen_print_end_errand_title),
      )
    },
    containerColor = BorealColors.Background,
  ) { paddingValues ->
    Box(
      modifier =
        Modifier
          .fillMaxSize()
          .padding(paddingValues),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = "TODO: Implement Print End Errand Screen",
        color = BorealColors.White,
        fontFamily = NunitoSansFamily,
        fontSize = BorealFontSizes.Body,
      )

      BorealButton(
        text = stringResource(R.string.print_end_errand_home_button),
        variant = ButtonVariant.OK,
        onClick = onNavigateHome,
        modifier = Modifier.align(Alignment.BottomCenter),
      )
    }
  }
}
