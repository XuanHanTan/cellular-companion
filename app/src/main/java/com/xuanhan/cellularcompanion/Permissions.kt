package com.xuanhan.cellularcompanion

import android.Manifest
import android.os.Build
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.xuanhan.cellularcompanion.destinations.PermissionsDestination
import com.xuanhan.cellularcompanion.models.PermissionViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
@Destination
fun Permissions(navigator: DestinationsNavigator) {
    val permissions = ArrayList<PermissionViewModel>().apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add(
                PermissionViewModel(
                    "Scanning for and connecting to nearby Bluetooth devices",
                    null,
                    status = rememberPermissionState(permission = Manifest.permission.BLUETOOTH_SCAN),
                    altStatus = rememberPermissionState(permission = Manifest.permission.BLUETOOTH_CONNECT)
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
    }
    val scrollState = rememberScrollState()

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
                                    tint = Color(0xFF1C850B)
                                )
                            }

                            is PermissionStatus.Denied -> {
                                if (permissionDescription.status.status.shouldShowRationale) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.outline_close_24),
                                        contentDescription = "Permission denied",
                                        tint = Color(0xFFA91C1C)
                                    )
                                } else {
                                    Icon(
                                        painter = painterResource(id = R.drawable.outline_done_24),
                                        contentDescription = "Permission granted",
                                        tint = Color(0xFF848484)
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
                    navigator.navigate(PermissionsDestination())
                }) {
                    Text("Next")
                }
            }
        }
    }
}