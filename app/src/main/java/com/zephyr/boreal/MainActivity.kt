package com.zephyr.boreal

import android.net.Uri
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.zephyr.boreal.ui.screens.AddPartnerScreen
import com.zephyr.boreal.ui.screens.AddPartnerViewModel
import com.zephyr.boreal.ui.screens.AppLockedScreen
import com.zephyr.boreal.ui.screens.ChangePasswordScreen
import com.zephyr.boreal.ui.screens.DiscountsScreen
import com.zephyr.boreal.ui.screens.DiscountsViewModel
import com.zephyr.boreal.ui.screens.EndErrandScreen
import com.zephyr.boreal.ui.screens.EndErrandViewModel
import com.zephyr.boreal.ui.screens.ErrandsScreen
import com.zephyr.boreal.ui.screens.LoginScreen
import com.zephyr.boreal.ui.screens.MainScreen
import com.zephyr.boreal.ui.screens.PrintEndErrandScreen
import com.zephyr.boreal.ui.screens.PrintSettingsScreen
import com.zephyr.boreal.ui.screens.ReviewItemsScreen
import com.zephyr.boreal.ui.screens.ReviewItemsViewModel
import com.zephyr.boreal.ui.screens.SearchPartnerNavScreen
import com.zephyr.boreal.ui.screens.SearchPartnerNavViewModel
import com.zephyr.boreal.ui.screens.SelectItemsScreen
import com.zephyr.boreal.ui.screens.SelectItemsViewModel
import com.zephyr.boreal.ui.screens.SelectOtherItemsScreen
import com.zephyr.boreal.ui.screens.SelectOtherItemsViewModel
import com.zephyr.boreal.ui.screens.SelectPartnerScreen
import com.zephyr.boreal.ui.screens.SelectPartnerViewModel
import com.zephyr.boreal.ui.screens.SettingsScreen
import com.zephyr.boreal.ui.screens.StartErrandScreen
import com.zephyr.boreal.ui.screens.StartErrandViewModel
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

@Suppress("LongMethod")
@Composable
fun BorealNavHost(navController: androidx.navigation.NavHostController) {
  NavHost(navController = navController, startDestination = "main") {
    composable("main") {
      MainRoute(navController)
    }
    composable("login") {
      LoginRoute(navController)
    }
    composable("app_locked") {
      AppLockedRoute(navController)
    }
    composable("settings") {
      SettingsRoute(navController)
    }
    composable("print_settings") {
      PrintSettingsRoute(navController)
    }
    composable("change_password") {
      ChangePasswordScreen()
    }
    composable("errands") {
      ErrandsRoute(navController)
    }
    composable("start_errand") {
      StartErrandRoute(navController)
    }
    composable("end_errand") {
      EndErrandRoute(navController)
    }
    composable("print_end_errand") {
      PrintEndErrandRoute(navController)
    }
    composable("select_partner") {
      SelectPartnerRoute(navController)
    }
    composable("search_partner_nav") {
      SearchPartnerNavRoute(navController)
    }
    composable(
      "add_partner?taxNumber={taxNumber}&selectedIndex={selectedIndex}",
      arguments =
        listOf(
          navArgument("taxNumber") {
            type = NavType.StringType
            nullable = true
            defaultValue = null
          },
          navArgument("selectedIndex") {
            type = NavType.IntType
            defaultValue = -1
          },
        ),
    ) {
      AddPartnerRoute(navController)
    }
    composable("select_items") {
      SelectItemsRoute(navController)
    }
    composable("review_items") {
      ReviewItemsRoute(navController)
    }
    composable("select_other_items") {
      SelectOtherItemsRoute(navController)
    }
    composable(
      "discounts/{itemId}/{expirationId}",
      arguments =
        listOf(
          navArgument("itemId") { type = NavType.IntType },
          navArgument("expirationId") { type = NavType.IntType },
        ),
    ) {
      DiscountsRoute(navController)
    }
  }
}

@Composable
private fun MainRoute(navController: androidx.navigation.NavHostController) {
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
    onNavigateToChangePassword = {
      navController.navigate("change_password")
    },
    onNavigateToErrands = {
      navController.navigate("errands")
    },
    onNavigateToSelectPartner = {
      navController.navigate("select_partner")
    },
  )
}

@Composable
private fun LoginRoute(navController: androidx.navigation.NavHostController) {
  LoginScreen(
    onLoginSuccess = {
      navController.navigate("main") {
        popUpTo(0) { inclusive = true }
      }
    },
  )
}

