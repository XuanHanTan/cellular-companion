package com.xuanhan.cellularcompanion

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.xuanhan.cellularcompanion.viewmodels.ChangeHotspotCredentialsViewModel
import kotlinx.coroutines.launch

@Composable
@OptIn(ExperimentalMaterial3Api::class)
@Destination
fun ChangeHotspotCredentials(navigator: DestinationsNavigator) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val viewModel = ChangeHotspotCredentialsViewModel(context)
    val ssid by viewModel.ssid.collectAsState()
    val password by viewModel.password.collectAsState()
    val prevSSID by viewModel.prevSSID.collectAsState()
    val prevPassword by viewModel.prevPassword.collectAsState()
    var isSSIDValid by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(false) }

    Scaffold(topBar = {
        LargeTopAppBar(
            title = { Text(text = "Change hotspot credentials") },
            navigationIcon = {
                IconButton(onClick = {
                    navigator.popBackStack()
                }) {
                    Icon(
                        painter = painterResource(id = R.drawable.outline_arrow_back_24),
                        contentDescription = "Back button"
                    )
                }
            },
        )
    }, snackbarHost = {
        SnackbarHost(snackbarHostState) {
            Snackbar(it)
        }
    }) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(it)
                .padding(16.dp, 0.dp, 16.dp, 32.dp)
                .fillMaxWidth()
        ) {
            if (isLoading) {
                Spacer(modifier = Modifier.weight(1f))
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Sharing hotspot credentials...")
                Spacer(modifier = Modifier.weight(1f))
            } else {
                Text(
                    "Enter this device's new mobile hotspot credentials in the following fields. They will be stored locally on your Mac to allow your Mac to connect to your hotspot automatically.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(32.dp))
                TextField(
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.outline_wifi_tethering_24),
                            contentDescription = "WiFi icon"
                        )
                    },
                    value = ssid,
                    label = {
                        Text("Name")
                    },
                    supportingText = {
                        if (isSSIDValid) {
                            Text(
                                text = "${ssid.length}/32",
                            )
                        } else {
                            Text(
                                text = "Invalid network name",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    isError = !isSSIDValid,
                    onValueChange = { newValue ->
                        if (newValue.length <= 32) {
                            viewModel.ssid.value = newValue
                        }
                        isSSIDValid = !newValue.contains("\"")
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(16.dp))
                TextField(
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.outline_key_24),
                            contentDescription = "Password icon"
                        )
                    },
                    value = password,
                    label = {
                        Text("Password")
                    },
                    supportingText = {
                        Text(
                            text = "${password.length}/63",
                        )
                    },
                    onValueChange = { newValue ->
                        if (newValue.length <= 63) {
                            viewModel.password.value = newValue
                        }
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.weight(1f))
                Button(onClick = {
                    isLoading = true
                    viewModel.save(
                        context = context,
                        onCompleteCallback = {
                            isLoading = false
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "Hotspot credentials updated successfully.",
                                    duration = SnackbarDuration.Short
                                )
                            }
                        },
                        onDefferedCallback = {
                            isLoading = false
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "Device is disconnected. Hotspot credentials will be updated when device is connected.",
                                    duration = SnackbarDuration.Short
                                )
                            }
                        },
                    )
                }, enabled = ssid.isNotEmpty() && (ssid != prevSSID || password != prevPassword)) {
                    Text("Update")
                }
            }
        }
    }
}