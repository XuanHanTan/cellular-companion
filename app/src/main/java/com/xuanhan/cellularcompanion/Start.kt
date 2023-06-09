package com.xuanhan.cellularcompanion

import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
@Preview(backgroundColor = 0xFFFFFFFF)
fun StartScreen() {
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
        Text(text = "Welcome to Cellular Companion", style = MaterialTheme.typography.displaySmall)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Ensure that you have purchased the Cellular app on the Mac App Store prior to setup.")
        Spacer(modifier = Modifier.height(48.dp))
        Button(onClick = { /*TODO*/ }) {
            Text("Get started")
        }
    }
}