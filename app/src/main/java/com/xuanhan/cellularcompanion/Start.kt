package com.xuanhan.cellularcompanion

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.xuanhan.cellularcompanion.destinations.HomePageDestination
import com.xuanhan.cellularcompanion.destinations.IntroDestination
import com.xuanhan.cellularcompanion.destinations.PermissionsDestination
import com.xuanhan.cellularcompanion.destinations.StartDestination
import com.xuanhan.cellularcompanion.viewmodels.StartViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Destination
@RootNavGraph(start = true)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Start(navigator: DestinationsNavigator) {
    val viewModel = StartViewModel()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(key1 = null) {
        if (isSetupComplete) {
            coroutineScope.launch(context = Dispatchers.Main.immediate) {
                if (viewModel.checkPermissions(context)) {
                    requiresBtPermissionCheck.value = true
                    navigator.navigate(HomePageDestination) {
                        popUpTo(StartDestination.route) { inclusive = true }
                    }
                } else {
                    navigator.navigate(PermissionsDestination) {
                        popUpTo(StartDestination.route) { inclusive = true }
                    }
                }
            }
        } else {
            coroutineScope.launch(context = Dispatchers.Main.immediate) {
                navigator.navigate(IntroDestination) {
                    popUpTo(StartDestination.route) { inclusive = true }
                }
            }
        }
    }

    Scaffold {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(it).fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.weight(1f))
            CircularProgressIndicator()
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}