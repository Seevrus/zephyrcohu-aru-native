package com.zephyr.boreal

import androidx.compose.material3.Surface
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.zephyr.boreal.ui.screens.AppLockedScreenContent
import com.zephyr.boreal.ui.screens.MainScreenContent
import com.zephyr.boreal.ui.theme.BorealTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class NavigationTest {
  @get:Rule
  val composeTestRule = createComposeRule()

  private lateinit var navController: NavController

  @Test
  fun navigationReplacement_fromAppLockedToMain_clearsBackstack() {
    composeTestRule.setContent {
      BorealTheme {
        Surface {
          navController = rememberNavController()
          NavHost(
            navController = navController as androidx.navigation.NavHostController,
            startDestination = "app_locked",
          ) {
            composable("app_locked") {
              AppLockedScreenContent(
                userName = "Test",
                canUseApp = true, // Should trigger navigation immediately
                onNavigateToMain = {
                  navController.navigate("main") {
                    popUpTo(0) { inclusive = true }
                  }
                },
              )
            }
            composable("main") {
              // Main Screen Mock
            }
          }
        }
      }
    }

    composeTestRule.waitForIdle()

    // Assert that we are on "main" and backstack is empty (current route is the only one)
    composeTestRule.runOnIdle {
      assertEquals("main", navController.currentDestination?.route)
      assert(navController.previousBackStackEntry == null)
    }
  }

  @Test
  fun navigationReplacement_fromMainToAppLocked_clearsBackstack() {
    composeTestRule.setContent {
      BorealTheme {
        Surface {
          navController = rememberNavController()
          NavHost(navController = navController as androidx.navigation.NavHostController, startDestination = "main") {
            composable("main") {
              MainScreenContent(
                isReady = true,
                isLoggedIn = true,
                canUseApp = false, // Should trigger navigation immediately
                onNavigateToAppLocked = {
                  navController.navigate("app_locked") {
                    popUpTo(0) { inclusive = true }
                  }
                },
              )
            }
            composable("app_locked") {
              // App Locked Mock
            }
          }
        }
      }
    }

    composeTestRule.waitForIdle()

    // Assert that we are on "app_locked" and backstack is empty
    composeTestRule.runOnIdle {
      assertEquals("app_locked", navController.currentDestination?.route)
      assert(navController.previousBackStackEntry == null)
    }
  }
}
