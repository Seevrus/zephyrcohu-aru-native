package com.zephyr.boreal

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import com.zephyr.boreal.ui.screens.RoundDetailsScreen
import com.zephyr.boreal.ui.screens.RoundDetailsViewModel

@Composable
internal fun RoundDetailsRoute() {
  val viewModel: RoundDetailsViewModel = hiltViewModel()
  RoundDetailsScreen(viewModel = viewModel)
}
