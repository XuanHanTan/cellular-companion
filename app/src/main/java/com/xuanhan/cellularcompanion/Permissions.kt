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
import com.xuanhan.cellularcompanion.destinations.QRCodeDestination
import com.xuanhan.cellularcompanion.viewmodels.PermissionViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
@Destination
fun Permissions(navigator: DestinationsNavigator) {
    val context = LocalContext.current
    val btConnectPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        rememberPermissionState(permission = Manifest.permission.BLUETOOTH_CONNECT)
    } else {
        null
    }
    val permissions = ArrayList<PermissionViewModel>().apply {
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
        }
        add(
            PermissionViewModel(
                "Camera access",
                "Required to scan the QR code shown on your Mac.",
                rememberPermissionState(permission = Manifest.permission.CAMERA)
            )
        )
    }
    val scrollState = rememberScrollState()
    val enableBt = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = {
            if (it.resultCode == RESULT_OK) {
                requiresBtPermissionCheck = true
                navigator.navigate(QRCodeDestination)
            }
        })

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text(text = "Permissions") },
                navigationIcon = {
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
                        when (permissionDescription.status.status) {
                            PermissionStatus.Granted -> {
                                Icon(
                                    painter = painterResource(id = R.drawable.outline_done_24),
                                    contentDescription = "Permission granted",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            is PermissionStatus.Denied -> {
                                if (permissionDescription.status.status.shouldShowRationale) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.outline_close_24),
                                        contentDescription = "Permission denied",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                } else {
                                    Icon(
                                        painter = painterResource(id = R.drawable.outline_done_24),
                                        contentDescription = "Permission granted",
                                        tint = MaterialTheme.colorScheme.outline
                                    )
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
                        permissionDescription.grantPermission()
                    }
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            if (permissions.all { permissionDescription -> permissionDescription.status.status == PermissionStatus.Granted }) {
                Button(onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        btConnectPermission!!.launchPermissionRequest()
                    }

                    val bluetoothManager =
                        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                    val bluetoothAdapter = bluetoothManager.adapter
                    if (!bluetoothAdapter.isEnabled) {
                        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                        enableBt.launch(enableBtIntent)
                    } else {
                        navigator.navigate(QRCodeDestination())
                        requiresBtPermissionCheck = true
                    }
                }) {
                    Text("Next")
                }
            }
        }
    }
}