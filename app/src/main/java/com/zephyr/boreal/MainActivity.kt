package com.zephyr.boreal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.zephyr.boreal.ui.components.BorealTile
import com.zephyr.boreal.ui.components.TileVariant
import com.zephyr.boreal.ui.theme.BorealColors
import com.zephyr.boreal.ui.theme.BorealFontSizes
import com.zephyr.boreal.ui.theme.BorealTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    installSplashScreen()
    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.dark(Color.Transparent.toArgb()),
      navigationBarStyle = SystemBarStyle.dark(Color.Transparent.toArgb()),
    )
    super.onCreate(savedInstanceState)
    setContent {
      BorealTheme {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = BorealColors.Background,
        ) {
          MainScreen()
        }
      }
    }
  }
}

data class TileData(
  val title: String,
  val variant: TileVariant,
  val icon: ImageVector,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
  val tiles =
    listOf(
      TileData("Status", TileVariant.OK, ImageVector.vectorResource(R.drawable.truck_solid_full)),
      TileData("Alerts", TileVariant.WARNING, ImageVector.vectorResource(R.drawable.cart_arrow_down_solid_full)),
      TileData("Settings", TileVariant.NEUTRAL, ImageVector.vectorResource(R.drawable.rectangle_list_solid_full)),
      TileData("History", TileVariant.DISABLED, ImageVector.vectorResource(R.drawable.receipt_solid_full)),
    )

  Scaffold(
    topBar = {
      CenterAlignedTopAppBar(
        colors =
          TopAppBarDefaults.topAppBarColors(
            containerColor = BorealColors.Background,
            titleContentColor = BorealColors.White,
          ),
        title = { Text("Zephyr Boreal", fontSize = BorealFontSizes.Title, fontWeight = FontWeight.Bold) },
      )
    },
    containerColor = BorealColors.Background,
  ) { innerPadding ->
    LazyColumn(
      contentPadding = PaddingValues(16.dp),
      modifier =
        Modifier
          .padding(innerPadding)
          .fillMaxSize(),
    ) {
      items(tiles) { tile ->
        BorealTile(
          title = tile.title,
          variant = tile.variant,
          icon = tile.icon,
          modifier = Modifier.padding(bottom = 16.dp),
        )
      }
    }
  }
}
