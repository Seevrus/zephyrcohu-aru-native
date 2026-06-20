package com.zephyr.boreal.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zephyr.boreal.R
import com.zephyr.boreal.domain.model.LocationType
import com.zephyr.boreal.domain.model.Partner
import com.zephyr.boreal.ui.components.BorealBottomTabBar
import com.zephyr.boreal.ui.components.BorealSearchField
import com.zephyr.boreal.ui.components.BorealTabItem
import com.zephyr.boreal.ui.components.BorealTopAppBar
import com.zephyr.boreal.ui.theme.BorealColors
import kotlinx.coroutines.delay

private const val ANIMATION_DURATION_MS = 300

@Composable
fun SelectPartnerScreen(
  viewModel: SelectPartnerViewModel,
  onNavigateNext: () -> Unit = {},
  onNavigateToAddPartner: (isInternetReachable: Boolean) -> Unit = {},
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  SelectPartnerScreenContent(
    uiState = uiState,
    onSearchQueryChanged = viewModel::onSearchQueryChanged,
    onTabSelected = viewModel::onTabSelected,
    onTogglePartnerExpanded = viewModel::onTogglePartnerExpanded,
    onAddPartnerClick = { onNavigateToAddPartner(uiState.isInternetReachable) },
    onSelectClick = {
      viewModel.selectPartner { onNavigateNext() }
    },
  )
}

@Composable
fun SelectPartnerScreenContent(
  uiState: SelectPartnerUiState,
  onSearchQueryChanged: (String) -> Unit,
  onTabSelected: (PartnerTab) -> Unit,
  onTogglePartnerExpanded: (Int) -> Unit,
  onAddPartnerClick: () -> Unit,
  onSelectClick: (Int) -> Unit,
) {
  val tabs = getPartnerTabs()

  Scaffold(
    topBar = {
      SelectPartnerTopBar(
        canAddPartner = uiState.canAddPartner,
        onAddPartnerClick = onAddPartnerClick,
      )
    },
    bottomBar = {
      BorealBottomTabBar(
        tabs = tabs,
        selectedIndex = if (uiState.selectedTab == PartnerTab.ROUND_STORES) 0 else 1,
        onTabSelected = { index ->
          onTabSelected(if (index == 0) PartnerTab.ROUND_STORES else PartnerTab.ALL_STORES)
        },
      )
    },
    containerColor = BorealColors.Background,
  ) { paddingValues ->
    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .padding(paddingValues),
    ) {
      BorealSearchField(
        query = uiState.searchQuery,
        onQueryChange = onSearchQueryChanged,
        modifier = Modifier.padding(16.dp),
        placeholderText = "${stringResource(R.string.select_partner_search)}...",
      )

      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp),
      ) {
        items(
          items = uiState.partners,
          key = { it.id },
        ) { partner ->
          val isExpanded = uiState.expandedPartnerIds.contains(partner.id)
          PartnerAccordionItem(
            partner = partner,
            isExpanded = isExpanded,
            onHeaderClick = { onTogglePartnerExpanded(partner.id) },
            onSelectClick = { onSelectClick(partner.id) },
          )
        }
      }
    }
  }
}

@Composable
private fun getPartnerTabs(): List<BorealTabItem> =
  listOf(
    BorealTabItem(
      text = stringResource(R.string.select_partner_tab_round),
      iconRes = R.drawable.store,
    ),
    BorealTabItem(
      text = stringResource(R.string.select_partner_tab_all),
      iconRes = R.drawable.all_inclusive,
    ),
  )

