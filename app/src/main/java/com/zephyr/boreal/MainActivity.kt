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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.zephyr.boreal.ui.screens.AppLockedScreen
import com.zephyr.boreal.ui.screens.MainScreen
import com.zephyr.boreal.ui.screens.PrintSettingsScreen
import com.zephyr.boreal.ui.screens.SettingsScreen
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
          val navController = rememberNavController()

          NavHost(navController = navController, startDestination = "main") {
            composable("main") {
              MainScreen(
                onNavigateToAppLocked = {
                  navController.navigate("app_locked") {
                    popUpTo(0) { inclusive = true }
                  }
                },
                onNavigateToSettings = {
                  navController.navigate("settings")
                },
              )
            }
            composable("app_locked") {
              AppLockedScreen(
                onNavigateToMain = {
                  navController.navigate("main") {
                    popUpTo(0) { inclusive = true }
                  }
                },
                onNavigateToSettings = {
                  navController.navigate("settings")
                },
              )
            }
            composable("settings") {
              SettingsScreen(
                onNavigateToPrintSettings = {
                  navController.navigate("print_settings")
                },
              )
            }
            composable("print_settings") {
              PrintSettingsScreen(
                onNavigateBack = {
                  navController.popBackStack()
                },
              )
            }
          }
        }
      }
    }
  }
}
