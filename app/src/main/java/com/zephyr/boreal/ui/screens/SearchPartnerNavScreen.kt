package com.zephyr.boreal.ui.screens

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun SearchPartnerNavScreen(
  viewModel: SearchPartnerNavViewModel,
  onNavigateToAddPartner: (taxNumber: String, selectedIndex: Int) -> Unit,
  onNavigateToManualEntry: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Text("SearchPartnerNav — coming soon")
}
