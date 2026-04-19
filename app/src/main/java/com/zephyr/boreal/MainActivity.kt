package com.zephyr.boreal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.zephyr.boreal.ui.screens.AppLockedScreen
import com.zephyr.boreal.ui.screens.LoginScreen
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
        BorealApp()
      }
    }
  }
}

@Composable
fun BorealApp() {
  Surface(
    modifier = Modifier.fillMaxSize(),
    color = BorealColors.Background,
  ) {
    val navController = rememberNavController()
    BorealNavHost(navController = navController)
  }
}

@Composable
fun BorealNavHost(navController: androidx.navigation.NavHostController) {
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
        onNavigateToLogin = {
          navController.navigate("login")
        },
      )
    }
    composable("login") {
      LoginScreen(
        onLoginSuccess = {
          navController.navigate("main") {
            popUpTo(0) { inclusive = true }
          }
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
        onLogout = {
          navController.navigate("main") {
            popUpTo(0) { inclusive = true }
          }
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
