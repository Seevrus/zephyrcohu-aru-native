package com.zephyr.boreal.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zephyr.boreal.R
import com.zephyr.boreal.ui.components.BorealButton
import com.zephyr.boreal.ui.components.BorealDropdown
import com.zephyr.boreal.ui.components.BorealTextInput
import com.zephyr.boreal.ui.components.BorealTopAppBar
import com.zephyr.boreal.ui.components.ButtonVariant
import com.zephyr.boreal.ui.components.DropdownItem
import com.zephyr.boreal.ui.components.ErrorCard
import com.zephyr.boreal.ui.components.InfoCard
import com.zephyr.boreal.ui.components.LoadingIndicator
import com.zephyr.boreal.ui.theme.BorealColors
import com.zephyr.boreal.ui.theme.BorealFontSizes
import com.zephyr.boreal.ui.theme.NunitoSansFamily

private const val LOGIN_BUTTON_WIDTH_FRACTION = 0.6f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
  onLoginSuccess: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: LoginViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsState()

  Scaffold(
    modifier = modifier,
    topBar = {
      BorealTopAppBar(
        title = stringResource(R.string.screen_login_title),
      )
    },
    containerColor = BorealColors.Background,
  ) { padding ->
    LoginScreenContent(
      uiState = uiState,
      onCompanyCodeChange = viewModel::onCompanyCodeChange,
      onUserNameChange = viewModel::onUserNameChange,
      onPasswordChange = viewModel::onPasswordChange,
      onLogin = { viewModel.login(onLoginSuccess) },
      modifier = Modifier.padding(padding),
    )
  }
}

@Composable
fun LoginScreenContent(
  uiState: LoginUiState,
  onCompanyCodeChange: (String) -> Unit,
  onUserNameChange: (String) -> Unit,
  onPasswordChange: (String) -> Unit,
  onLogin: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier.fillMaxSize(),
  ) {
    if (uiState.isLoading) {
      LoadingView()
    } else {
      Column(
        modifier =
          Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        LoginForm(
          uiState = uiState,
          onCompanyCodeChange = onCompanyCodeChange,
          onUserNameChange = onUserNameChange,
          onPasswordChange = onPasswordChange,
          onLogin = onLogin,
        )
      }
    }
  }
}

@Composable
private fun LoadingView() {
  Column(
    modifier = Modifier.fillMaxSize(),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Spacer(modifier = Modifier.height(64.dp))
    LoadingIndicator()
    Spacer(modifier = Modifier.height(16.dp))
    Text(
      text = stringResource(R.string.login_loading_message),
      color = BorealColors.White,
      fontFamily = NunitoSansFamily,
      fontSize = BorealFontSizes.Body,
      fontWeight = FontWeight.Bold,
    )
  }
}

@Composable
private fun LoginForm(
  uiState: LoginUiState,
  onCompanyCodeChange: (String) -> Unit,
  onUserNameChange: (String) -> Unit,
  onPasswordChange: (String) -> Unit,
  onLogin: () -> Unit,
) {
  val welcomeMessage =
    if (uiState.isReLogin) {
      stringResource(R.string.login_relogin_message)
    } else {
      stringResource(R.string.login_welcome_message)
    }

  InfoCard(
    message = welcomeMessage,
    modifier = Modifier.fillMaxWidth(),
  )

  if (uiState.errorMessage != null) {
    ErrorCard(
      message = uiState.errorMessage,
      modifier = Modifier.fillMaxWidth(),
    )
  }

  Spacer(modifier = Modifier.height(16.dp))

  LoginInputFields(
    uiState = uiState,
    onCompanyCodeChange = onCompanyCodeChange,
    onUserNameChange = onUserNameChange,
    onPasswordChange = onPasswordChange,
  )

  Spacer(modifier = Modifier.height(32.dp))

  val isLoginEnabled =
    uiState.isInternetReachable &&
      uiState.userName.trim().isNotEmpty() &&
      uiState.password.trim().isNotEmpty() &&
      uiState.errorMessage == null

  BorealButton(
    text = stringResource(R.string.login_button_label),
    variant = if (isLoginEnabled) ButtonVariant.NEUTRAL else ButtonVariant.DISABLED,
    onClick = onLogin,
    modifier = Modifier.fillMaxWidth(LOGIN_BUTTON_WIDTH_FRACTION),
  )

  Spacer(modifier = Modifier.height(32.dp))
}

@Composable
private fun LoginInputFields(
  uiState: LoginUiState,
  onCompanyCodeChange: (String) -> Unit,
  onUserNameChange: (String) -> Unit,
  onPasswordChange: (String) -> Unit,
) {
  BorealDropdown(
    label = stringResource(R.string.login_company_label),
    data =
      listOf(
        DropdownItem("002", stringResource(R.string.company_akro_bekes)),
        DropdownItem("001", stringResource(R.string.company_hunland_walnut)),
        DropdownItem("003", stringResource(R.string.company_teszt_ceg)),
      ),
    selectedKey = uiState.companyCode,
    onSelect = onCompanyCodeChange,
    modifier = Modifier.fillMaxWidth(),
  )

  Spacer(modifier = Modifier.height(16.dp))

  BorealTextInput(
    label = stringResource(R.string.login_username_label),
    value = uiState.userName,
    onValueChange = onUserNameChange,
    maxLength = 3,
    enabled = uiState.isIdle,
    isError = uiState.errorMessage != null,
    keyboardOptions =
      KeyboardOptions(
        capitalization = KeyboardCapitalization.None,
        autoCorrectEnabled = false,
        keyboardType = KeyboardType.Ascii,
        imeAction = ImeAction.Next,
      ),
    modifier = Modifier.fillMaxWidth(),
  )

  Spacer(modifier = Modifier.height(16.dp))

  BorealTextInput(
    label = stringResource(R.string.login_password_label),
    value = uiState.password,
    onValueChange = onPasswordChange,
    maxLength = 10,
    isError = uiState.errorMessage != null,
    keyboardOptions =
      KeyboardOptions(
        capitalization = KeyboardCapitalization.None,
        autoCorrectEnabled = false,
        keyboardType = KeyboardType.Password,
        imeAction = ImeAction.Done,
      ),
    modifier = Modifier.fillMaxWidth(),
  )
}
