package com.xuanhan.cellularcompanion

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.xuanhan.cellularcompanion.destinations.HotspotInfoDestination
import com.xuanhan.cellularcompanion.destinations.SettingUp2Destination

@Composable
@OptIn(ExperimentalMaterial3Api::class)
@Destination
fun HotspotInfo(navigator: DestinationsNavigator) {
    var ssid by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // TODO: ensure that wifi ssid/password is in ASCII and does not contain "

    Scaffold(topBar = {
        LargeTopAppBar(
            title = { Text(text = "Hotspot credentials") },
        )
    }) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(it)
                .padding(16.dp, 0.dp, 16.dp, 32.dp)
        ) {
            Text(
                "Enter this device's mobile hotspot credentials in the following fields. They will be sent to your Mac to allow your Mac to connect to your hotspot automatically. This may take a few tries.",
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
                    Text("SSID")
                },
                supportingText = {
                    Text(
                        text = "${ssid.length}/32",
                    )
                },
                onValueChange = { newValue ->
                    if (newValue.length <= 32) {
                        ssid = newValue
                    }
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
                        password = newValue
                    }
                },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.weight(1f))
            Button(onClick = {
                navigator.navigate(SettingUp2Destination(ssid, password)) {
                    popUpTo(HotspotInfoDestination.route) { inclusive = true }
                }
            }, enabled = ssid.isNotEmpty()) {
                Text("Next")
            }
        }
    }
}