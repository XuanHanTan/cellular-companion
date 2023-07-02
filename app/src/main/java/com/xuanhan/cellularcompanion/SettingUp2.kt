package com.xuanhan.cellularcompanion

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.xuanhan.cellularcompanion.destinations.StartDestination
import com.xuanhan.cellularcompanion.viewmodels.SettingUpViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
@OptIn(ExperimentalMaterial3Api::class)
@Destination
fun SettingUp2(navigator: DestinationsNavigator, ssid: String, password: String) {
    val currentContext = LocalContext.current
    val viewModel = remember { SettingUpViewModel(currentContext) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(keys = arrayOf(ssid, password)) {
        viewModel.shareHotspotDetails(ssid, password) {
            println("Hotspot credentials shared successfully!")
            coroutineScope.launch(context = Dispatchers.IO) {
                viewModel.completeSetup()
                coroutineScope.launch(context = Dispatchers.Main.immediate) {
                    navigator.navigate(StartDestination)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text(text = "Finishing up") },
            )
        }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(it)
                .padding(0.dp, 0.dp, 0.dp, 32.dp)
                .fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.weight(1f))
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text("Sharing hotspot credentials...")
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}
