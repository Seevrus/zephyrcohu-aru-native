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
fun ReviewStorageChangesScreen(onNavigateHome: () -> Unit) {
  Scaffold(
    topBar = {
      BorealTopAppBar(
        title = stringResource(R.string.screen_review_storage_changes_title),
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
        text = "TODO: Implement Review Storage Changes Screen",
        color = BorealColors.White,
        fontFamily = NunitoSansFamily,
        fontSize = BorealFontSizes.Body,
      )

      BorealButton(
        text = stringResource(R.string.select_items_from_store_home_button),
        variant = ButtonVariant.OK,
        onClick = onNavigateHome,
        modifier = Modifier.align(Alignment.BottomCenter),
      )
    }
  }
}
