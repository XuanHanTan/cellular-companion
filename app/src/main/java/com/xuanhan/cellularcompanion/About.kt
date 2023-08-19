package com.xuanhan.cellularcompanion

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Destination
fun About(navigator: DestinationsNavigator) {
    val context = LocalContext.current
    val websiteIntent = remember { Intent(Intent.ACTION_VIEW, Uri.parse("https://www.xuanhan.me")) }
    val emailIntent = remember { Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:contact@xuanhan.me")) }

    Scaffold(topBar = {
        LargeTopAppBar(
            title = { Text(text = "About") },
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
    }) {
        Column(
            horizontalAlignment = CenterHorizontally,
            modifier = Modifier
                .padding(it)
                .padding(0.dp, 0.dp, 0.dp, 32.dp)
                .fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = CenterHorizontally,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "v1.0.1",
                    style = MaterialTheme.typography.displayLarge,
                )
                Spacer(modifier = Modifier.height(48.dp))
                Text("© Xuan Han Tan 2023. All rights reserved.\n\nThis app was developed by Xuan Han Tan. I hope you find it useful! If you want to learn more about me and my other projects, visit my website. If you have any questions, feel free to contact me by email.")
                Spacer(modifier = Modifier.height(16.dp))
            }
            ListItem(
                leadingContent = {
                    Icon(
                        painter = painterResource(id = R.drawable.outline_language_24),
                        contentDescription = "Website icon",
                    )
                },
                headlineText = {
                    Text(
                        "Visit my website",
                    )
                },
                modifier = Modifier.clickable {
                    try {
                        context.startActivity(websiteIntent)
                    } catch (e: ActivityNotFoundException) {
                        println("Failed to start website activity: $e")
                    }
                }
            )
            ListItem(
                leadingContent = {
                    Icon(
                        painter = painterResource(id = R.drawable.outline_email_24),
                        contentDescription = "Email icon",
                    )
                },
                headlineText = {
                    Text(
                        "Email me",
                    )
                },
                modifier = Modifier.clickable {
                    try {
                        context.startActivity(emailIntent)
                    } catch (e: ActivityNotFoundException) {
                        println("Failed to start email activity: $e")
                    }
                }
            )
        }
    }
}