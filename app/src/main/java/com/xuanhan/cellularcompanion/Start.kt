package com.xuanhan.cellularcompanion

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.xuanhan.cellularcompanion.destinations.PermissionsDestination

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Destination(start = true)
fun Start(navigator: DestinationsNavigator) {
    Scaffold {
        Column(
            horizontalAlignment = CenterHorizontally,
            modifier = Modifier.padding(24.dp, 24.dp, 24.dp, 32.dp)
        ) {
            Spacer(modifier = Modifier.weight(1.0f))
            Image(
                painter = painterResource(id = if (isSystemInDarkTheme()) R.drawable.start_vector_night else R.drawable.start_vector_day),
                contentDescription = "Graphic showing device connected to the Internet."
            )
            Spacer(modifier = Modifier.weight(1.0f))
            Text(
                text = "Welcome to Cellular Companion",
                style = MaterialTheme.typography.headlineLarge,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Ensure that you have purchased the Cellular app on the Mac App Store prior to setup.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(48.dp))
            Button(onClick = {
                navigator.navigate(PermissionsDestination())
            }) {
                Text("Get started")
            }
        }
    }
}