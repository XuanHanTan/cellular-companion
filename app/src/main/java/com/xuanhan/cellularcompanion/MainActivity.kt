package com.xuanhan.cellularcompanion

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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

class MainActivity : ComponentActivity() {
    private val viewModel = MainViewModel()

    // TODO: ensure that Bluetooth is always on
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @OptIn(
        ExperimentalMaterialNavigationApi::class, ExperimentalAnimationApi::class,
        ExperimentalMaterial3Api::class
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

                    if (showScanFailedDialogState)
                        ScanFailedDialog()
                    if (showUnexpectedErrorDialogState)
                        UnexpectedErrorDialog()
                    if (showHotspotDetailsShareFailedDialogState)
                        HotspotDetailsShareFailedDialog()
                    if (showConnectFailedDialogState)
                        ConnectFailedDialog()

                    DestinationsNavHost(
                        navGraph = NavGraphs.root,
                        startRoute = StartDestination,
                        engine = navHostEngine
                    )
                }
            }
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
}