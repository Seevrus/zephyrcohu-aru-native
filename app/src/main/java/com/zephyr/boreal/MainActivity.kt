package com.zephyr.boreal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.zephyr.boreal.ui.screens.MainScreen
import com.zephyr.boreal.ui.theme.BorealColors
import com.zephyr.boreal.ui.theme.BorealTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
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