@Composable
private fun SelectPartnerTopBar(
  canAddPartner: Boolean,
  onAddPartnerClick: () -> Unit,
) {
  BorealTopAppBar(
    title = stringResource(R.string.select_partner_title),
    actions = {
      if (canAddPartner) {
        IconButton(onClick = onAddPartnerClick) {
          Icon(
            painter = painterResource(id = R.drawable.add_circle),
            contentDescription = stringResource(R.string.select_partner_add_btn),
            tint = BorealColors.White,
            modifier = Modifier.size(32.dp),
          )
        }
      }
    },
  )
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun PartnerAccordionItem(
  partner: Partner,
  isExpanded: Boolean,
  onHeaderClick: () -> Unit,
  onSelectClick: () -> Unit,
) {
  val headerColor = if (isExpanded) BorealColors.Ok else BorealColors.Neutral
  val bringIntoViewRequester = remember { BringIntoViewRequester() }

  LaunchedEffect(isExpanded) {
    if (isExpanded) {
      delay(ANIMATION_DURATION_MS.toLong() / 2) // Wait until partially expanded to determine size
      bringIntoViewRequester.bringIntoView()
    }
  }

  Column(
    modifier =
      Modifier
        .bringIntoViewRequester(bringIntoViewRequester)
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp)
        .clip(RoundedCornerShape(8.dp))
        .background(BorealColors.Neutral),
  ) {
    PartnerAccordionHeader(
      partnerName = partner.name,
      headerColor = headerColor,
      onHeaderClick = onHeaderClick,
    )

    AnimatedVisibility(
      visible = isExpanded,
      enter = expandVertically(animationSpec = tween(ANIMATION_DURATION_MS)),
      exit = shrinkVertically(animationSpec = tween(ANIMATION_DURATION_MS)),
    ) {
      PartnerAccordionExpandedContent(
        partner = partner,
        onSelectClick = onSelectClick,
      )
    }
  }
}

@Composable
private fun PartnerAccordionHeader(
  partnerName: String,
  headerColor: Color,
  onHeaderClick: () -> Unit,
) {
  Row(
    modifier =
      Modifier
        .fillMaxWidth()
        .background(headerColor)
        .clickable(onClick = onHeaderClick)
        .padding(16.dp),
  ) {
    Text(
      text = partnerName,
      style = MaterialTheme.typography.titleLarge,
      color = BorealColors.White,
      fontWeight = FontWeight.Bold,
    )
  }
}

@Composable
private fun PartnerAccordionExpandedContent(
  partner: Partner,
  onSelectClick: () -> Unit,
) {
  Column(
    modifier =
      Modifier
        .fillMaxWidth()
        .padding(16.dp),
  ) {
    val deliveryLocation = partner.locations.find { it.locationType == LocationType.DELIVERY }
    val hqLocation = partner.locations.find { it.locationType == LocationType.CENTRAL }

    if (deliveryLocation != null) {
      LocationText(
        label = null,
        postalCode = deliveryLocation.postalCode,
        city = deliveryLocation.city,
        address = deliveryLocation.address,
      )
    }

    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = BorealColors.White.copy(alpha = 0.5f))

    if (hqLocation != null) {
      LocationText(
        label = "HQ for ${partner.name}",
        postalCode = hqLocation.postalCode,
        city = hqLocation.city,
        address = hqLocation.address,
      )
    }

    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = BorealColors.White.copy(alpha = 0.5f))

    PartnerContactInfo(partner = partner)

    Spacer(modifier = Modifier.height(16.dp))

    Button(
      onClick = onSelectClick,
      modifier = Modifier.align(Alignment.CenterHorizontally),
      contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
      colors = ButtonDefaults.buttonColors(containerColor = BorealColors.Ok),
    ) {
      Text(
        text = stringResource(R.string.select_partner_select_btn),
        style = MaterialTheme.typography.titleLarge,
        color = BorealColors.White,
        fontWeight = FontWeight.Bold,
      )
    }
  }
}

@Composable
private fun LocationText(
  label: String?,
  postalCode: String,
  city: String,
  address: String,
) {
  if (label != null) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodyMedium,
      color = BorealColors.White,
    )
  }
  Text(
    text = "$postalCode $city, $address",
    style = MaterialTheme.typography.bodyMedium,
    color = BorealColors.White,
  )
}

@Composable
private fun PartnerContactInfo(partner: Partner) {
  val paymentMethod =
    if (partner.paymentDays > 0) {
      stringResource(R.string.payment_method_wire_transfer_abbr)
    } else {
      stringResource(R.string.payment_method_cash_abbr)
    }

  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text =
        buildAnnotatedString {
          withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
            append("${stringResource(R.string.select_partner_phone)}: ")
          }
          append(partner.phoneNumber ?: "-")
        },
      style = MaterialTheme.typography.bodyMedium,
      color = BorealColors.White,
    )
    Text(
      text =
        buildAnnotatedString {
          withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
            append("Fm: ")
          }
          append(paymentMethod)
        },
      style = MaterialTheme.typography.bodyMedium,
      color = BorealColors.White,
    )
  }
}
