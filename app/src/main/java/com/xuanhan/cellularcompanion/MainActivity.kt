package com.xuanhan.cellularcompanion

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.animations.defaults.RootNavGraphDefaultAnimations
import com.ramcosta.composedestinations.animations.rememberAnimatedNavHostEngine
import com.xuanhan.cellularcompanion.destinations.StartDestination
import com.xuanhan.cellularcompanion.models.BluetoothModel
import com.xuanhan.cellularcompanion.ui.theme.AppTheme
import com.xuanhan.cellularcompanion.viewmodels.MainViewModel

@SuppressLint("StaticFieldLeak")
// Note: Design flaw --> should have used state hoisting, but this isn't the biggest problem since there is only one activity.
val bluetoothModel = BluetoothModel()
var requiresBtPermissionCheck = false

class MainActivity : ComponentActivity() {
    private val viewModel = MainViewModel()
    private val enableBt =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                viewModel.setBluetoothEnabled(this)
            }
        }


    // TODO: ensure that Bluetooth is always on
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @OptIn(
        ExperimentalMaterialNavigationApi::class, ExperimentalAnimationApi::class,
        ExperimentalMaterial3Api::class
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!viewModel.setBluetoothEnabled(this)) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBt.launch(enableBtIntent)
        }

        setContent {
            val navHostEngine = rememberAnimatedNavHostEngine(
                rootDefaultAnimations = RootNavGraphDefaultAnimations(
                    enterTransition = {
                        slideInVertically(initialOffsetY = { it }) + fadeIn()
                    },
                    exitTransition = {
                        fadeOut()
                    },
                    popEnterTransition = {
                        fadeIn()
                    },
                    popExitTransition = {
                        slideOutVertically(targetOffsetY = { it }) + fadeOut()
                    }
                ),
            )
            AppTheme {
                Scaffold {
                    val showScanFailedDialogState: Boolean by viewModel.isShowingScanFailedDialog.collectAsState()
                    val showUnexpectedErrorDialogState: Boolean by viewModel.isShowingUnexpectedErrorDialog.collectAsState()
                    val showHotspotDetailsShareFailedDialogState: Boolean by viewModel.isShowingHotspotDetailsShareFailedDialog.collectAsState()
                    val showConnectFailedDialogState: Boolean by viewModel.isShowingConnectFailedDialog.collectAsState()
                    val isBluetoothEnabled: Boolean by viewModel.isBluetoothEnabled.collectAsState()

                    if (showScanFailedDialogState)
                        ScanFailedDialog()
                    if (showUnexpectedErrorDialogState)
                        UnexpectedErrorDialog()
                    if (showHotspotDetailsShareFailedDialogState)
                        HotspotDetailsShareFailedDialog()
                    if (showConnectFailedDialogState)
                        ConnectFailedDialog()
                    if (!isBluetoothEnabled)
                        EnableBluetoothDialog()

                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.3f))
                            .pointerInput(Unit) {}
                    )
                    DestinationsNavHost(
                        navGraph = NavGraphs.root,
                        startRoute = StartDestination,
                        engine = navHostEngine
                    )
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        println("onWindowFocusChanged")
        if (hasFocus) {
            viewModel.setBluetoothEnabled(this)
        }
    }

    @Composable
    fun ScanFailedDialog() {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {
                TextButton(onClick = {
                    viewModel.confirmScanFailedDialog()
                }) {
                    Text(text = "Retry")
                }
            },
            dismissButton = {},
            title = {
                Text(text = "Failed to scan for devices")
            },
            text = {
                Text(text = "Please make sure that Bluetooth is enabled and press the Retry button.")
            }
        )
    }

    @Composable
    fun UnexpectedErrorDialog() {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {
                TextButton(onClick = {
                    viewModel.confirmUnexpectedErrorDialog()
                }) {
                    Text(text = "Retry")
                }
            },
            dismissButton = {},
            title = {
                Text(text = "Unexpected error")
            },
            text = {
                Text(text = "An unexpected error occurred while communicating with your Mac. Please try again.")
            }
        )
    }

    @Composable
    fun HotspotDetailsShareFailedDialog() {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {
                TextButton(onClick = {
                    viewModel.confirmHotspotDetailsShareFailedDialog()
                }) {
                    Text(text = "Retry")
                }
            },
            dismissButton = {},
            title = {
                Text(text = "Failed to share hotspot details")
            },
            text = {
                Text(text = "Ensure that your Mac remains close to this device and try again.")
            }
        )
    }

    @Composable
    fun ConnectFailedDialog() {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {
                TextButton(onClick = {
                    viewModel.confirmConnectFailedDialog()
                }) {
                    Text(text = "Retry")
                }
            },
            dismissButton = {},
            title = {
                Text(text = "Failed to connect")
            },
            text = {
                Text(text = "Ensure that your Mac remains close to this device and try again.")
            }
        )
    }

    @Composable
    fun EnableBluetoothDialog() {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {
                TextButton(onClick = {
                    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    enableBt.launch(enableBtIntent)
                }) {
                    Text(text = "Enable")
                }
            },
            dismissButton = {},
            title = {
                Text(text = "Turn on Bluetooth")
            },
            text = {
                Text(text = "Bluetooth is required for Cellular Companion to connect to your Mac.")
            }
        )
    }
}