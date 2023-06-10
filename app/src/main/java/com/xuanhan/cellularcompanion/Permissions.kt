package com.xuanhan.cellularcompanion

import android.Manifest
import android.os.Build
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

enum class PermissionStatus {
    Granted,
    Denied,
    PermanentlyDenied,
    NotRequested
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Destination
fun Permissions(navigator: DestinationsNavigator) {
    class PermissionDescription(val title: String, val description: String?) {
        var status = mutableStateOf(PermissionStatus.NotRequested)
    }

    val permissions = HashMap<String, PermissionDescription>().apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            put(
                Manifest.permission.BLUETOOTH_SCAN, remember {
                    PermissionDescription(
                        "Scanning for nearby Bluetooth devices",
                        null
                    )
                }
            )
            put(
                Manifest.permission.BLUETOOTH_CONNECT, remember {
                    PermissionDescription(
                        "Connecting to saved Bluetooth device",
                        null
                    )
                }
            )
        }
        put(
            Manifest.permission.ACCESS_FINE_LOCATION, remember {
                PermissionDescription(
                    "Precise location access",
                    "Required to scan for nearby Bluetooth devices."
                )
            }
        )
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
                .padding(16.dp, 0.dp, 16.dp, 32.dp)
                .scrollable(scrollState, orientation = Orientation.Horizontal)
        ) {
            permissions.forEach { (permission, permissionDescription) ->
                ListItem(
                    leadingContent = {

                    },
                    headlineText = { Text(permissionDescription.title) },
                    supportingText = {
                        if (permissionDescription.description != null) Text(
                            text = permissionDescription.description
                        )
                    },
                )
            }
        }
    }
}