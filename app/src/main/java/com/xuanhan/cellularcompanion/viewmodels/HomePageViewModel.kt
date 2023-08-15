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
import com.xuanhan.cellularcompanion.models.dataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomePageViewModel(context: Context, navigator: DestinationsNavigator): ViewModel() {
    private val _isShowingConfirmUnlinkDialog = MutableStateFlow(false)
    val isShowingConfirmUnlinkDialog: StateFlow<Boolean> = _isShowingConfirmUnlinkDialog.asStateFlow()

    init {
        bluetoothModel.registerForReset {
            completeReset(context, navigator)
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
        bluetoothModel.reset()
    }

    private fun completeReset(context: Context, navigator: DestinationsNavigator) {
        viewModelScope.launch {
            context.dataStore.edit {
                it.clear()
            }

            val mainActivity = context.findActivity() as MainActivity
            mainActivity.disconnectService()
            mainActivity.stopService()

            navigator.navigate(StartDestination) {
                popUpTo(HomePageDestination) { inclusive = true }
            }
        }
    }
}