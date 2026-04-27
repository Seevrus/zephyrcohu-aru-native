package com.zephyr.boreal

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Surface
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.platform.app.InstrumentationRegistry
import com.zephyr.boreal.ui.screens.AppLockedScreenContent
import com.zephyr.boreal.ui.screens.LoginScreenContent
import com.zephyr.boreal.ui.screens.LoginUiState
import com.zephyr.boreal.ui.screens.MainScreenContent
import com.zephyr.boreal.ui.screens.MainScreenUiState
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
                onNavigateToSettings = {},
              )
            }
            composable("main") {
              Box {}
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
                uiState =
                  MainScreenUiState(
                    isReady = true,
                    isLoggedIn = true,
                    canUseApp = false, // Should trigger navigation immediately
                    isInternetReachable = true,
                    isPasswordExpired = false,
                  ),
                onNavigateToAppLocked = {
                  navController.navigate("app_locked") {
                    popUpTo(0) { inclusive = true }
                  }
                },
                onNavigateToSettings = {},
                onNavigateToLogin = {},
                onNavigateToChangePassword = {},
                onTileClick = {},
                onDismissAlert = {},
              )
            }
            composable("app_locked") {
              Box {}
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

  @Test
  fun navigation_fromMainToLogin() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val loginPrompt = context.getString(R.string.main_login_prompt)

    composeTestRule.setContent {
      BorealTheme {
        Surface {
          navController = rememberNavController()
          NavHost(navController = navController as androidx.navigation.NavHostController, startDestination = "main") {
            composable("main") {
              MainScreenContent(
                uiState =
                  MainScreenUiState(
                    isReady = true,
                    isLoggedIn = false,
                    canUseApp = null,
                    isInternetReachable = true,
                    isPasswordExpired = false,
                  ),
                onNavigateToAppLocked = {},
                onNavigateToSettings = {},
                onNavigateToLogin = {
                  navController.navigate("login")
                },
                onNavigateToChangePassword = {},
                onTileClick = {},
                onDismissAlert = {},
              )
            }
            composable("login") {
              Box {}
            }
          }
        }
      }
    }

    composeTestRule.onNodeWithText(loginPrompt).performClick()

    composeTestRule.runOnIdle {
      assertEquals("login", navController.currentDestination?.route)
    }
  }

  @Test
  fun navigationReplacement_fromLoginToMain_clearsBackstack() {
    composeTestRule.setContent {
      BorealTheme {
        Surface {
          navController = rememberNavController()
          NavHost(navController = navController as androidx.navigation.NavHostController, startDestination = "login") {
            composable("login") {
              LoginScreenContent(
                uiState = LoginUiState(isInternetReachable = true, userName = "abc", password = "123"),
                onCompanyCodeChange = {},
                onUserNameChange = {},
                onPasswordChange = {},
                onLogin = {
                  navController.navigate("main") {
                    popUpTo(0) { inclusive = true }
                  }
                },
              )
            }
            composable("main") {
              Box {}
            }
          }
        }
      }
    }

    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val loginButtonLabel = context.getString(R.string.login_button_label)

    composeTestRule.onNodeWithText(loginButtonLabel).performClick()

    composeTestRule.runOnIdle {
      assertEquals("main", navController.currentDestination?.route)
      assert(navController.previousBackStackEntry == null)
    }
  }

  @Test
  fun navigation_fromMainToSettings() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val settingsDescription = context.getString(R.string.settings_icon_description)

    composeTestRule.setContent {
      BorealTheme {
        Surface {
          navController = rememberNavController()
          NavHost(navController = navController as androidx.navigation.NavHostController, startDestination = "main") {
            composable("main") {
              MainScreenContent(
                uiState =
                  MainScreenUiState(
                    isReady = true,
                    isLoggedIn = true,
                    canUseApp = true,
                    isInternetReachable = true,
                    isPasswordExpired = false,
                  ),
                onNavigateToAppLocked = {},
                onNavigateToSettings = {
                  navController.navigate("settings")
                },
                onNavigateToLogin = {},
                onNavigateToChangePassword = {},
                onTileClick = {},
                onDismissAlert = {},
              )
            }
            composable("settings") {
              Box {}
            }
          }
        }
      }
    }

    composeTestRule.onNodeWithContentDescription(settingsDescription).performClick()

    composeTestRule.runOnIdle {
      assertEquals("settings", navController.currentDestination?.route)
    }
  }

  @Test
  fun navigation_fromAppLockedToSettings() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val settingsDescription = context.getString(R.string.settings_icon_description)

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
                canUseApp = false,
                onNavigateToMain = {},
                onNavigateToSettings = {
                  navController.navigate("settings")
                },
              )
            }
            composable("settings") {
              Box {}
            }
          }
        }
      }
    }

    composeTestRule.onNodeWithContentDescription(settingsDescription).performClick()

    composeTestRule.runOnIdle {
      assertEquals("settings", navController.currentDestination?.route)
    }
  }

  @Test
  fun navigation_fromSettingsToLogin() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val loginTileTitle = context.getString(R.string.tile_login)

    composeTestRule.setContent {
      BorealTheme {
        Surface {
          navController = rememberNavController()
          NavHost(
            navController = navController as androidx.navigation.NavHostController,
            startDestination = "settings",
          ) {
            composable("settings") {
              com.zephyr.boreal.ui.screens.SettingsScreenContent(
                isLoggedIn = false,
                isIdle = false,
                isLoading = false,
                isInternetReachable = true,
                onNavigateToLogin = {
                  navController.navigate("login")
                },
              )
            }
            composable("login") {
              Box {}
            }
          }
        }
      }
    }

    composeTestRule.onNodeWithText(loginTileTitle).performClick()

    composeTestRule.runOnIdle {
      assertEquals("login", navController.currentDestination?.route)
    }
  }
}
