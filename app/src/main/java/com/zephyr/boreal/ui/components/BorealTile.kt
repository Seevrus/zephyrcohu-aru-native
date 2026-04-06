package com.zephyr.boreal.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.zephyr.boreal.ui.theme.BorealColors

/**
 * Reusable Tile component for the Boreal application, redesigned as a horizontal ribbon.
 */
@Composable
fun BorealTile(
  title: String,
  variant: TileVariant,
  modifier: Modifier = Modifier,
  icon: ImageVector? = null,
  onClick: () -> Unit = {},
) {
  val (backgroundColor, rippleColor) = getTileColors(variant)

  Card(
    modifier =
      modifier
        .fillMaxWidth()
        .height(72.dp),
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = backgroundColor),
    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
  ) {
    TileContent(
      title = title,
      icon = icon,
      rippleColor = rippleColor,
      onClick = onClick,
    )
  }
}

@Composable
private fun TileContent(
  title: String,
  icon: ImageVector?,
  rippleColor: Color,
  onClick: () -> Unit,
) {
  Row(
    modifier =
      Modifier
        .fillMaxSize()
        .clickable(
          interactionSource = remember { MutableInteractionSource() },
          indication = ripple(color = rippleColor),
          onClick = onClick,
        ),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    if (icon != null) {
      Box(
        modifier =
          Modifier
            .width(72.dp)
            .fillMaxHeight(),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = Color.White,
          modifier = Modifier.size(50.dp),
        )
      }
    }

    Text(
      text = title,
      style = MaterialTheme.typography.bodyLarge,
      modifier = Modifier.padding(start = 16.dp),
    )
  }
}

private fun getTileColors(variant: TileVariant): Pair<Color, Color> =
  when (variant) {
    TileVariant.OK -> BorealColors.Ok to BorealColors.OkRipple
    TileVariant.WARNING -> BorealColors.Warning to BorealColors.WarningRipple
    TileVariant.DISABLED -> BorealColors.Disabled to BorealColors.DisabledRipple
    TileVariant.NEUTRAL -> BorealColors.Neutral to BorealColors.NeutralRipple
  }
