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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.xuanhan.cellularcompanion.viewmodels.SettingUpViewModel

@Composable
@OptIn(ExperimentalMaterial3Api::class)
@Destination
fun SettingUp(navigator: DestinationsNavigator, serviceUUID: String, sharedPIN: String) {
    val currentContext = LocalContext.current
    val viewModel = remember { SettingUpViewModel(currentContext.applicationContext) }

    LaunchedEffect(keys = arrayOf(serviceUUID, sharedPIN)) {
        viewModel.setupBluetooth(serviceUUID, sharedPIN)
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text(text = "Setting things up") },
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
            Text(viewModel.loadingMessage.value)
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}