@Composable
private fun AppLockedRoute(navController: androidx.navigation.NavHostController) {
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

@Composable
private fun SettingsRoute(navController: androidx.navigation.NavHostController) {
  SettingsScreen(
    onNavigateToLogin = {
      navController.navigate("login")
    },
    onNavigateToPrintSettings = {
      navController.navigate("print_settings")
    },
    onNavigateToChangePassword = {
      navController.navigate("change_password")
    },
    onLogout = {
      navController.navigate("main") {
        popUpTo(0) { inclusive = true }
      }
    },
  )
}

@Composable
private fun PrintSettingsRoute(navController: androidx.navigation.NavHostController) {
  PrintSettingsScreen(
    onNavigateBack = {
      navController.popBackStack()
    },
  )
}

@Composable
private fun ErrandsRoute(navController: androidx.navigation.NavHostController) {
  ErrandsScreen(
    onNavigateBack = {
      navController.popBackStack()
    },
    onNavigateToStartErrand = {
      navController.navigate("start_errand")
    },
    onNavigateToEndErrand = {
      navController.navigate("end_errand")
    },
  )
}

@Composable
private fun StartErrandRoute(navController: androidx.navigation.NavHostController) {
  val viewModel: StartErrandViewModel = hiltViewModel()
  StartErrandScreen(
    viewModel = viewModel,
    onSuccess = {
      navController.navigate("main") {
        popUpTo(0) { inclusive = true }
      }
    },
  )
}

@Composable
private fun EndErrandRoute(navController: androidx.navigation.NavHostController) {
  val viewModel: EndErrandViewModel = hiltViewModel()
  EndErrandScreen(
    viewModel = viewModel,
    onNavigateBack = {
      navController.popBackStack()
    },
    onSuccess = {
      navController.navigate("print_end_errand") {
        popUpTo("main") { inclusive = false }
      }
    },
  )
}

@Composable
private fun PrintEndErrandRoute(navController: androidx.navigation.NavHostController) {
  PrintEndErrandScreen(
    onNavigateHome = {
      navController.navigate("main") {
        popUpTo(0) { inclusive = true }
      }
    },
  )
}

@Composable
private fun SelectPartnerRoute(navController: androidx.navigation.NavHostController) {
  val viewModel: SelectPartnerViewModel = hiltViewModel()
  SelectPartnerScreen(
    viewModel = viewModel,
    onNavigateNext = {
      navController.navigate("select_items") {
        popUpTo("select_partner") { inclusive = true }
      }
    },
    onNavigateToAddPartner = { isOnline ->
      if (isOnline) {
        navController.navigate("search_partner_nav")
      } else {
        navController.navigate("add_partner")
      }
    },
  )
}

@Composable
private fun SearchPartnerNavRoute(navController: androidx.navigation.NavHostController) {
  val viewModel: SearchPartnerNavViewModel = hiltViewModel()
  SearchPartnerNavScreen(
    viewModel = viewModel,
    onNavigateToAddPartner = { taxNumber, selectedIndex ->
      navController.navigate(
        "add_partner?taxNumber=${Uri.encode(taxNumber)}&selectedIndex=$selectedIndex",
      )
    },
    onNavigateToManualEntry = {
      navController.navigate("add_partner")
    },
  )
}

@Composable
private fun AddPartnerRoute(navController: androidx.navigation.NavHostController) {
  val viewModel: AddPartnerViewModel = hiltViewModel()
  AddPartnerScreen(
    viewModel = viewModel,
    onNavigateToSelectItems = {
      navController.navigate("select_items") {
        popUpTo("main") { inclusive = false }
      }
    },
    onNavigateBack = { navController.popBackStack() },
  )
}

@Composable
private fun ReviewItemsRoute(navController: androidx.navigation.NavHostController) {
  val viewModel: ReviewItemsViewModel = hiltViewModel()
  ReviewItemsScreen(
    viewModel = viewModel,
    onNavigateHome = {
      navController.navigate("main") {
        popUpTo(0) { inclusive = true }
      }
    },
    onNavigateToOtherItems = {
      navController.navigate("select_other_items")
    },
    onNavigateToDiscounts = { itemId, expirationId ->
      navController.navigate("discounts/$itemId/$expirationId")
    },
  )
}

@Composable
private fun DiscountsRoute(navController: androidx.navigation.NavHostController) {
  val viewModel: DiscountsViewModel = hiltViewModel()
  DiscountsScreen(
    viewModel = viewModel,
    onNavigateBack = { navController.popBackStack() },
  )
}

@Composable
private fun SelectItemsRoute(navController: androidx.navigation.NavHostController) {
  val viewModel: SelectItemsViewModel = hiltViewModel()
  SelectItemsScreen(
    viewModel = viewModel,
    onNavigateHome = {
      navController.navigate("main") {
        popUpTo(0) { inclusive = true }
      }
    },
    onNavigateNext = {
      navController.navigate("review_items")
    },
  )
}

@Composable
private fun SelectOtherItemsRoute(navController: androidx.navigation.NavHostController) {
  val viewModel: SelectOtherItemsViewModel = hiltViewModel()
  SelectOtherItemsScreen(
    viewModel = viewModel,
    onNavigateBack = { navController.popBackStack() },
  )
}
