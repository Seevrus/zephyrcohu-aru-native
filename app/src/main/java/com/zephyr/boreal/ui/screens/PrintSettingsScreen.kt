package com.zephyr.boreal.ui.screens

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zephyr.boreal.R
import com.zephyr.boreal.ui.components.BorealButton
import com.zephyr.boreal.ui.components.BorealDropdown
import com.zephyr.boreal.ui.components.BorealTopAppBar
import com.zephyr.boreal.ui.components.ButtonVariant
import com.zephyr.boreal.ui.components.DropdownItem
import com.zephyr.boreal.ui.components.ErrorCard
import com.zephyr.boreal.ui.theme.BorealColors

@Composable
fun PrintSettingsScreen(
  onNavigateBack: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: PrintSettingsViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsState()
  val context = LocalContext.current
  val permissions = getBluetoothPermissions()

  val permissionLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { perms ->
      val allGranted = perms.entries.all { it.value }
      viewModel.updatePermissionsState(hasPermissions = allGranted, canAskPermissions = !allGranted)
      viewModel.finishInitialCheck()
    }

  val bluetoothEnableLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.StartActivityForResult(),
    ) { _ -> }

  LaunchedEffect(Unit) {
    val allGranted =
      permissions.all {
        androidx.core.content.ContextCompat
          .checkSelfPermission(context, it) ==
          android.content.pm.PackageManager.PERMISSION_GRANTED
      }
    if (!allGranted) {
      permissionLauncher.launch(permissions)
    } else {
      viewModel.updatePermissionsState(hasPermissions = true, canAskPermissions = false)
    }
  }

  PrintSettingsScreenContent(
    uiState = uiState,
    onOpenSettings = {
      val intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
          data = Uri.fromParts("package", context.packageName, null)
        }
      context.startActivity(intent)
    },
    onEnableBluetooth = {
      bluetoothEnableLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
    },
    onPrinterSelected = viewModel::onPrinterSelected,
    onPrintFullStorageListChanged = viewModel::onPrintFullStorageListChanged,
    onSave = {
      viewModel.saveSettings()
      onNavigateBack()
    },
    modifier = modifier,
  )
}

private fun getBluetoothPermissions(): Array<String> =
  if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
    arrayOf(android.Manifest.permission.BLUETOOTH_CONNECT)
  } else {
    arrayOf(
      android.Manifest.permission.BLUETOOTH,
      android.Manifest.permission.ACCESS_FINE_LOCATION,
    )
  }

@Suppress("LongMethod")
@Composable
fun PrintSettingsScreenContent(
  uiState: PrintSettingsUiState,
  onOpenSettings: () -> Unit,
  onEnableBluetooth: () -> Unit,
  onPrinterSelected: (String) -> Unit,
  onPrintFullStorageListChanged: (Boolean) -> Unit,
  onSave: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Scaffold(
    modifier = modifier,
    topBar = { BorealTopAppBar(title = stringResource(R.string.screen_print_title)) },
    containerColor = BorealColors.Background,
  ) { padding ->
    Column(
      modifier =
        Modifier
          .padding(padding)
          .fillMaxSize()
          .padding(16.dp)
          .verticalScroll(rememberScrollState()),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      when {
        uiState.isInitialCheck -> {}
        !uiState.hasPermissions -> PermissionErrorView(onOpenSettings)
        !uiState.isBluetoothEnabled -> BluetoothErrorView(onEnableBluetooth)
        else ->
          SettingsFormView(
            uiState = uiState,
            onPrinterSelected = onPrinterSelected,
            onPrintFullStorageListChanged = onPrintFullStorageListChanged,
            onSave = onSave,
          )
      }
    }
  }
}

@Composable
private fun PermissionErrorView(onOpenSettings: () -> Unit) {
  ErrorCard(
    message = stringResource(R.string.print_settings_permission_error),
    modifier = Modifier.fillMaxWidth(),
  )
  Spacer(modifier = Modifier.height(16.dp))
  BorealButton(
    text = stringResource(R.string.print_settings_open_settings),
    variant = ButtonVariant.NEUTRAL,
    onClick = onOpenSettings,
  )
}

@Composable
private fun BluetoothErrorView(onEnableBluetooth: () -> Unit) {
  ErrorCard(
    message = stringResource(R.string.print_settings_bluetooth_off_error),
    modifier = Modifier.fillMaxWidth(),
  )
  Spacer(modifier = Modifier.height(16.dp))
  BorealButton(
    text = stringResource(R.string.print_settings_enable_bluetooth),
    variant = ButtonVariant.NEUTRAL,
    onClick = onEnableBluetooth,
  )
}

@Composable
private fun SettingsFormView(
  uiState: PrintSettingsUiState,
  onPrinterSelected: (String) -> Unit,
  onPrintFullStorageListChanged: (Boolean) -> Unit,
  onSave: () -> Unit,
) {
  if (uiState.pairedDevices.isEmpty()) {
    ErrorCard(
      message = stringResource(R.string.print_settings_no_paired_devices),
      modifier = Modifier.fillMaxWidth(),
    )
    Spacer(modifier = Modifier.height(16.dp))
  }

  val deviceItems =
    remember(uiState.pairedDevices) {
      uiState.pairedDevices.map { DropdownItem(key = it.address, value = it.name) }
    }

  BorealDropdown(
    label = stringResource(R.string.print_settings_printer_label),
    data = deviceItems,
    selectedKey = uiState.selectedPrinterAddress,
    onSelect = onPrinterSelected,
    modifier = Modifier.fillMaxWidth(),
  )

  Spacer(modifier = Modifier.height(16.dp))

  BorealDropdown(
    label = stringResource(R.string.print_settings_mode_label),
    data =
      listOf(
        DropdownItem(key = "NORMAL", value = stringResource(R.string.print_settings_mode_changes)),
        DropdownItem(key = "FULL", value = stringResource(R.string.print_settings_mode_full)),
      ),
    selectedKey = if (uiState.printFullStorageList) "FULL" else "NORMAL",
    onSelect = { onPrintFullStorageListChanged(it == "FULL") },
    modifier = Modifier.fillMaxWidth(),
  )

  Spacer(modifier = Modifier.height(32.dp))

  Box(modifier = Modifier.fillMaxWidth()) {
    BorealButton(
      text = stringResource(R.string.print_settings_save),
      variant = if (uiState.selectedPrinterAddress == null) ButtonVariant.DISABLED else ButtonVariant.NEUTRAL,
      onClick = onSave,
      modifier = Modifier.align(Alignment.Center),
    )
  }
}
