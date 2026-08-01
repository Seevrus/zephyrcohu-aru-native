package com.zephyr.boreal

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import com.zephyr.boreal.ui.screens.ReceiptDetailsScreen
import com.zephyr.boreal.ui.screens.ReceiptDetailsViewModel

@Composable
internal fun ReceiptDetailsRoute() {
  val viewModel: ReceiptDetailsViewModel = hiltViewModel()
  ReceiptDetailsScreen(viewModel = viewModel)
}
