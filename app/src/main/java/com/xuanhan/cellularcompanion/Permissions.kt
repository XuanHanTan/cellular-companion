package com.xuanhan.cellularcompanion

import android.Manifest
import android.app.Activity.RESULT_OK
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.xuanhan.cellularcompanion.destinations.HomePageDestination
import com.xuanhan.cellularcompanion.destinations.PermissionsDestination
import com.xuanhan.cellularcompanion.destinations.QRCodeDestination
import com.xuanhan.cellularcompanion.viewmodels.PermissionViewModel
import kotlinx.coroutines.flow.update

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
@Destination
fun Permissions(navigator: DestinationsNavigator) {
    val context = LocalContext.current
    val enableBt = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = {
            if (it.resultCode == RESULT_OK) {
                if (isSetupComplete) {
                    navigator.navigate(HomePageDestination) {
                        popUpTo(PermissionsDestination.route) { inclusive = true }
                    }
                } else {
                    navigator.navigate(QRCodeDestination())
                }
                requiresBtPermissionCheck.value = true
            }
        })

    fun handleNextButton() {
        val bluetoothManager =
            context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter
        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBt.launch(enableBtIntent)
        } else {
            if (isSetupComplete) {
                navigator.navigate(HomePageDestination) {
                    popUpTo(PermissionsDestination.route) { inclusive = true }
                }
            } else {
                navigator.navigate(QRCodeDestination())
            }
            requiresBtPermissionCheck.value = true
        }
    }

    val permissions = ArrayList<PermissionViewModel>()
    val writeSystemSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = {
            if (PermissionViewModel.getWriteSettingsGranted(context)) {
                permissions.find { it.status.permission == Manifest.permission.WRITE_SETTINGS }
                    ?.let {
                        it.isSpecialPermissionGranted.update { true }
                    }
            }

            if (PermissionViewModel.getIgnoreBatteryOptimizationsGranted(context)) {
                permissions.find { it.status.permission == Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS }
                    ?.let {
                        it.isSpecialPermissionGranted.update { true }
                    }
            }
        })
    val btConnectPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = {
            if (it) {
                handleNextButton()
            }
        })
    permissions.apply {
        add(
            PermissionViewModel(
                "Writing system settings",
                "Required to enable and disable hotspot functionality automatically.",
                rememberPermissionState(permission = Manifest.permission.WRITE_SETTINGS),
                isSpecialPermission = true,
                specialPermissionLauncher = writeSystemSettingsLauncher,
                context = context
            )
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add(
                PermissionViewModel(
                    "Scanning for and connecting to nearby Bluetooth devices",
                    null,
                    status = rememberPermissionState(permission = Manifest.permission.BLUETOOTH_SCAN),
                )
            )
        } else {
            add(
                PermissionViewModel(
                    "Precise location access",
                    "Required to scan for nearby Bluetooth devices.",
                    rememberPermissionState(permission = Manifest.permission.ACCESS_FINE_LOCATION)
                )
            )

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                add(
                    PermissionViewModel(
                        "Managing phone state",
                        "Required to get information on your phone's cellular connection.",
                        rememberPermissionState(permission = Manifest.permission.READ_PHONE_STATE)
                    )
                )
            }
        }
        add(
            PermissionViewModel(
                "Camera access",
                "Required to scan the QR code shown on your Mac.",
                rememberPermissionState(permission = Manifest.permission.CAMERA),
                isOptionalPermission = isSetupComplete
            )
        )
        add(
            PermissionViewModel(
                "Ignore battery optimisations",
                "Recommended to keep this app running in the background.",
                rememberPermissionState(permission = Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS),
                isOptionalPermission = true,
                isSpecialPermission = true,
                specialPermissionLauncher = writeSystemSettingsLauncher,
                context = context,
            )
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(
                PermissionViewModel(
                    "Notifications",
                    "Allows notifications to show when your phone's hotspot is enabled or disabled.",
                    rememberPermissionState(permission = Manifest.permission.POST_NOTIFICATIONS),
                    isOptionalPermission = true
                )
            )
        }
    }
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text(text = "Permissions") },
                navigationIcon = {
                    if (!isSetupComplete) {
                        IconButton(
                            onClick = {
                                navigator.popBackStack()
                            }
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.outline_arrow_back_24),
                                contentDescription = "Back button"
                            )
                        }
                    }
                },
            )
        }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(it)
                .padding(0.dp, 0.dp, 0.dp, 32.dp)
                .scrollable(scrollState, orientation = Orientation.Horizontal)
        ) {
            Text(
                "To allow Cellular Companion to communicate with your Mac, you’ll have to grant some permissions. Tap on each permission to grant it to this app.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            permissions.forEach { permissionDescription ->
                ListItem(
                    leadingContent = {
                        val grantedIcon = @Composable {
                            Icon(
                                painter = painterResource(id = R.drawable.outline_done_24),
                                contentDescription = "Permission granted",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        val deniedIcon = @Composable {
                            Icon(
                                painter = painterResource(id = R.drawable.outline_close_24),
                                contentDescription = "Permission denied",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                        val notGrantedIcon = @Composable {
                            Icon(
                                painter = painterResource(id = R.drawable.outline_done_24),
                                contentDescription = "Permission not granted",
                                tint = MaterialTheme.colorScheme.outline
                            )
                        }

                        if (permissionDescription.isSpecialPermission) {
                            val isSpecialPermissionGranted: Boolean by permissionDescription.isSpecialPermissionGranted.collectAsState()

                            if (isSpecialPermissionGranted) {
                                grantedIcon()
                            } else {
                                notGrantedIcon()
                            }
                        } else {
                            when (permissionDescription.status.status) {
                                PermissionStatus.Granted -> {
                                    grantedIcon()
                                }

                                is PermissionStatus.Denied -> {
                                    if (permissionDescription.status.status.shouldShowRationale) {
                                        deniedIcon()
                                    } else {
                                        notGrantedIcon()
                                    }
                                }
                            }
                        }
                    },
                    headlineText = { Text(permissionDescription.title) },
                    supportingText = if (permissionDescription.description == null) null else {
                        {
                            Text(permissionDescription.description)
                        }
                    },
                    modifier = Modifier.clickable {
                        permissionDescription.grantPermission(context)
                    }
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Button(onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    btConnectPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                } else {
                    handleNextButton()
                }
            }, enabled = permissions.all { permissionDescription ->
                val isSpecialPermissionGranted: Boolean by permissionDescription.isSpecialPermissionGranted.collectAsState()
                permissionDescription.status.status == PermissionStatus.Granted || permissionDescription.isOptionalPermission || (permissionDescription.isSpecialPermission && isSpecialPermissionGranted)
            }) {
                Text("Next")
            }
        }
    }
}