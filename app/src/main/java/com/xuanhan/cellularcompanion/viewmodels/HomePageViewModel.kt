package com.xuanhan.cellularcompanion.viewmodels

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.navigation.popUpTo
import com.xuanhan.cellularcompanion.MainActivity
import com.xuanhan.cellularcompanion.bluetoothModel
import com.xuanhan.cellularcompanion.destinations.HomePageDestination
import com.xuanhan.cellularcompanion.destinations.StartDestination
import com.xuanhan.cellularcompanion.findActivity
import com.xuanhan.cellularcompanion.isSetupComplete
import com.xuanhan.cellularcompanion.models.dataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * This class is the view model for the Home screen.
 */
class HomePageViewModel(context: Context, navigator: DestinationsNavigator): ViewModel() {
    private val _isShowingConfirmUnlinkDialog = MutableStateFlow(false)
    val isShowingConfirmUnlinkDialog: StateFlow<Boolean> = _isShowingConfirmUnlinkDialog.asStateFlow()
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        bluetoothModel.registerForReset {
            completeReset(context, navigator)
        }
        startBluetoothService(context)
    }

    private fun startBluetoothService(context: Context) {
        if (isSetupComplete) {
            val mainActivity = context.findActivity() as MainActivity
            mainActivity.startService()
            mainActivity.connectService()
        }
    }

    fun enableHotspot() {
        bluetoothModel.enableHotspot()
    }

    fun disableHotspot() {
        bluetoothModel.disableHotspot()
    }

    fun showConfirmUnlinkDialog() {
        _isShowingConfirmUnlinkDialog.value = true
    }

    fun hideConfirmUnlinkDialog() {
        _isShowingConfirmUnlinkDialog.value = false
    }

    fun confirmConfirmUnlinkDialog() {
        _isShowingConfirmUnlinkDialog.value = false
        _isLoading.value = true
        bluetoothModel.reset()
    }

    /**
     * This function completes the reset process.
     * @param context The context of the composable that calls this function.
     * @param navigator The navigator of the composable that calls this function.
     */
    private fun completeReset(context: Context, navigator: DestinationsNavigator) {
        viewModelScope.launch {
            // Clear DataStore
            context.dataStore.edit {
                it.clear()
            }

            // Disconnect from and stop service
            val mainActivity = context.findActivity() as MainActivity
            mainActivity.disconnectService()
            mainActivity.stopService()

            // Navigate to Start screen
            navigator.navigate(StartDestination) {
                popUpTo(HomePageDestination) { inclusive = true }
            }
        }
    }
}