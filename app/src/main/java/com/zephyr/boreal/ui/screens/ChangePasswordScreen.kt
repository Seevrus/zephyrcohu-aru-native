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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zephyr.boreal.R
import com.zephyr.boreal.ui.components.BorealButton
import com.zephyr.boreal.ui.components.BorealTextInput
import com.zephyr.boreal.ui.components.BorealTopAppBar
import com.zephyr.boreal.ui.components.ButtonVariant
import com.zephyr.boreal.ui.components.ErrorCard
import com.zephyr.boreal.ui.components.InfoCard
import com.zephyr.boreal.ui.components.LoadingIndicator
import com.zephyr.boreal.ui.components.SuccessCard
import com.zephyr.boreal.ui.theme.BorealColors
import com.zephyr.boreal.ui.theme.BorealFontSizes
import com.zephyr.boreal.ui.theme.NunitoSansFamily

private const val SUBMIT_BUTTON_WIDTH_FRACTION = 0.8f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(
  modifier: Modifier = Modifier,
  viewModel: ChangePasswordViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsState()

  Scaffold(
    modifier = modifier,
    topBar = {
      BorealTopAppBar(
        title = stringResource(R.string.change_password_title),
      )
    },
    containerColor = BorealColors.Background,
  ) { padding ->
    ChangePasswordContent(
      uiState = uiState,
      onPasswordChange = viewModel::onPasswordChange,
      onSubmit = viewModel::changePassword,
      modifier = Modifier.padding(padding),
    )
  }
}

@Composable
fun ChangePasswordContent(
  uiState: ChangePasswordUiState,
  onPasswordChange: (String) -> Unit,
  onSubmit: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(modifier = modifier.fillMaxSize()) {
    if (uiState.isLoading) {
      ChangePasswordLoadingView()
    } else {
      Column(
        modifier =
          Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Spacer(modifier = Modifier.height(16.dp))

        InfoCard(
          message = stringResource(R.string.change_password_rules),
          modifier = Modifier.fillMaxWidth(),
        )

        if (uiState.errorMessage != null) {
          ErrorCard(
            message = uiState.errorMessage,
            modifier = Modifier.fillMaxWidth(),
          )
        }

        if (uiState.successMessage != null) {
          SuccessCard(
            message = uiState.successMessage,
            modifier = Modifier.fillMaxWidth(),
          )
        }

        Spacer(modifier = Modifier.height(16.dp))

        BorealTextInput(
          label = stringResource(R.string.login_password_label),
          value = uiState.password,
          onValueChange = onPasswordChange,
          maxLength = 10,
          isError = uiState.errorMessage != null,
          keyboardOptions =
            KeyboardOptions(
              autoCorrectEnabled = false,
              keyboardType = KeyboardType.Password,
              imeAction = ImeAction.Done,
            ),
          modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(32.dp))

        val isSubmitEnabled = uiState.password.isNotEmpty() && uiState.successMessage == null

        BorealButton(
          text = stringResource(R.string.change_password_button),
          variant = if (isSubmitEnabled) ButtonVariant.NEUTRAL else ButtonVariant.DISABLED,
          onClick = onSubmit,
          modifier = Modifier.fillMaxWidth(SUBMIT_BUTTON_WIDTH_FRACTION),
        )

        Spacer(modifier = Modifier.height(32.dp))
      }
    }
  }
}

@Composable
private fun ChangePasswordLoadingView() {
  Column(
    modifier = Modifier.fillMaxSize(),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Spacer(modifier = Modifier.height(64.dp))
    LoadingIndicator()
    Spacer(modifier = Modifier.height(16.dp))
    Text(
      text = stringResource(R.string.change_password_loading),
      color = BorealColors.White,
      fontFamily = NunitoSansFamily,
      fontSize = BorealFontSizes.Body,
      fontWeight = FontWeight.Bold,
    )
  }
}
