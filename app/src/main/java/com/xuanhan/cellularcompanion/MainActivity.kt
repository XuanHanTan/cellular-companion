package com.xuanhan.cellularcompanion

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.xuanhan.cellularcompanion.ui.theme.AppTheme

enum class Routes {
    Start
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                Main()
            }
        }
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Main() {
    val navController = rememberNavController()

    Scaffold {
        NavHost(navController = navController, startDestination = Routes.Start.name) {
            composable(Routes.Start.name) {
                StartScreen()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun Preview() {
    AppTheme {
        Main()
    }
}


/*@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val requestDiscoverableIntent: ActivityResultLauncher<String> = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { result ->

        }
    )

    Text(
        text = "Hello $name!",
        modifier = modifier
    )
    Button(onClick = {
        val bluetoothController = BluetoothController(context)
        bluetoothController.initialize()
    }) {
        Text("Test")
    }
}*/