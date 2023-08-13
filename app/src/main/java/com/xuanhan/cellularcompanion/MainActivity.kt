package com.xuanhan.cellularcompanion

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
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
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.animations.defaults.RootNavGraphDefaultAnimations
import com.ramcosta.composedestinations.animations.rememberAnimatedNavHostEngine
import com.xuanhan.cellularcompanion.destinations.StartDestination
import com.xuanhan.cellularcompanion.models.BluetoothModel
import com.xuanhan.cellularcompanion.models.BluetoothModel.Companion.ConnectStatus
import com.xuanhan.cellularcompanion.models.dataStore
import com.xuanhan.cellularcompanion.services.BluetoothService
import com.xuanhan.cellularcompanion.ui.theme.AppTheme
import com.xuanhan.cellularcompanion.utilities.WifiHotspotManager
import com.xuanhan.cellularcompanion.viewmodels.MainViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.properties.Delegates

@SuppressLint("StaticFieldLeak")
// Note: Design flaw --> should have used state hoisting, but this isn't the biggest problem since there is only one activity.
val bluetoothModel = BluetoothModel()
lateinit var bluetoothService: BluetoothService
var requiresBtPermissionCheck = false
val isSetupCompleteKey = booleanPreferencesKey("isSetupComplete")
var isSetupComplete by Delegates.notNull<Boolean>()
val ssidKey = stringPreferencesKey("ssid")
val passwordKey = stringPreferencesKey("password")

// TODO: handle missing permissions
class MainActivity : ComponentActivity() {
    private val viewModel = MainViewModel()
    private lateinit var hotspotManager: WifiHotspotManager
    private val enableBt =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                viewModel.setBluetoothEnabled(this)
            }
        }
    private var serviceConnected = false
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as BluetoothService.BluetoothServiceBinder
            bluetoothService = binder.getService()
            serviceConnected = true
            println("Service connected")
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            serviceConnected = false
            println("Service disconnected")
        }
    }

    override fun onStart() {
        super.onStart()

        CoroutineScope(Dispatchers.IO).launch {
            isSetupComplete = this@MainActivity.dataStore.data.map { settings ->
                settings[isSetupCompleteKey] ?: false
            }.first()
            CoroutineScope(Dispatchers.Main).launch {
                if (isSetupComplete) {
                    startService()
                    connectService()
                }
            }
        }
    }

    internal fun startService() {
        Intent(this, BluetoothService::class.java).also { intent ->
            startForegroundService(intent)
        }
    }

    internal fun connectService() {
        Intent(this, BluetoothService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

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

        hotspotManager = WifiHotspotManager(this)

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
                    val showBondFailedDialogState: Boolean by viewModel.isShowingBondFailedDialog.collectAsState()
                    val showHotspotFailedDialogState: Boolean by viewModel.isShowingHotspotFailedDialog.collectAsState()
                    val showResetFailedDialogState: Boolean by viewModel.isShowingResetFailedDialog.collectAsState()
                    val isBluetoothEnabled: Boolean by viewModel.isBluetoothEnabled.collectAsState()

                    if (showScanFailedDialogState)
                        ScanFailedDialog()
                    if (showUnexpectedErrorDialogState)
                        UnexpectedErrorDialog()
                    if (showHotspotDetailsShareFailedDialogState)
                        HotspotDetailsShareFailedDialog()
                    if (showConnectFailedDialogState)
                        ConnectFailedDialog()
                    if (showBondFailedDialogState)
                        BondFailedDialog()
                    if (showHotspotFailedDialogState)
                        HotspotFailedDialog()
                    if (showResetFailedDialogState)
                        ResetFailedDialog()
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
    fun BondFailedDialog() {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {
                TextButton(onClick = {
                    viewModel.confirmBondFailedDialog()
                }) {
                    Text(text = "Retry")
                }
            },
            dismissButton = {},
            title = {
                Text(text = "Failed to pair")
            },
            text = {
                Text(text = "Ensure that your Mac remains close to this device and try again.")
            }
        )
    }

    @Composable
    fun HotspotFailedDialog() {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {
                TextButton(onClick = {
                    viewModel.confirmHotspotFailedDialog()
                }) {
                    Text(text = "Retry")
                }
            },
            dismissButton = {},
            title = {
                Text(text = "Failed to enable hotspot")
            },
            text = {
                Text(text = "An unexpected error has occurred. Please try again.")
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

    @Composable
    fun ResetFailedDialog() {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {
                TextButton(onClick = {
                    viewModel.confirmResetFailedDialog()
                }) {
                    Text(text = "Retry")
                }
            },
            dismissButton = {},
            title = {
                Text(text = "Failed to unlink Mac")
            },
            text = {
                Text(text = "Ensure that your Mac remains close to this device and try again.")
            }
        )
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        println("onWindowFocusChanged")
        if (hasFocus) {
            viewModel.setBluetoothEnabled(this)

            if ((viewModel.connectStatus == ConnectStatus.Connected || viewModel.connectStatus == ConnectStatus.Connecting) && !hotspotManager.isTetherActive) {
                bluetoothModel.disableHotspot()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (serviceConnected) {
            unbindService(connection)
        }
    }
}