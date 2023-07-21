package com.xuanhan.cellularcompanion.viewmodels

import com.xuanhan.cellularcompanion.bluetoothModel
import com.xuanhan.cellularcompanion.models.BluetoothModel.Companion.ConnectStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class HomePageViewModel {
    private val _connectStatus = MutableStateFlow(ConnectStatus.Disconnected)
    val connectStatus: StateFlow<ConnectStatus> = _connectStatus.asStateFlow()

    init {
        bluetoothModel.registerForUIChanges(onConnectStatusUpdate = ::onConnectStatusUpdate)
    }

    private fun onConnectStatusUpdate(status: ConnectStatus) {
        _connectStatus.value = status
    }

    fun enableHotspot() {
        bluetoothModel.enableHotspot()
    }

    fun disableHotspot() {
        bluetoothModel.disableHotspot()
    }
}